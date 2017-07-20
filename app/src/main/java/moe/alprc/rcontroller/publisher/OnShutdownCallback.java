package moe.alprc.rcontroller.publisher;

public interface OnShutdownCallback {
    void call();

    void callShutdown();
}
