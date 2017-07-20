package moe.alprc.rcontroller;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

// Load & Save topics list.
class TopicData {
    private static final String TAG = moe.alprc.rcontroller.TopicData.class.getSimpleName();

    private final static int TOPIC_NAME = 0;
    private final static int TOPIC_TYPE = 1;
    private final static int TOPIC_CATEGORY = 2;

    final static String PUB = "Publisher";
    final static String SUB = "Subscriber";
    final static String SRV = "Server";
    final static String CLI = "Client";

    // Default topics
    final static String SPRFORNLP = "/sprfornlp";
    final static String SPRFORBRAIN = "/sprforbrain";
    final static String ASPFORBRAIN = "/aspforbrain";
    private final ArrayList<Topic> defaultTopicList = new ArrayList<>(Arrays.asList(new Topic[]{
            new Topic(SPRFORNLP, ArgumentTypeInfo.TYPE_STD_MSGS_STRING, PUB),
            new Topic(SPRFORBRAIN, ArgumentTypeInfo.TYPE_STD_MSGS_STRING, PUB),
            new Topic(ASPFORBRAIN, ArgumentTypeInfo.TYPE_STD_MSGS_STRING, SUB),
    }));

    private ArrayList<Topic> topicList = null;

    private final static String FILENAME = "topics.txt";
    private File externalTopicFile;


    TopicData(Activity activity) {
        this.activity = activity;

        initExternal(activity);
    }

    private ArrayList<Topic> loadTopicListInternal() {
        ArrayList<Topic> loadList = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(activity.openFileInput(FILENAME)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 2) {
                    String[] topic = line.split(" ");
                    if (topic.length == 3) {
                        if (loadList == null) {
                            loadList = new ArrayList<>();
                        }
                        Log.i(TAG, "Read topic " + topic[TOPIC_NAME]);
                        loadList.add(new Topic(topic[TOPIC_NAME], topic[TOPIC_TYPE], topic[TOPIC_CATEGORY]));
                    } else {
                        StringBuilder sb = new StringBuilder("Wrong topic: ");
                        for (String s : topic) {
                            sb.append(s);
                            sb.append(", ");
                        }
                        Log.w(TAG, sb.toString());
                    }
                }
            }
        } catch (IOException e) {
            loadList = null;
        } finally {
            if (loadList == null || loadList.size() == 0) {
                loadList = defaultTopicList;
            }
        }

        return loadList;
    }

    private ArrayList<Topic> loadTopicListExternal() {
        ArrayList<Topic> loadList = null;

        if (externalTopicFile != null) {
            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         new FileInputStream(externalTopicFile)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 2) {
                        String[] topic = line.split(" ");
                        if (topic.length == 3) {
                            if (loadList == null) {
                                loadList = new ArrayList<>();
                            }
                            Log.i(TAG, "Read topic " + topic[TOPIC_NAME]);
                            loadList.add(new Topic(topic[TOPIC_NAME], topic[TOPIC_TYPE], topic[TOPIC_CATEGORY]));
                        } else {
                            StringBuilder sb = new StringBuilder("Wrong topic: ");
                            for (String s : topic) {
                                sb.append(s);
                                sb.append(", ");
                            }
                            Log.w(TAG, sb.toString());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                // initExternal(activity);
            }
        }


        return loadList;
    }

    // load topic list from internal file, then the external file.
    // in case of the user denied the permission request.
    private void loadTopicList() {
        topicList = loadTopicListInternal();
        ArrayList<Topic> externalList = loadTopicListExternal();
        if (externalList != null) {
            topicList = externalList;
        }
    }

    private boolean saveTopicListInternal() {
        boolean result = false;
        try (PrintWriter writer = new PrintWriter(activity.openFileOutput(FILENAME, Context.MODE_PRIVATE))) {
            for (Topic topic : topicList) {
                writer.println(topic.getTopicName() + " " + topic.getTopicType() + " " + topic.getTopicCategory());
                Log.i(TAG, "Write topic " + topic.getTopicName() + " to internal file.");
            }
            result = true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return result;
    }

    private boolean saveTopicListExternal() {
        boolean result = false;
        if (externalTopicFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(externalTopicFile))) {
                for (Topic topic : topicList) {
                    writer.println(topic.getTopicName() + " " + topic.getTopicType() + " " + topic.getTopicCategory());
                    Log.i(TAG, "Write topic " + topic.getTopicName() + " to external file.");
                }
                result = true;
            } catch (FileNotFoundException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        return result;
    }

    private void saveTopicList() {
        saveTopicListInternal();
        saveTopicListExternal();
    }

    ArrayList<Topic> getTopicList() {
        if (topicList == null) {
            loadTopicList();
        }
        return topicList;
    }

    void rmTopic(String topicName, String topicCategory) {
        for (int i = 0; i < topicList.size(); ++i) {
            Topic topic = topicList.get(i);
            if (topic.getTopicName().equals(topicName) && topic.getTopicCategory().equals(topicCategory)) {
                Log.i(TAG, "Removed topic " + topicName + ", which is a " + topic.getTopicCategory());
                topicList.remove(i);
                --i;
            }
        }

        saveTopicList();
    }

    void addTopic(String topicName, String topicType, String topicCategory) {
        addTopic(new Topic(
                topicName,
                topicType,
                topicCategory
        ));
    }

    void addTopic(Topic topic) {
        topicList.add(topic);
        saveTopicList();
    }

    private Activity activity;

    private void initExternal(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } else {
            File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                    activity.getResources().getString(R.string.app_name));
            Log.i(TAG, "Given external root is " + root.getAbsolutePath());

            if (root.exists() || root.mkdirs()) {
                File topicFile = new File(root + File.separator + FILENAME);
                try {
                    if (topicFile.exists() || topicFile.createNewFile()) {
                        Log.i(TAG, topicFile.getAbsolutePath());
                        externalTopicFile = topicFile;
                    }
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    Log.e(TAG, "Can't create external topic file.");
                    externalTopicFile = null;
                }
            }
        }
    }

    boolean reset() {
        topicList = defaultTopicList;
        saveTopicList();
        return true;
    }
}
