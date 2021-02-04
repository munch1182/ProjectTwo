package com.munch.plugin.autocall

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static org.objectweb.asm.Opcodes.ASM5

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
        File callFile = null
        Log.log("scan start")
        trans.inputs.each { input ->
            //遍历查找class文件
            input.directoryInputs.each { di ->
                Log.log("class = ${di.name}")
                if (di.file.isDirectory()) {
                    di.file.eachFileRecurse { file ->
                        Log.log("file = ${file.absolutePath};${file.name}")
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
                if (shouldNotBeFilter(jarInput.name)) {
                    Log.log("not filter jar: ${src}")
                    def file = new JarFile(src)
                    def entries = file.entries()
                    while (entries.hasMoreElements()) {
                        def element = entries.nextElement()
                        if (element.isDirectory()) {
                            continue
                        }
                        def jarEntryName = element.name
                        Log.log("entryName: ${jarEntryName}")
                        if (isProxyJar(jarEntryName)) {
                            Log.log("add jar: ${jarEntryName}")
                            if (jarEntryName.contains("Caller")) {
                                callFile = src
                            } else {
                                proxyClass.add(jarEntryName)
                            }
                        }
                    }
                }
                FileUtils.copyFile(src, out)
            }
        }

        Log.log("scan end")
        Log.log("=========================")
        proxyClass.forEach({ fileName ->
            Log.log("scan: ${fileName}")
        })
        if (callFile != null) {
            Log.log("Caller : ${callFile.absolutePath}")
        }
        Log.log("=========================")

        if (callFile == null) {
            return
        }
        inject(callFile, proxyClass)
    }

    static void inject(File callerFile, List<String> proxyClass) {

        Log.log("start code inject")
        //Caller文件
        def temp = new File(callerFile.parent, callerFile.name + ".opt")
        if (temp.exists()) {
            temp.delete()
        }
        def callJarFile = new JarFile(callerFile)
        def enumeration = callJarFile.entries()
        def tempJarOps = new JarOutputStream(new FileOutputStream(temp))
        while (enumeration.hasMoreElements()) {
            def element = enumeration.nextElement()
            def name = element.name
            Log.log(name)
            //从源文件中读取内容复制到缓存文件
            def zipEntry = new ZipEntry(name)
            def inputStream = callJarFile.getInputStream(element)
            tempJarOps.putNextEntry(zipEntry)

            //找到Caller类
            if (name != "com/munch/autocall/generate/Caller.class") {
                //其余内容原样写入
                tempJarOps.write(IOUtils.toByteArray(inputStream))
            } else { // 修改call方法
                Log.log("Caller class: ${element}")
                def classReader = new ClassReader(inputStream)
                def classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
                //注入代码
                def visitor = new CallerClassVisitor(classWriter, proxyClass)
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)

                tempJarOps.write(classWriter.toByteArray())
            }
            try {
                inputStream.close()
                tempJarOps.closeEntry()
            } catch (Ignore) {
                //
            }
        }
        try {
            tempJarOps.close()
            callJarFile.close()
        } catch (Ignore) {
            //
        }
        FileUtils.copyFile(temp, new File(temp.parent, "temp_back.opt"))
        Log.log(temp.getAbsolutePath())
        //重命名替换
        temp.renameTo(callerFile)
    }

    static class CallerClassVisitor extends ClassVisitor {

        private List<String> proxyClass

        CallerClassVisitor(ClassVisitor classVisitor, ArrayList<String> proxyClass) {
            super(ASM5, classVisitor)
            this.proxyClass = proxyClass
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            Log.log("visitMethod: ${name}")
            def mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name == "call") {
                mv = new MethodAdapter(proxyClass, mv, access, name, descriptor)
            }
            return mv
        }
    }

    static class MethodAdapter extends AdviceAdapter {

        private List<String> proxyClass

        protected MethodAdapter(List<String> proxyClass, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(ASM5, methodVisitor, access, name, descriptor)
            this.proxyClass = proxyClass
        }

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter()
            proxyClass.forEach({ str ->
                def content = "com.munch.autocall.generate.${getTargetClassName(str)}"
                Log.log("content: ${content}")
                //将引用变量推到栈顶
                /*mv.visitLdcInsn(content)*/
                //添加方法
                mv.visitMethodInsn(INVOKESTATIC, "com.munch.autocall.generate.${getTargetClassName(str)}", "proxyFunction", "()V", false)
            })
        }

        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode)
        }

        static String getTargetClassName(String name) {
            if (name.contains("/")) {
                def split = name.split("/")
                name = split[split.size() - 1]
            }
            if (!isProxyFile(name)) {
                return null
            }
            return name.replace(".class", "")
        }

        static String getTarget(String name) {
            if (name.contains("/")) {
                def split = name.split("/")
                name = split[split.size() - 1]
            }
            if (!isProxyFile(name)) {
                return null
            }
            return name.replace("AutoCall\$\$", "").replace("\$\$Proxy", "")
        }
    }

    static boolean shouldNotBeFilter(String name) {
        return !name.startsWith("androidx") && !name.startsWith("com.android") && !name.startsWith("org.jetbrains") && !name.startsWith("com.google") && !name.startsWith("kotlinx") && !name.startsWith("javax") && !name.startsWith("okhttp") && !name.startsWith("retrofit") && !name.startsWith("dagger")
    }

    static boolean isProxyFile(String name) {
        name = name.replace(".class", "")
        return name.startsWith("AutoCall\$\$") && name.endsWith("\$\$Proxy")
    }

    static boolean isProxyJar(String name) {
        //包名
        return name.startsWith("com/munch/autocall/generate") || (name.contains("AutoCall\$\$") && name.contains("\$\$Proxy"))
    }
}