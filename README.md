# Yuan-Developed-By-27570

Stable version :1.2.0

"Yuan"是针对于FTRST Tech Challenge的sdk进行开发的，首个结合JUC并发编程的Java程序架构。利用了`ThreaPooolExecutor`线程池，以`Task`为基础形成了带有优先权和冲突策略的强大任务管理器。本架构使用了GPL-3.0License，你可以随意修改代码，但请注意基于此项目的代码必须开源！如果你想要使用该框架，详情请见Tutorial.md

"Yuan" was developed for the FTRST Tech Challenge SDK and is the first Java framework to integrate concurrent programming with JUC. It utilizes the `ThreadPoolExecutor` thread pool and builds a powerful task manager with priority and conflict management based on `Task`. This framework is licensed under the GPL-3.0 license. You are free to modify the code, but please note that code based on this project must be open source! If you would like to use this framework, please see Tutorial.md for details.

---

## How to get it?

我们已经将代码上传至Maven Central，你只需要在你的`build.dependencies.gradle`中（或`build.gradle.kts`如果你使用的是Kotlin的Gradle）添加如下依赖即可

```groovy
dependencies {
    implementation 'io.github.bear27570:Yuan:1.2.0'
}
```

We have uploaded the code to Maven Central. You only need to add the following dependency to your `build.dependencies.gradle` (or `build.gradle.kts` if you are using Kotlin's Gradle)

```groovy
dependencies {
    implementation 'io.github.bear27570:Yuan:1.2.0'
}
```

---

## About "Yuan"

### Hardware Part

#### GamepadEx

`GamepadEx`类是对于原生`Gamepad`的一个再封装，扩展了更多功能。其下有两个子类：`GamepadButton`和`GamepadValue`，分别对应着手柄上的按钮和可以记录按下位置的摇杆和扳机。同时保留了原生`Gamepad`的设置震动和LED灯带颜色的功能，你也可以使用我自建的`RGBColor`枚举类型简单的调用一些常见颜色。

The `GamepadEx` class is a repackage of the native `Gamepad` class, extending its functionality. It contains two subclasses: `GamepadButton` and `GamepadValue`, corresponding to the controller's buttons and the joystick and trigger, which record press positions. While the native `Gamepad` functionality for setting vibration and LED strip colors is retained, you can also use the custom `RGBColor` enumeration type to easily access common colors.

#### ServoEx

`ServoEx`对应着原生的`Servo`类，实现了`RunnableStructUnit`接口和`Lockable`结构，使用了JUC中基于AQS框架的ReentrantLock，通过对该控制机构上线程锁，防止一个连贯动作被修改（区别于打断，打断会释放锁并执行其他需要锁的逻辑）。其功能丰富，支持舵机同步，使用Builder Param实现链式调用以适应输入数据的不确定性。使用了`HashMap`将`Action`这一枚举类型的名称与对应的舵机位置进行映射，方便管理舵机数据，可以提升用户端程序的可读性，用户也可以通过读取有限状态机`ServoState`获取舵机当前的状态。同时对设置临时数据保持兼容性，以保证其可以用于视觉识别等场景。支持阻塞性动作，通过设置的舵机转速判断当前需要运行的角度对应需要阻塞的时间（线性的）。对于以DECODE赛季为例的可能需要队伍搭建云台控制发射机构的场景，我提供了`actWithVel(double DegPerSec)`方法和其阻塞方法，用一个200Hz的线程不断修正目标点实现PWM舵机的转速控制。

`ServoEx` corresponds to the native `Servo` class, implements the `RunnableStructUnit` interface and `Lockable` interface, and uses the ReentrantLock based on the AQS framework in JUC. By setting a thread lock on the control mechanism, a coherent action is prevented from being modified (different from interruption, which releases the lock and executes other logic that requires the lock). It is rich in functions and uses Builder Param to implement chain calls to adapt to the uncertainty of input data. `HashMap` is used to map the name of the `Action` enumeration type to the corresponding servo position, which facilitates the management of servo data and improves the readability of the user-side program. Users can also obtain the current status of the servo by reading the finite state machine `ServoState`. At the same time, compatibility is maintained for setting temporary data to ensure that it can be used in scenarios such as visual recognition.Supports blocking action, and determines the current required running angle and the corresponding blocking time (linear) by setting the servo speed.For scenarios like the DECODE season, where teams might need to build a gimbal-controlled launch mechanism, I provided the `actWithVel(double DegPerSec)` method and its blocking method, using a 200Hz thread to continuously correct the target point to achieve PWM servo speed control.

#### MotorEx

`MotorEx`是针对上层结构的`DcMotorEx`类的扩展类，实现了`RunnableStructUnit`和，支持原生的PIDF参数设定，同时也通过JUC包中基于AQS的ReentrantLock实现控制的原子性。其支持例如上升滑轨所必须的同步电机，与上文的`ServoEx`通用一个`Action`类型进行对数据进行映射以增强通用性。其功能同样十分丰富，支持有功率限制的`actWithPowerLimit`方法以及可以随时设置位置的`setTemporaryPosition`方法还有其他基础方法。对于阻塞性动作，以200Hz轮询动作是否完成以保障CPU性能。

`MotorEx` is an extension class of the `DcMotorEx` class for the upper structure. It implements `RunnableStructUnit`, supports native PIDF parameter settings, and also implements control atomicity through the AQS-based ReentrantLock in the JUC package. It supports synchronous motors required for rising slides, for example, and uses the same `Action` type as the `ServoEx` mentioned above to map data to enhance versatility. It is also very functional, supporting the power-limited `actWithPowerLimit` method and the `setTemporaryPosition` method that can set the position at any time, as well as other basic methods.

#### StructureLink

由上文三个实现了`RunnableStructUnit`的基础结构单元构成，通过共同的Action名称调用各自的位置，使用此方法便于集成控制，同时可以为动作添加SafetyCheck以保障动作群活动时的结构安全。该结构群线程安全，会给整个结构上锁同时给所有子系统上锁。

Composed of the three basic structural units mentioned above that implement `RunnableStructUnit`, each location is called through a common Action name. This method facilitates integrated control and allows for adding SafetyChecks to actions to ensure structural safety during action group activity. This structural group is thread-safe and locks the entire structure and all subsystems.

### Interface Part

#### RunnableStructUnit

对于基础结构单元定义接口，实现该接口可以使`Motor`和`Servo``方法统一，便于控制。同时降低对于结构群调用时的循环复杂度。

An interface is defined for the basic structural unit. Implementing this interface can unify the methods of `Motor` and `Servo`, making it easier to control. At the same time, it reduces the cyclic complexity of the structure group calls.

#### Lockable

包括所有硬件(组)，辅助任务管理的冲突策略，降低管理复杂度。
