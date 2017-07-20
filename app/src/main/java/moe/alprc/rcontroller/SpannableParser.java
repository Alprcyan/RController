package moe.alprc.rcontroller;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

/**
 * This class would make response text of subscriber rich.
 * However it's not completed yet.
 */
class SpannableParser {
    private static final String TAG = SpannableParser.class.getSimpleName();

    public static CharSequence parseSpannable(String string) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (string != null) {
            for (int start = 0; start < string.length(); ) {
                int end = string.indexOf("\\033[", start);
                if (end == -1) {
                    end = string.length();
                    builder.append(string.substring(start, end));
                    break;
                } else {
                    builder.append(string.substring(start, end));
                    int m = string.indexOf("m", end), codeStart = end + 1;

                    if (m == -1) {
                        builder.append(string.substring(end, m));
                        break;
                    }

                    end = string.indexOf("\\033[", m);
                    if (end == -1) {
                        end = string.length();
                    }

                    int startBefore = builder.length();
                    builder.append(string.substring(m + 1, end));
                    builder.setSpan(
                            what(string.substring(codeStart, m)),
                            startBefore,
                            builder.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    start = end;
                }
            }
        }

        return builder;
    }

    private static Object what(String codeStr) {
        Object o = null;
        StringBuilder builder = new StringBuilder("");
        int code = Integer.parseInt(codeStr);
        switch (code) {
            case 33:
                break;
        }

        return o;
    }
}
