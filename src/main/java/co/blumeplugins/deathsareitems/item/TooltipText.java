package co.blumeplugins.deathsareitems.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** One translated sentence per item; wrap at word boundaries, including Turkish text. */
public final class TooltipText {
    public static List<String> wrap(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int words = 0;
        for (String word : text.strip().split("\\s+")) {
            int length = line.codePointCount(0, line.length()) + 1 + word.codePointCount(0, word.length());
            if (!line.isEmpty() && (length > 42 || words >= 8)) {
                result.add(line.toString()); line.setLength(0); words = 0;
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word); words++;
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }
    public static void append(List<Component> lines, String key) {
        for (String line : wrap(Component.translatable(key).getString()))
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
    private TooltipText() {}
}
