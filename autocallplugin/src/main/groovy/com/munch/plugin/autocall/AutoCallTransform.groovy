package com.munch.plugin.autocall

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

import java.util.jar.JarFile

class AutoCallTransform extends Transform {
    @Override
    String getName() {
        return "AutoCall"
    }

    /**
     *
     * 该Transform支持扫描的文件类型，分为class文件和资源文件，这里只处理class文件的扫描
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * Transfrom的扫描范围，我这里扫描整个工程，包括当前module以及其他jar包、aar文件等所有的class
     * 因此即可操作其它模块
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否增量扫描
     */
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation trans) throws TransformException, InterruptedException, IOException {
        super.transform(trans)
        Log.log("transform start")

        def proxyClass = []
        Log.log("scan start")
        trans.inputs.each { input ->
            //遍历查找class文件
            input.directoryInputs.each { di ->
                Log.log("class = ${di.name}")
                if (di.file.isDirectory()) {
                    di.file.eachFileRecurse { file ->
                        Log.log("file = ${file.name}")
                        if (isProxyFile(file.name)) {
                            Log.log("add file = ${file}")
                            proxyClass.add(file.name)
                        }
                    }
                }
                //输出路径
                //transform的逻辑是获取输入，然后自行负责输出到指定位置
                def out = trans.outputProvider.getContentLocation(di.name, di.contentTypes, di.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(di.file, out)
            }
            //遍历查找jar包
            input.jarInputs.each { jarInput ->
                Log.log("jarInput = ${jarInput}")

                def jarName = jarInput.name
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //主要是保证文件名不重复
                jarName += DigestUtils.md5Hex(jarInput.file.getAbsolutePath())

                def out = trans.outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                def src = jarInput.file
                if (!shouldBeFilter(jarInput.name)) {
                    Log.log("not filter jar: ${src}")
                    def entries = new JarFile(src).entries()
                    while (entries.hasMoreElements()) {
                        def jarEntryName = entries.nextElement().name
                        Log.log("entryName: ${jarEntryName}")
                        if (isProxyFile(jarEntryName)) {
                            Log.log("add jar: ${jarEntryName}")
                            proxyClass.add(jarEntryName)
                        }
                    }
                }
                FileUtils.copyFile(src, out)
            }
        }

        Log.log("scan end")
        proxyClass.forEach({ fileName ->
            Log.log("scan: ${fileName}")
        })
    }

    static boolean shouldBeFilter(String name) {
        return !name.startsWith("androidx") && !name.startsWith("com.android") && !name.startsWith("org.jetbrains") && !name.startsWith("com.google") && !name.startsWith("kotlinx") && !name.startsWith("javax") && !name.startsWith("okhttp") && !name.startsWith("retrofit") && !name.startsWith("dagger")
    }

    static boolean isProxyFile(String name) {
        name = name.replace(".class", "")
        return name.startsWith("AutoCall\$\$") && name.endsWith("\$\$Proxy")
    }
}