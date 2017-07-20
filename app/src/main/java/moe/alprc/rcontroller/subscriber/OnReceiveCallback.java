package moe.alprc.rcontroller.subscriber;

import org.ros.internal.message.Message;

public interface OnReceiveCallback<T extends Message> {
    void call(T newMessage);
}
