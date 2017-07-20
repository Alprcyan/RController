package moe.alprc.rcontroller.subscriber;

import moe.alprc.rcontroller.Log;
import moe.alprc.rcontroller.Topic;

public class SubscriberNodeGenerator {
    private static final String TAG = SubscriberNodeGenerator.class.getSimpleName();

    private static final String PATH = "moe.alprc.rcontroller.subscriber.";

    @SuppressWarnings("unchecked")
    public static SubscriberNode newInstance(Topic topic, OnReceiveCallback callback) {
        try {
            Class<? extends SubscriberNode> c =
                    (Class<? extends SubscriberNode>)
                            Class.forName(PATH + topic.getTopicType() + topic.getTopicCategory());
            SubscriberNode node = c.getConstructor().newInstance();
            node.setCallback(callback);
            node.setTopicName(topic.getTopicName());
            return node;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }
}
