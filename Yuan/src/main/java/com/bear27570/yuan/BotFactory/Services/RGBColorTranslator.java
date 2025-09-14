package com.bear27570.yuan.BotFactory.Services;

import com.bear27570.yuan.BotFactory.Model.RGB;
import com.bear27570.yuan.BotFactory.RGBColor;

import static com.bear27570.yuan.BotFactory.RGBColor.*;

import java.util.Map;

public class RGBColorTranslator {
    private static final Map<RGBColor,RGB> map = Map.ofEntries(
            Map.entry(Red, new RGB(255, 0, 0)),
            Map.entry(Orange, new RGB(255, 165, 0)),
            Map.entry(Yellow, new RGB(255, 255, 0)),
            Map.entry(Green, new RGB(0, 255, 0)),
            Map.entry(Blue, new RGB(0, 0, 255)),
            Map.entry(Indigo, new RGB(75, 0, 130)),
            Map.entry(Violet, new RGB(238, 130, 238)),
            Map.entry(White, new RGB(255, 255, 255)),
            Map.entry(Black, new RGB(0, 0, 0))
    );
    public static RGB RGBColorToRGB(RGBColor color) {
        return map.get(color);
    }
}
