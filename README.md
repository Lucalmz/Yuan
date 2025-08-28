# Yuan-Developed-By-27570

Stable version :1.0.2

"Yuan"是针对于FTRST Tech Challenge的sdk进行开发的，首个结合JUC并发编程的一个具有线程控制的Java程序架构。使用了GPL-3.0License，你可以随意修改代码，但请注意基于此项目的代码必须开源！如果你想要使用该框架，详情请见Tutorial.md

"Yuan" was developed for the FTRST Tech Challenge SDK. It's the first Java program framework with thread control, combined with JUC concurrent programming. It uses the GPL-3.0 license, so you can modify the code at will. However, please note that the code based on this project must be open source!If you want to use this framework, please see the Tutorial.md for details.

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
    implementation 'io.github.bear27570:Yuan:1.0.2'
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

`ServoEx`对应着原生的`Servo`类，实现了`RunnableStructUnit`接口，使用了JUC中基于AQS框架的ReentrantLock，通过对该控制机构上线程锁，防止一个连贯动作被修改（区别于打断，打断会释放锁并执行其他需要锁的逻辑）。其功能丰富，支持舵机同步，使用Builder Param实现链式调用以适应输入数据的不确定性。使用了`HashMap`将`Action`这一枚举类型的名称与对应的舵机位置进行映射，方便管理舵机数据，可以提升用户端程序的可读性，用户也可以通过读取有限状态机`ServoState`获取舵机当前的状态。同时对设置临时数据保持兼容性，以保证其可以用于视觉识别等场景。支持阻塞性动作，通过设置的舵机转速判断当前需要运行的角度对应需要阻塞的时间（线性的）。

`ServoEx` corresponds to the native `Servo` class, implements the `RunnableStructUnit` interface, and uses the ReentrantLock based on the AQS framework in JUC. By setting a thread lock on the control mechanism, a coherent action is prevented from being modified (different from interruption, which releases the lock and executes other logic that requires the lock). It is rich in functions and uses Builder Param to implement chain calls to adapt to the uncertainty of input data. `HashMap` is used to map the name of the `Action` enumeration type to the corresponding servo position, which facilitates the management of servo data and improves the readability of the user-side program. Users can also obtain the current status of the servo by reading the finite state machine `ServoState`. At the same time, compatibility is maintained for setting temporary data to ensure that it can be used in scenarios such as visual recognition.Supports blocking action, and determines the current required running angle and the corresponding blocking time (linear) by setting the servo speed.

#### CRServoEx

该类主要是为了兼容`CRServo`类型，逻辑与上面的`ServoEx`相同，在此不再赘述。

This class is mainly for compatibility with the `CRServo` type. The logic is the same as the above `ServoEx` and will not be repeated here.

#### MotorEx

`MotorEx`是针对上层结构的`DcMotorEx`类的扩展类，实现了`RunnableStructUnit`，支持原生的PIDF参数设定，同时也通过JUC包中基于AQS的ReentrantLock实现控制的原子性。其支持例如上升滑轨所必须的同步电机，与上文的`ServoEx`通用一个`Action`类型进行对数据进行映射以增强通用性。其功能同样十分丰富，支持有功率限制的`actWithPowerLimit`方法以及可以随时设置位置的`setTemporaryPosition`方法还有其他基础方法。对于阻塞性动作，我采用了`CountDownLatch`辅助，新建一个轮询线程（略有延迟防空转）来激活阻塞的主线程。

`MotorEx` is an extension class of the `DcMotorEx` class for the upper structure. It implements `RunnableStructUnit`, supports native PIDF parameter settings, and also implements control atomicity through the AQS-based ReentrantLock in the JUC package. It supports synchronous motors required for rising slides, for example, and uses the same `Action` type as the `ServoEx` mentioned above to map data to enhance versatility. It is also very functional, supporting the power-limited `actWithPowerLimit` method and the `setTemporaryPosition` method that can set the position at any time, as well as other basic methods. For blocking actions, I used `CountDownLatch` to assist and created a new polling thread (with a slight delay to prevent idling) to activate the blocked main thread.

#### StructureLink

由上文三个实现了`RunnableStructUnit`的基础结构单元构成，通过共同的Action名称调用各自的位置，使用此方法便于集成控制，同时可以为动作添加SafetyCheck以保障动作群活动时的结构安全。该结构群线程安全，会给整个结构上锁同时给所有子系统上锁。

Composed of the three basic structural units mentioned above that implement `RunnableStructUnit`, each location is called through a common Action name. This method facilitates integrated control and allows for adding SafetyChecks to actions to ensure structural safety during action group activity. This structural group is thread-safe and locks the entire structure and all subsystems.

#### StructureGroup

主要为上锁设计，例如一个动作需要原子性的完成一连串带有阻塞的动作，可以通过调用`lockAllSubsystems`实现将其下所有结构全部上锁以保障该结构动作原子性且不影响结构群外的其他结构。

It is mainly designed for locking. For example, if an action needs to complete a series of blocking actions atomically, you can call `lockAllSubsystems` to lock all the structures under it to ensure the atomicity of the structure's actions without affecting other structures outside the structure group.

### Interface Part

#### RunnableStructUnit

对于基础结构单元定义接口，实现该接口可以使`Motor`和`Servo`以及`CRServo`方法统一，便于控制。同时降低对于结构群调用时的循环复杂度。

An interface is defined for the basic structural unit. Implementing this interface can unify the methods of `Motor` and `Servo`, making it easier to control. At the same time, it reduces the cyclic complexity of the structure group calls.

#### LockableGroup

对于多个`RunnableStructUnit`形成的`StructLink`以及`StructureGroup`强制有`lockAllSubsystem`方法，以实现递归逐级上锁.

For `StructLink` and `StructureGroup` formed by multiple `RunnableStructUnit`, a `lockAllSubsystem` method is mandatory to achieve recursive and step-by-step locking.
