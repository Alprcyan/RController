package moe.alprc.rcontroller;

import moe.alprc.rcontroller.Log;

import java.util.regex.Pattern;

/**
 * Created by alprc on 14/07/2017.
 * It's necessary to update values/arrays.xml as well
 * And, consider adding new Publisher and Subscriber for the new type.
 *
 * Make sure all names here are same to that in arrays.xml.
 */

class ArgumentTypeInfo {
    private static final String TAG = ArgumentTypeInfo.class.getSimpleName();

    static final String TYPE_STD_MSGS_STRING = "Std_msgsString";
    static final String TYPE_GEOMETRY_MSGS_TWIST = "Geometry_msgsTwist";

    static String getHint(String type) {
        String hint;
        switch (type) {
            case TYPE_STD_MSGS_STRING:
                hint = "<arguments>";
                break;
            case TYPE_GEOMETRY_MSGS_TWIST:
                hint = "<double> * 6";
                break;
            // add more here.
            default:
                hint = "Error";
                Log.e(TAG, "getHint(): Get wrong argument type.");
        }
        return hint;
    }
}
