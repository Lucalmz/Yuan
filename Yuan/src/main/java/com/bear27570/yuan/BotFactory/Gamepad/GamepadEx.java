package com.bear27570.yuan.BotFactory.Gamepad;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.jetbrains.annotations.Contract;

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
    @NonNull
    @Contract(value = "_ -> new", pure = true)
    public static GamepadEx GetGamepadEx(Gamepad gamepad){
        return new GamepadEx(gamepad);
    }
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
