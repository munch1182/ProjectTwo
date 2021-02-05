package com.munch.project.two

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.munch.annotation.autocall.AutoCall
import com.munch.autocall.generate.Caller
//因为跨模块引用的关系，经常不能正常识别lib里正常的代码
//见build.gradle
import com.munch.project.two.libtest.Main

class MainActivity : AppCompatActivity() {

    var a = 0.5f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("test", "app main")
        /**
         * 通过占位的call方法实现调用
         * 未编译前的Caller.call方法指向annotation中的Caller类
         * 编译后则指向编译的类，可以通过方法跳转跳转到该生成类
         */
        Caller.call("lib")
        Caller.call("libs", this)
        Caller.call("test", this)
        Main.mainLib()
    }

    companion object {

        @AutoCall(target = "test")
        fun test(any: MainActivity) {
            Log.d("test", "app test target = ${any.a}")
        }
    }
}