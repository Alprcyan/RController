package moe.alprc.rcontroller;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import org.ros.address.InetAddressFactory;
import org.ros.android.AppCompatRosActivity;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.LinkedList;
import java.util.Queue;

import moe.alprc.rcontroller.publisher.OnShutdownCallback;
import moe.alprc.rcontroller.publisher.PublisherNode;
import moe.alprc.rcontroller.publisher.PublisherNodeGenerator;
import moe.alprc.rcontroller.subscriber.OnReceiveCallback;
import moe.alprc.rcontroller.subscriber.SubscriberNode;
import moe.alprc.rcontroller.subscriber.SubscriberNodeGenerator;

public class MainActivity extends AppCompatRosActivity implements NavigationView.OnNavigationItemSelectedListener {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = MainActivity.class.toString();

    private final Handler handler = new Handler();

    // objects needs to be declared constant, if accessed from inner classes.
    private static class WrapBool {
        private boolean focus = false;
    }

    // stop focusing the nestedScrollView on input.
    private class WrapBoolManager {
        Queue<WrapBool> wrapBools = new LinkedList<>();
        Queue<Button> toggleButtons = new LinkedList<>();

        // call this method when focusing on subscribe_text_view.
        void register(@Nullable WrapBool bool, @Nullable Button focusButton) {
            if (wrapBools != null) {
                wrapBools.add(bool);
            }
            if (focusButton != null) {
                toggleButtons.add(focusButton);
            }
        }

