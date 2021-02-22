@file:Suppress("MemberVisibilityCanBePrivate")

package com.munch.manager.autocall

object AutoCallManager {
    private const val PREFIX = "AutoCall".plus("$").plus("$")
    private const val SUFFIX = "$".plus("$").plus("Proxy")

    fun newClassName(target: String) = "$PREFIX$target$SUFFIX"

    fun proxyFunName() = "proxyFunction"
    fun proxyFunParameterName() = "target"
    fun generatePackageName() = "com.munch.autocall.generate"

    fun callClassName() = "Caller"
    fun callFunName() = "call"
    fun callFunParameterName() = proxyFunParameterName()

    fun isProxyFile(name: String): Boolean {
        return name.startsWith(PREFIX) && name.endsWith(SUFFIX)
    }
}