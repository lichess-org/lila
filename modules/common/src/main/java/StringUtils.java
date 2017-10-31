package lila.common.base;

public class StringUtils {
    public static String safeJsonString(String s) {
        char[] sArr = s.toCharArray();
        int len = sArr.length;
        int sizeGuess = len > 0 && sArr[0] <= '~' ? 1 : 6;
        StringBuilder sb = new StringBuilder(sizeGuess * len + 2);

        sb.append('"');
        for (char c : sArr) {
            boolean safe = c >= ' ' && c <= '~';
            if (safe) switch(c) {
                case '<': case '>': case '&': case '"':
                case '\'': case '\\': case '`': case '/':
                  safe = false;
            }

            if (safe) {
                sb.append(c);
            } else {
                sb.append(c > '\u00ff' ?
                    c > '\u0fff' ? "\\u" : "\\u0" :
                    c > '\u000f' ? "\\u00" : "\\u000");
                sb.append(Integer.toHexString(c));
            }
        }
        sb.append('"');

        return sb.toString();
    }

    // Extensively profiled with jmh. It is faster to pass
    // around the String reference than to reuse the char
    // array.
    public static String escapeHtmlUnsafe(String s) {
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': case '>': case '&': case '"': case '\'':
                  return realHtmlEscape(s);
            }
        }
        return s; 
    }

    private static String realHtmlEscape(String s) {
        char[] sArr = s.toCharArray();
        StringBuilder sb = new StringBuilder(sArr.length + 10);
        for (char c : sArr) {
            switch (c) {
                case '<': sb.append("&lt;"); continue;
                case '>': sb.append("&gt;"); continue;
                case '&': sb.append("&amp;"); continue;
                case '"': sb.append("&quot;"); continue;
                case '\'': sb.append("&#39;"); continue;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}