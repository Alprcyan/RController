package moe.alprc.rcontroller.publisher;

import android.support.annotation.Nullable;

import org.ros.concurrent.CancellableLoop;
import org.ros.node.NodeMain;

/**
 * Created by alprc on 14/07/2017.
 * On creating a new publisher, update values/arrays.xml and ArgumentTypeInfo.java as well.
 * Make sure the new publisher has a default constructor.
 * It's recommend to add a new subscriber for the same message type at the same time.
 */

public interface PublisherNode extends NodeMain {
    void setArgs(String args);

    void setFrequency(int hz);

    void setTimes(int times);

    void setTopicName(String topicName);

    void setCallback(@Nullable OnShutdownCallback callback);

    @Nullable CancellableLoop getCancellableLoop();

    void cancel();
}
