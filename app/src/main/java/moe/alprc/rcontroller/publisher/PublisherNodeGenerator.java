package moe.alprc.rcontroller.publisher;

import moe.alprc.rcontroller.Log;
import moe.alprc.rcontroller.Topic;

/**
 * Created by alprc on 17/07/2017.
 */

public class PublisherNodeGenerator {
    private static final String TAG = PublisherNodeGenerator.class.getSimpleName();

    private static final String path = "moe.alprc.rcontroller.publisher.";

    public static PublisherNode newInstance(Topic topic) {
        String className = path + topic.getTopicType() + topic.getTopicCategory();
        Log.i(TAG, "Attempting to construct: " + className);
        try {
            Class<? extends PublisherNode> c =
                    (Class<? extends PublisherNode>)
                            Class.forName(className);
            PublisherNode node = c.getConstructor().newInstance();
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
