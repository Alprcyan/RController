package moe.alprc.rcontroller.publisher;

import android.support.annotation.Nullable;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import moe.alprc.rcontroller.Log;

/**
 * This is a sample Publisher that can works with the turtlesim.
 * create a new node with titleName /turtle1/cmd_vel and see
 * official wiki for more information
 */
class Geometry_msgsTwistPublisher extends AbstractNodeMain implements PublisherNode {
    private String TAG;

    private double[] args;
    private String topicName;
    private int times;
    private int waitTime;
    private OnShutdownCallback callback;
    private CancellableLoop cancellableLoop;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(topicName);
    }

    @Override
    public void setArgs(String arg) {
        String[] args = arg.split(" ");

        if (args.length != 6) {
            Log.e(TAG, "Wrong arguments");
            args = null;
        } else {
            this.args = new double[]{
                    Double.parseDouble(args[0]),
                    Double.parseDouble(args[1]),
                    Double.parseDouble(args[2]),
                    Double.parseDouble(args[3]),
                    Double.parseDouble(args[4]),
                    Double.parseDouble(args[5]),
            };
        }
    }

    @Override
    public void setTopicName(String topicName) {
        TAG = Geometry_msgsTwistPublisher.class.getSimpleName() + " " + topicName;
        this.topicName = topicName;
    }

    @Override
    public void setFrequency(int hz) {
        hz = hz > 0 ? hz : -hz;
        this.waitTime = (hz != 0 ? 1000 / hz : hz);
    }

    @Override
    public void setTimes(int times) {
        this.times = times;
    }

    @Override
    public void setCallback(OnShutdownCallback callback) {
        this.callback = callback;
    }

    @Override
    @Nullable
    public CancellableLoop getCancellableLoop() {
        return cancellableLoop;
    }

    private long stopValue;

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        if (args != null) {
            final Publisher<geometry_msgs.Twist> publisher = connectedNode.newPublisher(topicName, geometry_msgs
                    .Twist._TYPE);

            publisher.setLatchMode(true);

            if (waitTime == 0) {
                geometry_msgs.Twist twist = publisher.newMessage();

                geometry_msgs.Vector3 angular = twist.getAngular();
                angular.setX(args[0]);
                angular.setY(args[1]);
                angular.setZ(args[2]);
                geometry_msgs.Vector3 linear = twist.getLinear();
                linear.setX(args[3]);
                linear.setY(args[4]);
                linear.setZ(args[5]);
                twist.setAngular(angular);
                twist.setLinear(linear);

                publisher.publish(twist);
            } else {
                if (times == 0) {
                    stopValue = ((long) Integer.MIN_VALUE) - 1L;
                } else {
                    stopValue = 0L;
                }

                cancellableLoop = new CancellableLoop() {
                    @Override
                    protected void loop() throws InterruptedException {
                        geometry_msgs.Twist twist = publisher.newMessage();

                        geometry_msgs.Vector3 angular = twist.getAngular();
                        angular.setX(args[0]);
                        angular.setY(args[1]);
                        angular.setZ(args[2]);
                        geometry_msgs.Vector3 linear = twist.getLinear();
                        linear.setX(args[3]);
                        linear.setY(args[4]);
                        linear.setZ(args[5]);
                        twist.setAngular(angular);
                        twist.setLinear(linear);

                        publisher.publish(twist);
                        Log.i(TAG, "published " + twist.toString());
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
        if (cancellableLoop != null) {
            cancellableLoop.cancel();
        }
        if (callback != null) {
            callback.call();
        }
        Log.i(TAG, "node terminated.");
    }

    @Override
    public void cancel() {
        stopValue = times - 1;
    }
}
