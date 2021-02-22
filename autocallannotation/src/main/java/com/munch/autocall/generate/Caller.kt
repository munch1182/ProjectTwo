@file:Suppress("UNUSED")

package com.munch.autocall.generate

/**
 * Create by munch1182 on 2021/2/3 14:04.
 */
object Caller {

    /**
     * 在此包名下占用，以提供未编译情况下的调用，但实际上只有编译后的调用才有效
     * 等待编译注解完成后，调用的会是编译后的Caller.call方法
     * 因为dex顺序的原因，编译后的同名同包在同一个dex中所有会被调用
     */
    @JvmStatic
    fun call(target: String?) {
        target ?: return
    }

}