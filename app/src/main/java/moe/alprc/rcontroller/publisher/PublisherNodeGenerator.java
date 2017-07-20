package moe.alprc.rcontroller.publisher;

import moe.alprc.rcontroller.Log;
import moe.alprc.rcontroller.Topic;

public class PublisherNodeGenerator {
    private static final String TAG = PublisherNodeGenerator.class.getSimpleName();

    private static final String PATH = "moe.alprc.rcontroller.publisher.";

    @SuppressWarnings("unchecked")
    public static PublisherNode newInstance(Topic topic) {
        String className = PATH + topic.getTopicType() + topic.getTopicCategory();
        Log.i(TAG, "Attempting to construct: " + className);
        try {
            Class<? extends PublisherNode> c =
                    (Class<? extends PublisherNode>)
                            Class.forName(className);
            PublisherNode node = c.getConstructor().newInstance();
            node.setTopicName(topic.getTopicName());
            return node;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }
}