        // call this method when focusing on other elements.
        void stopFocusingAll() {
            while (wrapBools.size() > 0) {
                wrapBools.poll().focus = false;
            }
            while (toggleButtons.size() > 0) {
                toggleButtons.poll().setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));
            }
        }
    }

    private WrapBoolManager wrapBoolManager = new WrapBoolManager();

    private TopicData topicData;

    private Validator validator;

    /**
     * MainAcrivity is the default constructor, see rosjava documentation for more information.
     */
    public MainActivity() {
        super("RController", "RController");
    }

    /**
     * initInternal executes ros nodes.
     *
     * @param nodeMainExecutor is the executor, call
     *                         nodeMainExecutor.execute(NodeMain, NodeConfiguration).
     */
    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {
        // test node.
        // new Thread(new Runnable() {
        //     @Override
        //     public void run() {
        //         NodeConfiguration nodeConfiguration =
        //                 NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        //         nodeConfiguration.setMasterUri(getMasterUri());
        //         nodeConfiguration.setNodeName("turtle1/cmd_vel");
        //         PublisherNode pub1 = PublisherNodeGenerator.newInstance(new Topic("turtle1/cmd_vel", ArgumentTypeInfo
        //                 .TYPE_GEOMETRY_MSGS_TWIST, TopicData.PUB));
        //         pub1.setArgs("3.0 3.0 3.0 3.0 3.0 3.0");
        //         pub1.setTimes(100000);
        //         pub1.setFrequency(1);
        //         nodeMainExecutor.execute(pub1, nodeConfiguration);
        //
        //         try {
        //             Thread.sleep(2000);
        //         } catch (Exception e) {
        //             Log.e(TAG, Log.getStackTraceString(e));
        //         }
        //
        //         try {
        //             pub1.cancel();
        //             nodeMainExecutor.shutdownNodeMain(pub1);
        //         } catch (Exception e) {
        //             Log.e(TAG, Log.getStackTraceString(e));
        //         }
        //     }
        // }).run();
    }

    private DrawerLayout drawerLayout;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button button = navigationView.findViewById(R.id.select_master_button);
        Preconditions.checkNotNull(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nodeMainExecutorService.setMasterUri(null);
                startMasterChooser();
            }
        });

        // Load topic List before construct the listView.
        topicData = new TopicData(this);
        validator = new Validator(this);

        // Generate the list.
        listView = findViewById(R.id.list_view_main);
        listView.setAdapter(new MyAdapter(this));

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                TextView nameTextView = v.findViewById(R.id.topic_name);
                TextView categoryTextView = v.findViewById(R.id.topic_category);
                if (nameTextView == null || categoryTextView == null) {
                    v.setLongClickable(false);
                    return false;
                }
                final String topicName = (nameTextView).getText().toString();
                final String topicCategory = (categoryTextView).getText().toString();

                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert
                        .setTitle("Delete!")
                        .setMessage("Are you sure to delete topic \n\"" + topicName + "\"")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                topicData.rmTopic(topicName, topicCategory);
                                listView.setAdapter(new MyAdapter(MainActivity.this));
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alert.show();

                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_navigation, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawers();
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                break;
            case R.id.navigation_menu_add_topic:
                // show an alert dialog
                LinearLayout layout = new LinearLayout(MainActivity.this);
                final View view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.creat_new_topic_dialog, layout);
                AlertDialog.Builder addTopicAlertBuilder = new AlertDialog.Builder(this);
                addTopicAlertBuilder.setView(layout)
                        .setTitle(R.string.dialog_add_topic_confirm_title)
                        .setPositiveButton(R.string.dialog_add_topic_confirm_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String name = ((EditText) view.findViewById(R.id.create_dialog_topic_name))
                                        .getText().toString();
                                String type = ((Spinner) view.findViewById(R.id.create_dialog_topic_type))
                                        .getSelectedItem().toString();
                                String category = ((Spinner) view.findViewById(R.id.create_dialog_topic_category))
                                        .getSelectedItem().toString();
                                topicData.addTopic(name.trim(), type, category);
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.dialog_add_topic_confirm_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                break;
            case R.id.navigation_reset_topic_list:
                AlertDialog.Builder resetTopicListAlertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                resetTopicListAlertDialogBuilder
                        .setTitle(R.string.reset_topic_list)
                        .setMessage(R.string.reset_topic_list)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                topicData.reset();
                                listView.setAdapter(new MyAdapter(MainActivity.this));
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });

                resetTopicListAlertDialogBuilder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * show an Toast on clicked.
     *
     * @param item see android official documentation for more information.
     * @return see android official documentation for more information.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // TODO add listeners.
        if (id == R.id.nav_menu_item_gpsr) {
            Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean pressedBackOnce = false;

    /**
     * Double press back to exit.
     */
    @Override
    public void onBackPressed() {
        if (pressedBackOnce) {
            nodeMainExecutorService.forceShutdown();
            super.onBackPressed();
        } else {
            Toast.makeText(MainActivity.this, "Press again to exit.", Toast.LENGTH_SHORT).show();
            pressedBackOnce = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pressedBackOnce = false;
                }
            }, 2000);
        }
    }

    // Create items in the ListView
    private class MyAdapter extends ArrayAdapter<Topic> {
        private final String PUB_TAG = "PublishView";
        private final String SUB_TAG = "SubscribeView";

        // Constructor
        MyAdapter(Context context) {
            super(context, 0, topicData.getTopicList());
        }

        /**
         * getView generate the items on the list view. add listeners and
         * determine which sub level view to show.
         *
         * @param position    is the position (count from 0, the top) of the
         *                    item on the list.
         * @param convertView is the view of the item.
         * @param parent      is the parent view.
         * @return the convertView.
         */
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            // load the item view from xml file if not loaded yet.
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(getContext())
                        .inflate(R.layout.list_view_items, parent, false);
            }

            // read a topic from the TopicData class.
            final Topic topic = topicData.getTopicList().get(position);
            TextView nameText = convertView.findViewById(R.id.topic_name);
            nameText.setText(topic.getTopicName());
            ((TextView) convertView.findViewById(R.id.topic_category)).setText(topic.getTopicCategory());

            // used to set the visibility of view.
            final ViewGroup layout;

            View.OnClickListener listItemOnClickListener = null;
            switch (topic.getTopicCategory()) {
                case TopicData.PUB:
                    ((TextView) convertView.findViewById(R.id.topic_name)).setText(topic.getTopicName());
                    ((TextView) convertView.findViewById(R.id.topic_category)).setText(topic.getTopicCategory());
                    layout = (LinearLayout) convertView.findViewById(R.id.publisher_linear_layout);

                    // the EditText of arguments to public.
                    final EditText argText = convertView.findViewById(R.id.input_arguments_edit_text);
                    final EditText hzArg = convertView.findViewById(R.id.hz_edit_text);
                    final EditText secArg = convertView.findViewById(R.id.millisecond_edit_text);

                    final Button sendButton = convertView.findViewById(R.id.publish_button);
                    final Button shutdownButton = convertView.findViewById(R.id.shutdown_publisher_button);
                    final TextView statusTextView = convertView.findViewById(R.id.send_status_text_view);

                    final PublisherNode publisherNode = PublisherNodeGenerator.newInstance(topic);
                    if (publisherNode == null) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setNegativeButton("Return", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setMessage(topic.getTopicCategory() + " for " + topic
                                        .getTopicType() + " is not available now.")
                                .show();
                        // TopicData.rmTopic(topic.getTopicName(), topic.getTopicCategory());
                        // convertView.setVisibility(View.GONE);
                        nameText.append(" (not available)");
                        break;
                    }
                    final NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

                    listItemOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (layout != null) {
                                if (layout.getVisibility() == View.GONE) {
                                    layout.setVisibility(View.VISIBLE);
                                } else {
                                    layout.setVisibility(View.GONE);
                                }
                            } else {
                                Log.e(TAG, "Attempted to show the view, " +
                                        "which is belong to a unrecognizable category.");
                            }
                        }
                    };

                    // stop focusing on the subscribe TextView while typing.
                    final View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean hasFocus) {
                            if (hasFocus) {
                                // if start focusing on a EditText, stop focusing on TextView.
                                wrapBoolManager.stopFocusingAll();
                            }
                            statusTextView.setText(null);
                        }
                    };
                    argText.setOnFocusChangeListener(onFocusChangeListener);
                    hzArg.setOnFocusChangeListener(onFocusChangeListener);
                    secArg.setOnFocusChangeListener(onFocusChangeListener);
                    secArg.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                            if (i == EditorInfo.IME_ACTION_DONE) {
                                sendButton.performClick();
                                return true;
                            }
                            return false;
                        }
                    });

                    // hints are defined in ArgumentTypeInfo.java
                    argText.setHint(ArgumentTypeInfo.getHint(topic.getTopicType()));

                    sendButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String str;
                            try {
                                str = validator.process(topic, argText.getText().toString());
                            } catch (IllegalArgumentException e) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Illegal argument")
                                        .setMessage("Please check your arguents")
                                        .setNegativeButton("Return", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        }).show();
                                return;
                            }
                            int hz = Integer.parseInt(hzArg.getText().toString().equals("")
                                    ? "0"
                                    : hzArg.getText().toString());
                            int times = Integer.parseInt(secArg.getText().toString().equals("")
                                    ? "0"
                                    : secArg.getText().toString());

                            if (getMasterUri() != null) {
                                publisherNode.setArgs(str);
                                publisherNode.setFrequency(hz);
                                publisherNode.setTimes(times);
                                publisherNode.setCallback(new OnShutdownCallback() {
                                    @Override
                                    public void call() {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                shutdownButton.setEnabled(false);
                                            }
                                        });
                                    }

                                    @Override
                                    public void callShutdown() {
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                publisherNode.cancel();
                                                nodeMainExecutor.shutdownNodeMain(publisherNode);
                                            }
                                        }, 200);
                                    }
                                });

                                NodeConfiguration nodeConfiguration =
                                        NodeConfiguration
                                                .newPublic(InetAddressFactory
                                                        .newNonLoopback()
                                                        .getHostAddress());
                                nodeConfiguration.setMasterUri(getMasterUri());

                                // shutdown preview opened node or the program would crash.
                                publisherNode.cancel();
                                nodeMainExecutor.shutdownNodeMain(publisherNode);

                                nodeMainExecutor.execute(publisherNode, nodeConfiguration);
                                Log.i(PUB_TAG, "Send an " + topic.getTopicName() + " topic with arg: " + str);

                                // argText.getText().clear();

                                shutdownButton.setEnabled(hz > 0);

                                statusTextView.setText(R.string.status_published);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusTextView.setText(null);
                                    }
                                }, 2500);
                            } else {
                                Log.e(PUB_TAG, "Attempted to publish on topic: " + str + ", but Master Uri Unset.");
                                Toast.makeText(MainActivity.this, "Master Uri Unset", Toast.LENGTH_SHORT).show();
                                startMasterChooser();
                            }
                        }
                    });
                    shutdownButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // CancellableLoop cancellableLoop = publisherNode.getCancellableLoop();
                            // if (cancellableLoop != null) {
                            //     cancellableLoop.cancel();
                            //
                            //     nodeMainExecutor.shutdownNodeMain(publisherNode);
                            // }
                            publisherNode.cancel();
                            nodeMainExecutor.shutdownNodeMain(publisherNode);
                            Log.i(TAG, "Shutting down node on topic " + topic.getTopicName());
                            // shutdownButton.setEnabled(false);
                        }
                    });
                    break;

                // Subscribe
                case TopicData.SUB:
                    layout = (FrameLayout) convertView.findViewById(R.id.subscriber_frame_layout);

                    final String SHUTDOWN = getResources().getString(R.string.shutdown_button_text);
                    final String START = getResources().getString(R.string.start_button_text);

                    final WrapBool focus = new WrapBool();
                    final TextView textView = convertView.findViewById(R.id.subscribe_text);
                    final Button ssButton = convertView.findViewById(R.id.start_shutdown_subscriber_button);
                    final NestedScrollView nestedScrollView
                            = convertView.findViewById(R.id.nested_scroll_view);
                    final Button focusButton = convertView.findViewById(R.id.focus_button);

                    final SubscriberNode subscriberNode = SubscriberNodeGenerator.newInstance(topic,
                            new OnReceiveCallback<std_msgs.String>
                                    () {
                                // int count = 0;

                                @Override
                                public void call(final std_msgs.String message) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            textView.append("> ");
                                            textView.append(message.getData());
                                            textView.append("\n");
                                            // uncomment this and the int field above to enable
                                            // removing head line when it's long enough.
                                            // if (++count > 1000) {
                                            //     String string = textView.getText().toString();
                                            //     textView.getEditableText().delete(0, string.indexOf("> ",
                                            //             1));
                                            // }
                                            if (focus.focus) {
                                                nestedScrollView
                                                        .fullScroll(NestedScrollView.FOCUS_DOWN);
                                                Log.i(TAG, "focus: " + focus.focus);
                                            }
                                        }
                                    });
                                }
                            });
                    if (subscriberNode == null) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setNegativeButton("Return", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setMessage(topic.getTopicCategory() + " for " + topic
                                        .getTopicType() + " is not available now.")
                                .show();
                        // TopicData.rmTopic(topic.getTopicName(), topic.getTopicCategory());
                        // convertView.setVisibility(View.GONE);
                        nameText.append(" (not available)");
                        break;
                    }

                    listItemOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (layout != null) {
                                if (layout.getVisibility() == View.GONE) {
                                    layout.setVisibility(View.VISIBLE);
                                } else {
                                    layout.setVisibility(View.GONE);
                                }
                                focus.focus = false;
                                focusButton.setTextColor(ContextCompat.getColor(MainActivity.this, R.color
                                        .white));
                            } else {
                                Log.e(TAG, "Attempted to show the view, " +
                                        "which is belong to a unrecognizable category.");
                            }
                        }
                    };

                    wrapBoolManager.register(focus, focusButton);
                    focusButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (focus.focus) {
                                focusButton.setTextColor(ContextCompat.getColor(MainActivity.this, R.color
                                        .white));
                                focus.focus = false;
                            } else {
                                wrapBoolManager.stopFocusingAll();
                                focusButton.setTextColor(ContextCompat.getColor(MainActivity.this, R.color
                                        .colorAccent));
                                focus.focus = true;
                                wrapBoolManager.register(focus, focusButton);
                            }
                        }
                    });

                    ssButton.setText(START);
                    ssButton.setOnClickListener(
                            new View.OnClickListener() {
                                NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

                                @Override
                                public void onClick(View view) {
                                    if (ssButton.getText().toString().equals(SHUTDOWN)) {
                                        nodeMainExecutor.shutdownNodeMain(subscriberNode);
                                        ssButton.setText(START);
                                    } else {
                                        // start subscribing
                                        if (getMasterUri() != null) {

                                            NodeConfiguration nodeConfiguration =
                                                    NodeConfiguration.newPublic(InetAddressFactory
                                                            .newNonLoopback()
                                                            .getHostAddress()
                                                    );
                                            nodeConfiguration.setMasterUri(getMasterUri());

                                            nodeMainExecutor.execute(subscriberNode, nodeConfiguration);
                                            Log.i(SUB_TAG, "Add subscriber on topic: " + topic.getTopicName());

                                            ssButton.setText(SHUTDOWN);
                                        } else {
                                            Log.e(SUB_TAG, "Attempted to add a subscriber to topic: "
                                                    + topic.getTopicName() + ". But Master Uri Unset.");
                                            ssButton.setText(START);
                                            Toast.makeText(MainActivity.this, "Master Uri Unset", Toast.LENGTH_SHORT)
                                                    .show();
                                            startMasterChooser();
                                        }
                                    }
                                }
                            }
                    );
                    break;
                case TopicData.SRV:
                    break;
                case TopicData.CLI:
                    break;
                default:
                    // the category is not one of PUB, SUB, SRV, and CLI.
                    Log.e(TAG, "A strange node? How could that happen!");
                    break;
            }

            convertView.setLongClickable(true);
            convertView.setOnClickListener(listItemOnClickListener);

            return convertView;
        }
    }
}
