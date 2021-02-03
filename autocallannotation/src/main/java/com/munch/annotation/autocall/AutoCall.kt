package com.munch.annotation.autocall

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class AutoCall(val target: String, val priority: Int = 0)
