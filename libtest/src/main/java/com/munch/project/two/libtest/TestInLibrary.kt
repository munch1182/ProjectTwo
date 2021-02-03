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
        Log.d("test","lib123")
    }

    @AutoCall(target = "lib",priority = 2)
    @JvmStatic
    fun test2() {
        Log.d("test","lib123456")
    }

    @AutoCall(target = "libs",priority = 2)
    @JvmStatic
    fun test3() {
        Log.d("test","lib123456")
    }
}

