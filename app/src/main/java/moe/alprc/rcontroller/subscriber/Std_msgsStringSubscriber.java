package moe.alprc.rcontroller.subscriber;

import android.support.annotation.Nullable;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import moe.alprc.rcontroller.Log;

class Std_msgsStringSubscriber extends AbstractNodeMain implements SubscriberNode {
    private String TAG;
    private String topicName;
    private OnReceiveCallback<std_msgs.String> callback;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(topicName);
    }

    @Override
    public void setTopicName(String topicName) {
        TAG = this.getClass().getSimpleName() + " " + topicName;
        this.topicName = topicName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setCallback(@Nullable OnReceiveCallback callback) {
        this.callback = (OnReceiveCallback<std_msgs.String>) callback;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(topicName, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                callback.call(message);
                Log.i(TAG, "On receive new message: " + message.toString());
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
    }
}
