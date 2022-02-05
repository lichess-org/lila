package lila.common.base;

public class StringUtils {
    private static final char[] DIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f'
    };

    public static String safeJsonString(final String s) {
        final char[] sArr = s.toCharArray();
        final int len = sArr.length;
        StringBuilder sb = null;
        int copyIdx = 0;
        for (int i = 0; i < len; i++) {
            char c = sArr[i];
            if (c >= ' ' && c <= '~') switch(c) {
                case '<': case '>': case '&': case '"':
                case '\'': case '\\': case '`':
                    break; // cur char is bad, escape it
                default:
                    continue; // char is ok, continue scan
            }
            // This code runs when char is either out of alphanumeric range OR
            // char is restricted.
            if (sb == null) {
              sb = new StringBuilder(c <= '~' ? len + 22 : len * 6 + 2);
              sb.append('"');
            }
            sb.append(s, copyIdx, i);
            sb.append(new char[] { '\\', 'u',
                DIGITS[c >>> 12],
                DIGITS[(c >>> 8) & 0xf],
                DIGITS[(c >>> 4) & 0xf],
                DIGITS[c & 0xf]
            });
            copyIdx = i + 1;
        }
        if (sb == null) return "\"" + s + "\"";
        sb.append(s, copyIdx, len);
        sb.append('"');
        return sb.toString();
    }

    public static String escapeHtmlRaw(final String s) {
        final char[] sArr = s.toCharArray();
        for (int i = 0, end = sArr.length; i < end; i++) {
            switch (sArr[i]) {
                case '<': case '>': case '&': case '"': case '\'':
                  final StringBuilder sb = new StringBuilder(end + 20);
                  sb.append(s, 0, i);
                  escapeHtmlRaw(sb, sArr, i, end);
                  return sb.toString();
            }
        }
        return s;
    }

    public static void escapeHtmlRaw(final StringBuilder sb, final char[] sArr,
        int start, final int end) {

        for (int i = start; i < end; i++) {
            switch (sArr[i]) {
                case '<': case '>': case '&': case '"': case '\'':
                  sb.append(sArr, start, i - start);
                  switch (sArr[i]) {
                      case '<': sb.append("&lt;"); break;
                      case '>': sb.append("&gt;"); break;
                      case '&': sb.append("&amp;"); break;
                      case '"': sb.append("&quot;"); break;
                      case '\'': sb.append("&#39;");
                  }
                  start = i + 1;
            }
        }
        sb.append(sArr, start, end - start);
    }

    public static String removeGarbageChars(final String s) {
        final char[] sArr = s.toCharArray();
        final int size = sArr.length;
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            final char c = sArr[i];
            switch (c) {
              case '\u200b':
              case '\u200c':
              case '\u200d':
              case '\u200e':
              case '\u200f':
              case '\u202e':
              case '\u1160': 
                break;
              default:
                sb.append(c);
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
        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == len2 && s1.equals(s2)) return 0;
        if (len1 < len2) return levenshtein(s2, s1);
        if (len2 == 0) return len1;

        char[] c1 = s1.toCharArray();
        char[] c2 = s2.toCharArray();

        // create two work vectors of integer distances
        int[] v0 = new int[len2 + 1];
        int[] v1 = new int[len2 + 1];
        int[] vtemp;

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i <= len2; i++) v0[i] = i;

        for (int i = 0; i < len1; i++) {
            // calculate v1 (current row distances) from the previous row v0
            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < len2; j++)
                v1[j + 1] = Math.min(
                    c1[i] == c2[j] ? v0[j] : v0[j] + 1, // substitute
                    Math.min(v0[j + 1], v1[j]) + 1  // remove / insert
                );


            // Flip references to current and previous row
            vtemp = v0;
            v0 = v1;
            v1 = vtemp;

        }

        return v0[len2];
    }
}
