package moe.alprc.rcontroller.subscriber;

import android.support.annotation.Nullable;

import org.ros.node.NodeMain;

public interface SubscriberNode extends NodeMain {
    void setTopicName(String topicName);

    void setCallback(@Nullable OnReceiveCallback callback);
}
