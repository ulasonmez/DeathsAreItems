package co.blumeplugins.deathsareitems.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Balance translated descriptions at word boundaries, without changing their wording. */
public final class TooltipText {
    private static final int MAX_WIDTH = 220, MAX_WORDS = 8;
    public static List<String> wrap(String text) {
        if (text.isBlank()) return List.of();
        String[] words = text.strip().split("\\s+");
        int n = words.length;
        int[] widths = new int[n + 1], characters = new int[n + 1];
        for (int i = 0; i < n; i++) {
            widths[i + 1] = widths[i] + width(words[i]) + 4;
            characters[i + 1] = characters[i] + words[i].codePointCount(0, words[i].length()) + 1;
        }
        int lines = 1, start = 0;
        for (int i = 1; i < n; i++) {
            if (i - start >= MAX_WORDS || widths[i + 1] - widths[start] - 4 > MAX_WIDTH || characters[i + 1] - characters[start] - 1 > 42) { lines++; start = i; }
        }
        double target = (widths[n] - 4D * lines) / lines;
        double[][] cost = new double[lines + 1][n + 1];
        int[][] previous = new int[lines + 1][n + 1];
        for (double[] row : cost) Arrays.fill(row, Double.POSITIVE_INFINITY);
        cost[0][0] = 0;
        for (int line = 1; line <= lines; line++) for (int end = line; end <= n; end++) {
            for (int begin = Math.max(line - 1, end - MAX_WORDS); begin < end; begin++) {
                int length = widths[end] - widths[begin] - 4;
                if ((length > MAX_WIDTH || characters[end] - characters[begin] - 1 > 42) && end - begin > 1) continue;
                double candidate = cost[line - 1][begin] + Math.pow(length - target, 2);
                if (candidate < cost[line][end]) { cost[line][end] = candidate; previous[line][end] = begin; }
            }
        }
        List<String> result = new ArrayList<>();
        int end = n;
        for (int line = lines; line > 0; line--) {
            int begin = previous[line][end];
            result.add(0, String.join(" ", Arrays.copyOfRange(words, begin, end))); end = begin;
        }
        return result;
    }
    private static int width(String word) {
        // Vanilla's default font widths; accented/Turkish letters use the normal glyph width.
        return word.codePoints().map(c -> "il.,!:'|".indexOf(c) >= 0 ? 2 : c == 'I' || c == '[' || c == ']' ? 4
                : c == 't' || c == 'f' || c == '(' || c == ')' ? 5 : 6).sum();
    }
    public static void append(List<Component> lines, String key) {
        for (String line : wrap(Component.translatable(key).getString()))
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
    private TooltipText() {}
}
