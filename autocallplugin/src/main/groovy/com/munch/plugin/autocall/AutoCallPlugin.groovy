package com.munch.plugin.autocall

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 如果提示文件已存在，则是已经编译过，删掉build文件夹即可
 */
class AutoCallPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        //注册transform
        Log.log("registerTransform")
        try {
            def android = project.extensions.getByType(AppExtension)
            android.registerTransform(new AutoCallTransform())
        } catch (Ignore) {
            //
        }
    }
}