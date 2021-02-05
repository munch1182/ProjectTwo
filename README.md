# P2

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=3 orderedList=false} -->

<!-- code_chunk_output -->

- [AOP](#aop)
- [APT(Annotation Processing Tool)](#aptannotation-processing-tool)
  - [1. 自定义注解](#1-自定义注解)
  - [2. 创建注解器](#2-创建注解器)
  - [3. 使用](#3-使用)
  - [4. 其它](#4-其它)
  - [5. AbstractProcessor的实现](#5-abstractprocessor的实现)
- [plugin](#plugin)
  - [实现](#实现)
  - [使用](#使用)
  - [发布](#发布)
- [Transform](#transform)
  - [引入](#引入)
  - [使用](#使用-1)
  - [调试](#调试)
  - [实现](#实现-1)

<!-- /code_chunk_output -->

## AOP

![avatar][base64str4aop]


## APT(Annotation Processing Tool)

### 1. 自定义注解

### 2. 创建注解器

- a. 创建一个`java`的`Library`
- b. 在`java`文件夹创建`javax.annotation.processing.AbstractProcessor`的实现类
- c. 创建`resources/META-INF/services`文件夹并在其下添加`javax.annotation.processing.Processor`文件，并在文件中添加实现类的全路径
    
### 3. 使用

- 项目依赖(`implementation`或`api`)自定义注解，使用编译时注解(`kapt`)引入注解器依赖

### 4. 其它

- 对于注解器，应该与自定义声明类分属两个不同的module，因为声明类会被代码依赖，而注解类与代码无关，对于`android`来说，声明类最终会被编译进apk，而注解器的内容则不需要被放入apk
- `java`的`library`相较于`android`，没有`res`文件夹，`build.gradle`不需要`com.android`相关插件及配置，`java`的`library`的插件名为`java-library`
- `java`下可以使用[auto-service](https://mvnrepository.com/artifact/com.google.auto.service/auto-service)自动生成`resources/META-INF/services`相关注册，但如果对于`apt`的实现类是`kotlin`文件则无效

### 5. AbstractProcessor的实现

#### 方法

- 必须要实现`getSupportedAnnotationTypes`、`getSupportedSourceVersion`和`process`三个方法，其中前两个方法可以使用注解`@SupportedAnnotationTypes`和`@SupportedSourceVersion`完成，具体见其实现代码，但注解上必须使用常量即字符串，不如方法返回可以使用class来返回类名，`process`即具体处理的方法

#### debug

- 此类中可以使用`processingEnv.getMessager()`来获取打印对象，使用`processingEnv.getMessager().printMessage()`即可往控制台(`build`信息窗口)输出消息

#### 类型

- 可以从`process`的变量`roundEnvironment`的3个以`get`开头的方法的返回值中获取所有被注解注解的元素的类型，统一为`javax.lang.model.element`类型，可以转为其子类以相应被注解的类型，而从类型中又可以通过`element.getAnnotation`获取注解类对象，从而实现自己的处理处理，一般是根据注解生成代码实现
- 如果`getSupportedAnnotationTypes`返回的是`*`，则`getRootElements`会返回所有被扫到的根元素，一般来说是类
- `element.getEnclosingElement`和`element.getEnclosedElements`分别获取该元素的内外范围内的元素，可以借此获取相关范围内的所有元素(比如方法上的注解可以借此来获取方法外的类元素)，但同时要考虑兼容性

#### 被调用

- `process`会被调用多次，因为同一模块会被多次扫描，使用`roundEnvironment.processingOver()`即可判断是否是最后一次扫描，但当其为`true`之后，已经不能从`roundEnvironment`中获取注解对象，一般来说，无需此判断
- 当项目为多模块时，每个使用了注解处理器的模块会分别调用此实现类并多次调用`process`，即使模块之间有依赖关系，因此`apt`实质上不能跨模块

#### 代码生成

- 因为`apt`是编译成`java`文件，因此生成代码的生成本质上是按格式的字符串拼接
- 可以使用[javapoet](https://github.com/square/javapoet)来帮助格式化的生成java代码

## plugin

- `plugin`指`gradle`的插件
- `gradle`的`plugin`使用`groovy`开发的，兼容`java`语言，不支持直接调用`kotlin`。建议直接使用`groovy`，`groovy`对于`java`来说除了不能自动导入类不会报编译器异常之外基本不会有开发障碍

### 实现

- 1. 创建`java`的`library`
- 2. 在新建的模块中引入依赖`groovy`，包括插件`groovy`和依赖`localGroovy()`和`gradleApi()`
    ```java
    dependencies {
        implementation gradleApi()
        implementation localGroovy()
    }
    ```
- 3. 实现`org.gradle.api.Plugin`
- 4. 在`resources/META-INF/gradle-plugins`文件内建立`pluginname.properties`并在其中添加`implementation-class=实现plugin的全路径类名`，`pluginname`需要自定义，其也是使用时的插件名

### 使用

- `plugin`没找到使用项目依赖的方式使用的方法，因此通过发布的方式引入
- 发布后，在项目的`build.gradle`文件的`buildscript`的`repositories`引入仓库路径，在`dependencies`中使用`classpath`引入`plugin`，即可在对于的模块中以`apply plugin:模块名`的方式使用
    - `buildScript`是用来加载gradle脚本自身所需要的资源

### 发布

#### MAVEN发布到本地

- 1. 在插件模块中引入`maven`插件
- 2. 在插件模块的`build.gradle`文件中配置任务
    ```java
    uploadArchives() {
        repositories.mavenDeployer {
            pom.version = '1.0.0'
            pom.artifactId = 'artifactId'
            pom.groupId = 'groupId'
            repository(url: uri("../repository/"))//本地仓库位置，此处使用项目根文件夹下repository文件夹
        }
    }
    ```
- 3. 发布：执行uploadArchives即可
    - 可以使用androidstuido中`uploadArchives`左侧的快捷图标，也可以使用`gradle`窗口中的对应模块`upload`分组下的`uploadArchives`，也可以使用命令行执行
- 4. 使用
    - 引入: 在`repositories`中添加本地仓库位置`maven { url './repository' }`，在`dependencies`中添加`classpath "groupId:artifactId:version"`
        ```java
        buildscript {
            repositories {
                maven { url './repository' }
                ...
            }
            dependencies {
                ...
                classpath "groupId:artifactId:version"
            }
        }
        ```
    - 使用：`apply plugin: 'pluginname'`
- 5. 注意：每次更改都需要发布到本地仓库才并同步版本号才能生效
    - 建议开发时引入插件路径用`+`代替具体的版本号，每次更改时则不必更新版本号，只需发布并编译即可
    - 或者自定义一个gradle的task用以自动更新版本号并发布

## Transform

- `transform`是class文件转dex文件前修改class文件的api，可以配合`gradle`的`plugin`使用

### 引入

- 在`java library`的依赖中引入`com.android.tools.build:gradle:version`，此依赖即`android`项目中`buildscript `需要的依赖

### 使用

- 1. 实现`com.android.build.api.transform.Transform`
- 2. 在`org.gradle.api.Plugin`的实现类的`apply`方法中注册
    ```groovy
    import com.android.build.gradle.AppExtension
    import org.gradle.api.Plugin
    import org.gradle.api.Project

    //如果提示文件已存在，则是已经编译过，删掉build文件夹即可
    class MyPlugin implements Plugin<Project> {
        @Override
        void apply(Project project) {
            def android = project.extensions.getByType(AppExtension)
            android.registerTransform(new MyTransform())//MyTransform即实现类
        }
    }
    ```

### 调试

- 使用`print`即可输出信息到控制台(`build`信息窗口)

### 实现

- 需要实现的方法：
    - `getName`返回`String`，是一个插件的标识
    - `getInputTypes`，可以返回`com.android.build.gradle.internal.pipeline.TransformManager`下以`CONTENT_`开头的常量，指扫描的文件类型，分class文件和资源文件
    - `getScopes`，可以返回`com.android.build.gradle.internal.pipeline.TransformManager`下以`SCOPE_`开头的常量，指扫描的范围，包括当前module、其它jar包等
    - `isIncremental`，是否启用增量扫描
    - `transform`扫描的回调方法，主要的处理方法





















---


[base64str4aop]:data:image/webp;base64,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