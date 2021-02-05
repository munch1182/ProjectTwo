@file:Suppress("UNUSED")

package com.munch.autocall.generate

/**
 * Create by munch1182 on 2021/2/3 14:04.
 */
object Caller {

    /**
     * 在此包名下占位，以提供未编译情况下代码的调用，但实际上只有编译后的调用才有效
     * 等待编译注解完成后，会生成一个同包同名的文件，调用的会是编译后的Caller.call方法
     *
     * 因为此类和编译后生成的类会被分在不同的dex下，且此类在lib中所以生成的dex顺序在后面，所以会加载编译后的Caller类而不是此类
     *
     * @param target 与[com.munch.annotation.autocall.AutoCall]的target参数匹配
     * @param any 任意类型的参数
     *
     * 编译生成见autocallaptonemodule下{com.munch.apt.autocall.onemodule.AutoCallOneModuleProcessor}
     */
    @JvmStatic
    fun call(target: String?, any: Any? = null) {
        //避免未使用警告，实际上没有意义
        target ?: return
        any ?: return
    }

}