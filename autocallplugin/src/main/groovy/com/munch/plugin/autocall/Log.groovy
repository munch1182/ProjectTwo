package com.munch.plugin.autocall

class Log {

    static debug = true

    static void log(String msg) {
        if (!debug) {
            return
        }
        println "AutoCall:" + msg
    }

}