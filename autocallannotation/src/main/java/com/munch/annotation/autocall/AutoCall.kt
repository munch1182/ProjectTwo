package com.munch.annotation.autocall


/**
 * 方法声明，在方法上声明，即可通过[com.munch.autocall.generate.Caller.call]使用target调用
 *
 * @param target 调用方法时使用的标记
 * @param priority 优先级，同一target下依据priority从大到小排列
 *
 * @see com.munch.autocall.generate.Caller
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class AutoCall(val target: String, val priority: Int = 0)
