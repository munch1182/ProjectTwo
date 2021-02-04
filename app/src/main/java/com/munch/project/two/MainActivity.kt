package com.munch.project.two

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.munch.annotation.autocall.AutoCall
import com.munch.autocall.generate.Caller
import com.munch.project.two.libtest.TestInLibrary

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("test", "00000")
        Caller.call("lib")
        Caller.call("libs")
        Caller.call("test")
    }

    companion object {

        @AutoCall(target = "test")
        fun test() {
            Log.d("test", "123123")
        }
    }
}