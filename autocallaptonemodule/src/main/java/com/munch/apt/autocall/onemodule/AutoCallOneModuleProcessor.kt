package com.munch.apt.autocall.onemodule

import com.munch.annotation.autocall.AutoCall
import com.munch.apt.autocall.AnnotationElement
import com.munch.apt.autocall.AutoCallProcessor
import com.munch.manager.autocall.AutoCallManager
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.FilerException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class AutoCallOneModuleProcessor : AutoCallProcessor() {

    /**
     * 根据找到的注解归总调用并生成文件
     *
     * 在注解未生成之前使用[AutoCallManager.generatePackageName].[AutoCallManager.callClassName]来调用
     * 编译后再自动生成[AutoCallManager.generatePackageName].[AutoCallManager.callClassName]类来调用生成的代理方法，
     * 以实现实际调用
     *
     * 但问题在于：apt无法跨模块，两个模块单独生成，无法汇总
     * 在apk中module和lib生成的caller也是在俩个dex文件中
     * 要跨模块调用，需要dex加载
     *
     * 因此此方法只适合一个模块使用，但一个模块应用场景不大
     */
    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        roundEnvironment ?: return false
        hash.clear()
        roundEnvironment.getElementsAnnotatedWith(AutoCall::class.java)
            .forEach {
                check(it is ExecutableElement) { "AutoCall can only be used to static function." }

                val autoCall = it.getAnnotation(AutoCall::class.java)

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
        //创建Caller类并实现call方法
        val callMethodBuilder = MethodSpec.methodBuilder(AutoCallManager.callFunName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addParameter(
                ParameterSpec.builder(
                    String::class.java,
                    AutoCallManager.callFunParameterName()
                ).build()
            )
            .addJavadoc("Auto generate by AutoCall annotation")
        callMethodBuilder.beginControlFlow("switch(\$L)", AutoCallManager.callFunParameterName())

        hash.forEach { (s, mutableList) ->
            mutableList.sortDescending()
            //根据每个target创建一个代理类

            val proxyMethodBuilder = MethodSpec.methodBuilder(AutoCallManager.proxyFunName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            //开始方法体
            mutableList.forEach {
                proxyMethodBuilder.addStatement("\$L.\$L()", it.clazz, it.element.simpleName)
            }

            val proxyClazz = TypeSpec.classBuilder(AutoCallManager.newClassName(s))
                .addModifiers(Modifier.FINAL)
                .addMethod(proxyMethodBuilder.build())
                .build()
            val file = JavaFile.builder(AutoCallManager.generatePackageName(), proxyClazz).build()

            try {
                file.writeTo(processingEnv.filer)
                //忽略重建异常
            } catch (e: FilerException) {
                e.printStackTrace()
            }
            //call调用每个代理类
            callMethodBuilder.addCode("case \$S:\n", s)
            callMethodBuilder.addStatement(
                "\t\$L.\$L()",
                proxyClazz.name,
                AutoCallManager.proxyFunName()
            )
            callMethodBuilder.addStatement("\tbreak")
        }
        callMethodBuilder.endControlFlow()

        val caller = TypeSpec.classBuilder(AutoCallManager.callClassName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(callMethodBuilder.build())
            .build()

        val file = JavaFile.builder(AutoCallManager.generatePackageName(), caller).build()

        try {
            file.writeTo(processingEnv.filer)
            //忽略重建异常
        } catch (e: FilerException) {
            /*e.printStackTrace()*/
        }
        return true
    }

}