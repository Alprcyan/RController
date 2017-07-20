package moe.alprc.rcontroller.publisher;

/**
 * Created by alprc on 14/07/2017.
 */

public interface OnShutdownCallback {
    void call();

    void callShutdown();
}
