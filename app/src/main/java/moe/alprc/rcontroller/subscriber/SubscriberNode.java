package moe.alprc.rcontroller.subscriber;

import android.support.annotation.Nullable;

import org.ros.node.NodeMain;

/**
 * Created by alprc on 17/07/2017.
 */

public interface SubscriberNode extends NodeMain {
    void setTopicName(String topicName);

    void setCallback(@Nullable OnReceiveCallback callback);
}
