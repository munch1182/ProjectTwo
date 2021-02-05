package com.munch.apt.autocall.onemodule

import com.munch.annotation.autocall.AutoCall
import com.munch.manager.autocall.AutoCallManager
import com.squareup.javapoet.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.FilerException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class AutoCallOneModuleProcessor : AbstractProcessor() {

    private val hash: HashMap<String, MutableList<AnnotationElement>> = hashMapOf()

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(AutoCall::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }


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
        //创建Caller类并实现两个call方法
        val callMethodBuilder = MethodSpec.methodBuilder(AutoCallManager.callFunName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addParameter(parameterTarget())
            .addParameter(parameterAny())
            .addJavadoc("Auto generate by AutoCall annotation")
        callMethodBuilder.beginControlFlow(
            "switch(\$L)",
            AutoCallManager.callFunParameterTargetName()
        )

        val callMethod2Builder = MethodSpec.methodBuilder(AutoCallManager.callFunName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .addParameter(
                parameterTarget()
            )
            .addJavadoc("Auto generate by AutoCall annotation")
        callMethod2Builder.beginControlFlow(
            "switch(\$L)",
            AutoCallManager.callFunParameterTargetName()
        )

        hash.forEach { (s, mutableList) ->
            mutableList.sortDescending()
            //根据每个target创建一个代理类

            val proxyMethodBuilder = MethodSpec.methodBuilder(AutoCallManager.proxyFunName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addParameter(
                    parameterAny()
                )
            //开始方法体
            mutableList.forEach {
                val parameters = it.element.parameters
                if (parameters.size > 0) {
                    proxyMethodBuilder.addStatement(
                        "\$L.\$L((\$L)\$L)",
                        it.clazz,
                        it.element.simpleName,
                        parameters[0].asType(),
                        AutoCallManager.callFunParameterAnyName()
                    )
                } else {
                    proxyMethodBuilder.addStatement(
                        "\$L.\$L()",
                        it.clazz,
                        it.element.simpleName
                    )
                }
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
                "\t\$L.\$L(\$L)",
                proxyClazz.name,
                AutoCallManager.proxyFunName(),
                AutoCallManager.callFunParameterAnyName()
            )
            callMethodBuilder.addStatement("\tbreak")
            //call2调用每个代理类
            callMethod2Builder.addCode("case \$S:\n", s)
            callMethod2Builder.addStatement(
                "\t\$L.\$L(null)",
                proxyClazz.name,
                AutoCallManager.proxyFunName()
            )
            callMethod2Builder.addStatement("\tbreak")
        }
        callMethodBuilder.endControlFlow()
        callMethod2Builder.endControlFlow()

        val caller = TypeSpec.classBuilder(AutoCallManager.callClassName())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(callMethodBuilder.build())
            .addMethod(callMethod2Builder.build())
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

    private fun parameterTarget() = ParameterSpec.builder(
        String::class.java,
        AutoCallManager.callFunParameterTargetName()
    ).addAnnotation(NotNull::class.java).build()

    private fun parameterAny() = ParameterSpec.builder(
        Any::class.java,
        AutoCallManager.callFunParameterAnyName()
    ).addAnnotation(Nullable::class.java).build()

    data class AnnotationElement(
        val clazz: String,
        val annotation: AutoCall,
        val element: ExecutableElement
    ) : Comparable<AnnotationElement> {
        override fun compareTo(other: AnnotationElement): Int {
            return annotation.priority.compareTo(other.annotation.priority)
        }
    }
}