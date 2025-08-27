# Yuan-Developed-By-27570

"Yuan"是针对于FTRST Tech Challenge的sdk进行开发的，首个结合JUC并发编程的一个具有线程控制的Java程序架构。使用了GPL-3.0License，你可以随意修改代码，但请注意基于此项目的代码必须开源！

"Yuan" was developed for the FTRST Tech Challenge SDK. It's the first Java program framework with thread control, combined with JUC concurrent programming. It uses the GPL-3.0 license, so you can modify the code at will. However, please note that the code based on this project must be open source!

---

## How to get it?

我们已经将代码上传至Maven Central，你只需要在你的`build.dependencies.gradle`中添加如下依赖即可

```groovy
dependencies {
    /**    
    implementation 'org.firstinspires.ftc:Inspection:10.3.0'
    implementation 'org.firstinspires.ftc:Blocks:10.3.0'
    implementation 'org.firstinspires.ftc:RobotCore:10.3.0'
    implementation 'org.firstinspires.ftc:RobotServer:10.3.0'
    implementation 'org.firstinspires.ftc:OnBotJava:10.3.0'
    implementation 'org.firstinspires.ftc:Hardware:10.3.0'
    implementation 'org.firstinspires.ftc:FtcCommon:10.3.0'
    implementation 'org.firstinspires.ftc:Vision:10.3.0'
    以上是FTC的官方SDK
    **/
    implementation 'io.github.bear27570:Yuan:1.0.1'
}
```

We have uploaded the code to Maven Central. You only need to add the following dependency to your `build.dependencies.gradle`

```groovy
dependencies {
    /**    
    implementation 'org.firstinspires.ftc:Inspection:10.3.0'
    implementation 'org.firstinspires.ftc:Blocks:10.3.0'
    implementation 'org.firstinspires.ftc:RobotCore:10.3.0'
    implementation 'org.firstinspires.ftc:RobotServer:10.3.0'
    implementation 'org.firstinspires.ftc:OnBotJava:10.3.0'
    implementation 'org.firstinspires.ftc:Hardware:10.3.0'
    implementation 'org.firstinspires.ftc:FtcCommon:10.3.0'
    implementation 'org.firstinspires.ftc:Vision:10.3.0'
    The above is the official SDK of FTC
    **/
    implementation 'io.github.bear27570:Yuan:1.0.1'
}
```

---

## About "Yuan"

### Hardware Part

#### GamepadEx

`GamepadEx`类是对于原生`Gamepad`的一个再封装，扩展了更多功能。其下有两个子类：`GamepadButton`和`GamepadValue`，分别对应着手柄上的按钮和可以记录按下位置的摇杆和扳机。`GamepadEx`所有按键都自带一个线程，该线程对外开放修改，定义后**不要调用`start()`方法**，在`update()`中已经实现了该功能，重复调用会报`IllegalThreadStateException`异常

The `GamepadEx` class is a re-encapsulation of the native `Gamepad`, extending it with more functionality. It has two subclasses: `GamepadButton` and `GamepadValue`, which correspond to the buttons on the controller and the joystick and trigger that can record the pressed position, respectively. All keys in `GamepadEx` have their own thread, which is open to modification. After defining it, **do not call the `start()` method**. This function has been implemented in `update()`. Repeated calls will result in an `IllegalThreadStateException` exception.

#### ServoEx

`ServoEx`对应着原生的`Servo`类，实现了`RunnableStructUnit`接口，使用了JUC中基于AQS框架的ReentrantLock，通过对该控制机构上线程锁，防止一个连贯动作被修改（区别于打断，打断会释放锁并执行其他需要锁的逻辑）。其功能丰富，使用Builder Param实现链式调用以适应输入数据的不确定性。使用了`HashMap`将`Action`这一枚举类型的名称与对应的舵机位置进行映射，方便管理舵机数据，可以提升用户端程序的可读性，用户也可以通过读取有限状态机`ServoState`获取舵机当前的状态。同时对设置临时数据保持兼容性，以保证其可以用于视觉识别等场景。

`ServoEx` corresponds to the native `Servo` class, implements the `RunnableStructUnit` interface, and uses the ReentrantLock based on the AQS framework in JUC. By setting a thread lock on the control mechanism, a coherent action is prevented from being modified (different from interruption, which releases the lock and executes other logic that requires the lock). It is rich in functions and uses Builder Param to implement chain calls to adapt to the uncertainty of input data. `HashMap` is used to map the name of the `Action` enumeration type to the corresponding servo position, which facilitates the management of servo data and improves the readability of the user-side program. Users can also obtain the current status of the servo by reading the finite state machine `ServoState`. At the same time, compatibility is maintained for setting temporary data to ensure that it can be used in scenarios such as visual recognition.

#### CRServoEx

该类主要是为了兼容`CRServo`类型，逻辑与上面的`ServoEx`相同，在此不再赘述。

This class is mainly for compatibility with the `CRServo` type. The logic is the same as the above `ServoEx` and will not be repeated here.


