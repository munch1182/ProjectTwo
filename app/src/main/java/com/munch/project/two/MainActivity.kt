package com.munch.project.two

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.munch.annotation.autocall.AutoCall
import com.munch.autocall.generate.Caller
import com.munch.project.two.libtest.Main

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("test", "app main")
        Caller.call("lib")
        Caller.call("libs")
        Caller.call("test")
        Main.mainLib()
    }

    companion object {

        @AutoCall(target = "test")
        fun test() {
            Log.d("test", "app test target = test")
        }
    }
}