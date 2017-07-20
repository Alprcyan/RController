package moe.alprc.rcontroller;

/**
 * Created by alprc on 14/07/2017.
 * topic name is the name like "/rosout"
 * topic type is the argument type liks "Std_msgsString"
 * topicCategory is either TopicData.PUB or SUB, may add SRV or CLI in the future.
 */

public class Topic {
    private String topicName;
    private String topicType;
    private String topicCategory;

    Topic(String topicName, String topicType, String topicCategory) {
        this.topicName = topicName;
        this.topicType = topicType;
        this.topicCategory = topicCategory;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getTopicType() {
        return topicType;
    }

    public String getTopicCategory() {
        return topicCategory;
    }
}
