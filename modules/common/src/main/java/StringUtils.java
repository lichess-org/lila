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
                case '\'': case '\\': case '`':
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

    public static String escapeHtml(String s) {
        // Extensively profiled with jmh. It is faster to pass
        // around the String reference than to reuse the char
        // array.
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': case '>': case '&': case '"': case '\'':
                  return reallyEscapeHtml(s);
            }
        }
        return s;
    }

    private static String reallyEscapeHtml(String s) {
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

    /**
     * https://github.com/tdebatty/java-string-similarity/blob/master/src/main/java/info/debatty/java/stringsimilarity/Levenshtein.java
     *
     * The Levenshtein distance, or edit distance, between two words is the
     * minimum number of single-character edits (insertions, deletions or
     * substitutions) required to change one word into the other.
     *
     * http://en.wikipedia.org/wiki/Levenshtein_distance
     *
     * It is always at least the difference of the sizes of the two strings.
     * It is at most the length of the longer string.
     * It is zero if and only if the strings are equal.
     * If the strings are the same size, the Hamming distance is an upper bound
     * on the Levenshtein distance.
     * The Levenshtein distance verifies the triangle inequality (the distance
     * between two strings is no greater than the sum Levenshtein distances from
     * a third string).
     *
     * Implementation uses dynamic programming (Wagner-Fischer algorithm), with
     * only 2 rows of data. The space requirement is thus O(m) and the algorithm
     * runs in O(mn).
     *
     * @param s1 The first string to compare.
     * @param s2 The second string to compare.
     * @return The computed Levenshtein distance.
     * @throws NullPointerException if s1 or s2 is null.
     */
    public static final int levenshtein(final String s1, final String s2) {
        if (s1 == null) {
            throw new NullPointerException("s1 must not be null");
        }

        if (s2 == null) {
            throw new NullPointerException("s2 must not be null");
        }

        if (s1.equals(s2)) {
            return 0;
        }

        if (s1.length() == 0) {
            return s2.length();
        }

        if (s2.length() == 0) {
            return s1.length();
        }

        // create two work vectors of integer distances
        int[] v0 = new int[s2.length() + 1];
        int[] v1 = new int[s2.length() + 1];
        int[] vtemp;

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++) {
            v0[i] = i;
        }

        for (int i = 0; i < s1.length(); i++) {
            // calculate v1 (current row distances) from the previous row v0
            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < s2.length(); j++) {
                int cost = 1;
                if (s1.charAt(i) == s2.charAt(j)) {
                    cost = 0;
                }
                v1[j + 1] = Math.min(
                        v1[j] + 1,              // Cost of insertion
                        Math.min(
                                v0[j + 1] + 1,  // Cost of remove
                                v0[j] + cost)); // Cost of substitution
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            //System.arraycopy(v1, 0, v0, 0, v0.length);

            // Flip references to current and previous row
            vtemp = v0;
            v0 = v1;
            v1 = vtemp;

        }

        return v0[s2.length()];
    }
}
