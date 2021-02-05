package com.munch.project.two.libtest

import android.util.Log
import com.munch.annotation.autocall.AutoCall

/**
 * Create by munch1182 on 2021/2/3 11:07.
 */
object TestInLibrary {

    @AutoCall(target = "lib")
    @JvmStatic
    fun test() {
        Log.d("test", "TestInLibrary test target = lib")
    }

    @AutoCall(target = "lib", priority = 2)
    @JvmStatic
    fun test2() {
        Log.d("test", "TestInLibrary test2 target = lib priority = 2")
    }

    @AutoCall(target = "libs", priority = 2)
    @JvmStatic
    fun test3() {
        Log.d("test", "TestInLibrary test3 target = libs")
    }
}

