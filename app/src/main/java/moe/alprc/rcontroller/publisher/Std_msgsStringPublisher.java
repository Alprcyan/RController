package moe.alprc.rcontroller.publisher;

import android.support.annotation.Nullable;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import moe.alprc.rcontroller.Log;

class Std_msgsStringPublisher extends AbstractNodeMain implements PublisherNode {
    private String TAG;

    private String args;
    private String topicName;
    private int waitTime;
    private int times;
    private OnShutdownCallback callback;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(topicName);
    }

    @Override
    public void setArgs(String args) {
        this.args = args;
    }

    @Override
    public void setFrequency(int hz) {
        hz = hz > 0 ? hz : -hz;
        this.waitTime = (hz != 0 ? 1000 / hz : hz);
    }

    @Override
    public void setTopicName(String topicName) {
        TAG = Std_msgsStringPublisher.class.getSimpleName() + " " + topicName;
        this.topicName = topicName;
    }

    @Override
    public void setTimes(int times) {
        this.times = times;
    }

    @Override
    public void setCallback(@Nullable OnShutdownCallback callback) {
        this.callback = callback;
    }

    private long stopValue;

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (args != null) {
            final Publisher<std_msgs.String> publisher = connectedNode.newPublisher(topicName, std_msgs.String._TYPE);

            publisher.setLatchMode(true);

            if (waitTime == 0) {
                std_msgs.String string = publisher.newMessage();

                string.setData(args);

                publisher.publish(string);
            } else {
                if (times == 0) {
                    stopValue = ((long) Integer.MIN_VALUE) - 1L;
                } else {
                    stopValue = 0L;
                }

                CancellableLoop cancellableLoop = new CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {
                        std_msgs.String string = publisher.newMessage();

                        string.setData(args);
                        publisher.publish(string);
                        Log.i(TAG, "published " + string.toString());
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            Log.e(TAG, e.toString());
                        }
                        if (stopValue >= (long) (--times)) {
                            cancel();
                            Log.i(TAG, "Loop Canceled.");
                            if (callback != null) {
                                callback.callShutdown();
                            }
                        }
                    }
                };

                connectedNode.executeCancellableLoop(cancellableLoop);
            }
        }
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
        cancel();
        if (callback != null) {
            callback.call();
        }
        Log.i(TAG, "node terminated.");
    }

    @Override
    public void cancel() {
        stopValue = Long.MAX_VALUE;
    }
}
