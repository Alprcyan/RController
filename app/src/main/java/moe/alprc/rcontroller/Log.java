package moe.alprc.rcontroller;

/**
 * replace android.util.Log with moe.alprc.rcontroller.Log.
 */
public class Log {
    private static final boolean LOG = false;

    public static void i(String tag, String string) {
        if (LOG) {
            android.util.Log.i(tag, string);
        }
    }

    public static void e(String tag, String string) {
        if (LOG) {
            android.util.Log.e(tag, string);
        }
    }

    public static void w(String tag, String string) {
        if (LOG) {
            android.util.Log.w(tag, string);
        }
    }

    public static String getStackTraceString(Throwable e) {
        if (LOG) {
            return android.util.Log.getStackTraceString(e);
        }
        return null;
    }
}