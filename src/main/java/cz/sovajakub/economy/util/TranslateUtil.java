package cz.sovajakub.economy.util;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateUtil {
    public static @NotNull String translateMessage(String messageToTranslate) {
        final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(messageToTranslate);
        while (matcher.find()) {
            String color = messageToTranslate.substring(matcher.start(), matcher.end());
            messageToTranslate = messageToTranslate.replace(color, ChatColor.of(color) + "");
            matcher = pattern.matcher(messageToTranslate);
        }
        return ChatColor.translateAlternateColorCodes('&', messageToTranslate);
    }
}