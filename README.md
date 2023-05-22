# VLX-VMENGINE-JVM

[中文说明](https://github.com/vlinx-io/vlx-vmengine-jvm/blob/main/README_zh.md)

## Java Bytecode Execution Engine

Traditional Java dynamic debugging can only be done at the source code level. If there is no source code, or if the Java class file is obfuscated, dynamic debugging cannot be performed.

Java programs run on the JVM (Java Virtual Machine). The JVM uses bytecode as the basis for execution. We have built a JVM bytecode execution engine using Kotlin, which allows us to debug Java programs at the bytecode level using modern IDEs, such as IntelliJ IDEA, to observe the program's runtime behavior.

**Please note that this project is for learning and researching the JVM's operating principles and analyzing malicious programs only. It is strictly forbidden to use it for illegal purposes.**

## Prerequisites

Before using this project, please make sure you have the following knowledge:

1. Understand the format of Java class files
2. Understand the purpose and meaning of each JVM bytecode

## Debugging at the Bytecode Level with IntelliJ IDEA

```bash
git clone https://github.com/vlinx-io/vlx-vmengine-jvm.git
```

Open the project with IntelliJ IDEA (requires JDK 17) and navigate to TestCases.

There are two test cases in TestCases, one for executing static methods and one for executing instance methods, named `executeStaticMethod` and `executeVirtualMethod` respectively.

Fill in the `classPath`, `className`, `methodName`, and `methodSignature` information for the corresponding methods.
Detailed information about the class file can be viewed using [ClassViewer](https://github.com/ClassViewer/ClassViewer).

### Running Directly

Take the class file compiled from the following code as an example:

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

Execute `executeVirtualMethod` to run the `hello` method of the class:

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

You can get the following output in the console:

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

The console output displays all the bytecode instructions of the method, the changes in the stack during instruction execution, and the results of each bytecode instruction running.

### Breakpoint debugging

If you need to debug bytecode instructions with breakpoints, you can set breakpoints in the `execute()` method of the `VMExecutor`.

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

### Debugging Sub-method Bytecode

By default, the virtual engine only interprets and executes the bytecode of the specified method. The sub-methods called within the specified method still run in the JVM to avoid the huge performance overhead of multi-level calls. If you want all methods to be interpreted and executed by the virtual engine, please modify `io.vlinx.vmengine.Options` and set `handleSubMethod` to true.



