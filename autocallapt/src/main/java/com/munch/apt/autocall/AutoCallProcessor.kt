package com.munch.apt.autocall

import com.munch.annotation.autocall.AutoCall
import com.munch.manager.autocall.AutoCallManager
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.FilerException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

open class AutoCallProcessor : AbstractProcessor() {

    protected val hash: HashMap<String, MutableList<AnnotationElement>> = hashMapOf()

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(AutoCall::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    /**
     * 根据找到的注解归总调用并生成文件
     *
     * 生成的文件在build/generated/source/kapt/debug/文件路径(kapt时kotlin路径)
     *
     *
     */
    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        roundEnvironment ?: return false
        hash.clear()
        roundEnvironment.getElementsAnnotatedWith(AutoCall::class.java)
            .forEach {
                check(it is ExecutableElement) { "AutoCall can only be used to static function" }

                val autoCall = it.getAnnotation(AutoCall::class.java)

                //如果方法不是在类里调用
                if (it.enclosingElement !is TypeElement) {
                    return@forEach
                }

                //获取类名
                val clazz = (it.enclosingElement as TypeElement).qualifiedName.toString()
                //根据target分类
                if (hash.containsKey(autoCall.target)) {
                    hash[autoCall.target]!!.add(AnnotationElement(clazz, autoCall, it))
                } else {
                    hash[autoCall.target] = mutableListOf(AnnotationElement(clazz, autoCall, it))
                }
            }
        hash.forEach { (s, mutableList) ->
            mutableList.sortDescending()
            //根据每个target创建一个代理类

            val methodBuilder = MethodSpec.methodBuilder(AutoCallManager.proxyFunName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            //开始方法体
            mutableList.forEach {
                methodBuilder.addStatement("${it.clazz}.${it.element.simpleName}()")
            }

            val clazz = TypeSpec.classBuilder(AutoCallManager.newClassName(s))
                .addModifiers(Modifier.FINAL)
                .addMethod(methodBuilder.build())
                .build()
            val file = JavaFile.builder(AutoCallManager.generatePackageName(), clazz).build()

            try {
                file.writeTo(processingEnv.filer)
                //忽略重建异常
            } catch (e: FilerException) {
                e.printStackTrace()
            }
        }
        return true
    }
}

data class AnnotationElement(
    val clazz: String,
    val annotation: AutoCall,
    val element: ExecutableElement
) : Comparable<AnnotationElement> {
    override fun compareTo(other: AnnotationElement): Int {
        return annotation.priority.compareTo(other.annotation.priority)
    }
}