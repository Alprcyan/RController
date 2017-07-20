package moe.alprc.rcontroller.subscriber;

import org.ros.internal.message.Message;

/**
 * Created by alprc on 10/07/2017.
 */

public interface OnReceiveCallback<T extends Message> {
    public void call(T newMessage);
}
