package com.munch.autocall.generate

/**
 * Create by munch1182 on 2021/2/3 14:04.
 */
class Caller {

    companion object {

        /**
         * 在此包名下占用，以提供未编译情况下的调用，但实际上只有编译后的调用才有效
         * 等待编译注解完成后，调用的会是编译后的Caller.call方法
         * 可能时因为编译文件的路径问题，所以同包同名的文件会调用更近的文件
         */
        @JvmStatic
        fun call(target: String) {
        }
    }
}