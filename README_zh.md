# VLX-VMENGINE-JVM

## JAVA 字节码执行引擎

传统的Java动态调试仅能够基于源码级别，如果没有源码，或者被混淆后的Java类文件，则无法进行动态调试。

Java程序的运行基于JVM虚拟机， JVM虚拟机以字节码作为执行的基础，我们使用Kotlin构造了一个JVM字节码执行引擎，可以借助现代的IDE，如IDEA，在字节码层面对Java程序进行调试，以观察程序的运行行为。

**注意，本项目仅用于学习和研究JVM的运行原理以及对恶意程序进行分析，严禁将其应用于非法用途。**

## 前置知识基础

使用本项目前，请确保你已经有如下知识基础

1. 了解Java类文件的格式
2. 了解JVM的各个字节码的作用和含义

## 使用IDEA在字节码层面进行调试

```bash
git clone https://github.com/vlinx-io/vlx-vmengine-jvm.git
```

使用IDEA打开本项目(需要JDK17)，并转到TestCases

TestCases中有两个测试用例，一个用于执行静态方法，一个用于执行实例方法，分别为`executeStaticMethod`与`executeVirtualMethod`,

在对应的方法上，填充上`classPath`, `className`, `methodName`, `methodSignature`这些信息，
类文件的详细信息可以使用[ClassViewer](https://github.com/ClassViewer/ClassViewer)查看。

### 直接运行

以下面这段代码编译的类文件为例

```java
public class Hello {

    public void hello() {
        System.out.println("hello");
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

执行`executeVirtualMethod`, 运行该类的hello方法

```kotlin
val classPath = "your-classpath"
val className = "Hello"
val methodName = "hello"
val methodSignature = "()V"
val args = listOf<Any?>()

val url = File(classPath).toURI().toURL()
val urls = arrayOf(url)
val loader = VlxClassLoader(urls)

val clazz = loader.loadClass(className)

val method = ClassUtils.getMethod(clazz, methodName, methodSignature, loader)
val instance = clazz.getDeclaredConstructor().newInstance()


val thread = VMThread(VMEngine.instance, loader)
thread.execute(instance, method!!, args, true, 0)
```

可以在控制台得到如下输出

```bash
2023-05-21 17:51:10 [DEBUG] Execute method: public void Hello.hello()
2023-05-21 17:51:10 [DEBUG] Receiver: Hello@3daf7722
2023-05-21 17:51:10 [DEBUG] Args: []
2023-05-21 17:51:11 [DEBUG] LocalVars: [kotlin.Unit]
2023-05-21 17:51:11 [DEBUG] "L0: GETSTATIC"
2023-05-21 17:51:11 [DEBUG] "#7"
2023-05-21 17:51:11 [DEBUG] public static final java.io.PrintStream java.lang.System.out
2023-05-21 17:51:11 [DEBUG] "push" org.gradle.internal.io.LinePerThreadBufferingOutputStream@6aa3a905
2023-05-21 17:51:11 [DEBUG] "L3: LDC"
2023-05-21 17:51:11 [DEBUG] "#13"
2023-05-21 17:51:11 [DEBUG] "hello"
2023-05-21 17:51:11 [DEBUG] "push" "hello"
2023-05-21 17:51:11 [DEBUG] "L5: INVOKEVIRTUAL"
2023-05-21 17:51:11 [DEBUG] "#15"
2023-05-21 17:51:11 [DEBUG] "class java.io.PrintStream, NameAndType(name='println', type='(Ljava/lang/String;)V')"
2023-05-21 17:51:11 [DEBUG] public void java.io.PrintStream.println(java.lang.String)
2023-05-21 17:51:11 [DEBUG] "pop" "hello"
2023-05-21 17:51:11 [DEBUG] "pop" org.gradle.internal.io.LinePerThreadBufferingOutputStream@6aa3a905
2023-05-21 17:51:11 [DEBUG] 	Execute method: public void org.gradle.internal.io.LinePerThreadBufferingOutputStream.println(java.lang.String)
2023-05-21 17:51:11 [DEBUG] 	Receiver: org.gradle.internal.io.LinePerThreadBufferingOutputStream@6aa3a905
2023-05-21 17:51:11 [DEBUG] 	Args: [org.gradle.internal.io.LinePerThreadBufferingOutputStream@6aa3a905, hello]
2023-05-21 17:51:11 [ERROR] Can't parse class class org.gradle.internal.io.LinePerThreadBufferingOutputStream
hello
2023-05-21 17:51:11 [DEBUG] "L8: RETURN"
```

控制台输出展示了该方法所有的字节码指令，在指令执行中堆栈的变化情况，以及每个字节码指令运行的结果

### 断点调试

如果需要断点调试字节码指令，可以在`VMExecutor`中的`execute()`方法上下断点

```kotlin
 fun execute() {
    while (true) {
        try {
            pc = sequence.index()
            val opcode = sequence.readUnsignedByte().toShort()
            if (execute(opcode)) {
                break
            }
        } catch (vme: VlxVmException) {
            Logger.FATAL(vme)
            exitProcess(1)
        } catch (t: Throwable) {
            handleException(t)
        }

    }
}
```

### 调试子方法字节码

默认情况下，虚拟引擎仅解释执行指定方法的字节码，在指定方法中调用的子方法，仍然在JVM中运行，避免多层调用的巨大性能开销，如果希望所有的方法都通过虚拟引擎
解释执行，请修改`io.vlinx.vmengine.Options`，将`handleSubMethod`修改为true



