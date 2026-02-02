package org.by1337.bairdrop.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?\\s*(?:(\\d+)\\s*s)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_NUMBER_PATTERN = Pattern.compile("^\\d+$");

    public static int parseToSeconds(String input) {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }
        
        input = input.trim().replace("--force", "").trim();
        
        if (PLAIN_NUMBER_PATTERN.matcher(input).matches()) {
            return Integer.parseInt(input);
        }
        
        Matcher matcher = TIME_PATTERN.matcher(input);
        if (matcher.matches()) {
            int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return hours * 3600 + minutes * 60 + seconds;
        }
        
        return 0;
    }
    
    public static boolean hasForceFlag(String input) {
        if (input == null) return false;
        return input.toLowerCase().contains("--force");
    }

    public static String formatSeconds(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }
        
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(minutes).append("m");
        }
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(seconds).append("s");
        }
        
        return sb.toString();
    }

    public static String formatSecondsRu(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 сек";
        }
        
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(" ч");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(minutes).append(" мин");
        }
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(seconds).append(" сек");
        }
        
        return sb.toString();
    }
}
