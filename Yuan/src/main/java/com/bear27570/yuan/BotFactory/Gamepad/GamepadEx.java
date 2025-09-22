package com.bear27570.yuan.BotFactory.Gamepad;

import androidx.annotation.NonNull;

import com.bear27570.yuan.BotFactory.Model.RGB;
import com.bear27570.yuan.BotFactory.Model.RGBColor;
import com.bear27570.yuan.BotFactory.Services.RGBColorTranslator;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.jetbrains.annotations.Contract;

/**
 * 增强的Gamepad，支持附加线程，支持边缘检测和摇杆速度获取
 */
public class GamepadEx{
    public final Gamepad gamepad;
    public final GamepadButton cross;
    public final GamepadButton circle;
    public final GamepadButton square;
    public final GamepadButton triangle;
    public final GamepadButton left_bumper;
    public final GamepadButton right_bumper;
    public final GamepadButton dpad_up;
    public final GamepadButton dpad_down;

    public final GamepadButton dpad_left;
    public final GamepadButton dpad_right;
    public final GamepadButton left_stick_button;
    public final GamepadButton right_stick_button;
    public final GamepadButton touch_pad;
    public final GamepadButton ps;
    public final GamepadButton options;
    public final GamepadButton share;
    public final GamepadButton touchpad_finger_1;
    public final GamepadButton touchpad_finger_2;
    public final GamepadValue left_trigger;
    public final GamepadValue right_trigger;
    public final GamepadValue left_stick_x;
    public final GamepadValue left_stick_y;
    public final GamepadValue right_stick_x;
    public final GamepadValue right_stick_y;
    public final GamepadValue touchpad_finger_1_x;
    public final GamepadValue touchpad_finger_1_y;
    public final GamepadValue touchpad_finger_2_x;
    public final GamepadValue touchpad_finger_2_y;
    private GamepadEx(Gamepad gamepad){
        this.gamepad=gamepad;
        cross = GamepadButton.initButton();
        circle = GamepadButton.initButton();
        square = GamepadButton.initButton();
        triangle = GamepadButton.initButton();
        left_bumper = GamepadButton.initButton();
        right_bumper = GamepadButton.initButton();
        dpad_up = GamepadButton.initButton();
        dpad_down = GamepadButton.initButton();
        dpad_left = GamepadButton.initButton();
        dpad_right = GamepadButton.initButton();
        left_stick_button = GamepadButton.initButton();
        right_stick_button = GamepadButton.initButton();
        touch_pad = GamepadButton.initButton();
        ps = GamepadButton.initButton();
        options = GamepadButton.initButton();
        share = GamepadButton.initButton();
        touchpad_finger_1 = GamepadButton.initButton();
        touchpad_finger_2 = GamepadButton.initButton();
        left_trigger = GamepadValue.initValue();
        right_trigger = GamepadValue.initValue();
        left_stick_x = GamepadValue.initValue();
        left_stick_y = GamepadValue.initValue();
        right_stick_x = GamepadValue.initValue();
        right_stick_y = GamepadValue.initValue();
        touchpad_finger_1_x = GamepadValue.initValue();
        touchpad_finger_1_y = GamepadValue.initValue();
        touchpad_finger_2_x = GamepadValue.initValue();
        touchpad_finger_2_y = GamepadValue.initValue();
    }

    /**
     * GamepadEx静态工厂
     * @param gamepad 对应的原生gamepad
     * @return 返回GamepadEx实例
     */
    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static GamepadEx GetGamepadEx(Gamepad gamepad){
        return new GamepadEx(gamepad);
    }

    /**
     * 调用Gamepad的runRumbleEffect方法
     * @param effect 对应的RumbleEffect
     */
    public void RunRumbleEffect(Gamepad.RumbleEffect effect){
        gamepad.runRumbleEffect(effect);
    }

    /**
     * 调用Gamepad的rumble方法
     * @param ms 对应的毫秒数
     */
    public void rumbleMs(int ms){
        gamepad.rumble(ms);
    }

    /**
     * 调用Gamepad的rumble方法
     * @param leftRumbleIntensity 左摇杆强度
     * @param rightRumbleIntensity 右摇杆强度
     * @param durationMs 持续时间
     */
    public void rumble(double leftRumbleIntensity, double rightRumbleIntensity, int durationMs){
        gamepad.rumble(leftRumbleIntensity, rightRumbleIntensity,durationMs);
    }
    public void setLed(int red, int green, int blue,int durationMs){
        gamepad.setLedColor(red, green, blue, durationMs);
    }
    public void setLed(RGBColor color,int durationMs){
        RGB rgb = RGBColorTranslator.RGBColorToRGB(color);
        gamepad.setLedColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), durationMs);
    }
    /**
     * 根据传入的Gamepad更新按键
     */
    public void update(){
        circle.update(gamepad.circle);
        cross.update(gamepad.cross);
        square.update(gamepad.square);
        triangle.update(gamepad.triangle);
        left_bumper.update(gamepad.left_bumper);
        right_bumper.update(gamepad.right_bumper);
        dpad_up.update(gamepad.dpad_up);
        dpad_down.update(gamepad.dpad_down);
        dpad_left.update(gamepad.dpad_left);
        dpad_right.update(gamepad.dpad_right);
        touchpad_finger_1.update(gamepad.touchpad_finger_1);
        touchpad_finger_2.update(gamepad.touchpad_finger_2);
        left_stick_button.update(gamepad.left_stick_button);
        right_stick_button.update(gamepad.right_stick_button);
        touch_pad.update(gamepad.touchpad);
        ps.update(gamepad.ps);
        options.update(gamepad.options);
        share.update(gamepad.share);
        left_trigger.update(gamepad.left_trigger);
        right_trigger.update(gamepad.right_trigger);
        left_stick_x.update(gamepad.left_stick_x);
        left_stick_y.update(gamepad.left_stick_y);
        right_stick_x.update(gamepad.right_stick_x);
        right_stick_y.update(gamepad.right_stick_y);
        touchpad_finger_1_x.update(gamepad.touchpad_finger_1_x);
        touchpad_finger_1_y.update(gamepad.touchpad_finger_1_y);
        touchpad_finger_2_x.update(gamepad.touchpad_finger_2_x);
        touchpad_finger_2_y.update(gamepad.touchpad_finger_2_y);
    }
}
