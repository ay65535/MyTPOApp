package jp.ac.ritsumei.cs.ubi.ats.mytpoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import jp.ac.ritsumei.cs.ubi.logger.client.api.matching.CompareOperation;
import jp.ac.ritsumei.cs.ubi.logger.client.api.matching.EventDetectionRequest;
import jp.ac.ritsumei.cs.ubi.logger.client.api.utility.MatchingConstants;


public class MainActivity extends ActionBarActivity {
    private TextView textViewA, textViewB, textViewC, textViewD, textViewE;

    /**
     * イベントを受け取るレシーバー
     */
    private EventReciever ev = new EventReciever();
    private final Object monitor = new Object(); // 専用のロック用オブジェクト
    public class EventReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BroadcastIntentAction.EVENT_ON_NOT_LEAVE.equals(action)){
                textViewA.setText(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) +
                        " | " + intent.getStringExtra("text"));
            }
            if(BroadcastIntentAction.EVENT_ON_LEAVE.equals(action)){
                textViewB.setText(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) +
                        " | " + intent.getStringExtra("text"));
            }
            if(BroadcastIntentAction.EVENT_GPS_LISTEN_FINE.equals(action)){
                textViewC.setText(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) +
                        " | " + intent.getStringExtra("text"));
            }
            if(BroadcastIntentAction.EVENT_GPS_LISTEN_NOT_FINE.equals(action)){
                textViewD.setText(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) +
                        " | " + intent.getStringExtra("text"));
            }
            if(BroadcastIntentAction.EVENT_ON_CHANGE_BUILDING.equals(action)){
                textViewE.setText(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) +
                        " | " + intent.getStringExtra("text"));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 遷移前状態のイベント検知要求を削除
        //sendBroadcast(new Intent(MatchingConstants.QUERY_ALL_REMOVE));

        textViewA = (TextView) findViewById(R.id.textView1);
        textViewB = (TextView) findViewById(R.id.textView2);
        textViewC = (TextView) findViewById(R.id.textView3);
        textViewD = (TextView) findViewById(R.id.textView4);
        textViewE = (TextView) findViewById(R.id.textView5);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(mButtonListener);

        synchronized (monitor) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BroadcastIntentAction.EVENT_ON_NOT_LEAVE);
            filter.addAction(BroadcastIntentAction.EVENT_ON_LEAVE);
            filter.addAction(BroadcastIntentAction.EVENT_GPS_LISTEN_FINE);
            filter.addAction(BroadcastIntentAction.EVENT_GPS_LISTEN_NOT_FINE);
            filter.addAction(BroadcastIntentAction.EVENT_ON_CHANGE_BUILDING);
            registerReceiver(ev, filter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    // ボタン押下イベントリスナー
    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            requesetEventAndSetting(v.getContext());
        }
    };

    // イベント検知機構
    private void requesetEventAndSetting(Context context) {
        /*** 受信待機状態におけるセンシング制御への設定変更要求 ***/
//        // 放置非放置判定のセンシング再開
//        Intent settingChangeRequestIntent =
//                Task.convertToIntent(Task.createResumeTask(Task.TargetType.DATAKIND_LEAVE));
//        context.sendBroadcast(settingChangeRequestIntent);
//        // GPS測位の再開
//        settingChangeRequestIntent =
//                Task.convertToIntent(Task.createResumeTask(Task.TargetType.DATAKIND_LOCATION));
//        context.sendBroadcast(settingChangeRequestIntent);

        /*** 受信待機状態における他状態遷移のためのイベント検知要求 ***/
        // 遷移前状態のイベント検知要求を削除
        context.sendBroadcast(new Intent(MatchingConstants.QUERY_ALL_REMOVE));

        EventDetectionRequest query[] = new EventDetectionRequest[5];
        // Leave:ON_NOT_LEAVE(0) が検知されたら"移動（非放置）"
        Intent replyIntentOnNotLeave = new Intent(BroadcastIntentAction.EVENT_ON_NOT_LEAVE);
        replyIntentOnNotLeave.putExtra("text", "\"非放置\" 状態です。");
        CompareOperation predicateOnNotLeave = CompareOperation.create(
                MatchingConstants.DT_INT, MatchingConstants.SN_LEAVE,
                MatchingConstants.VN_STATE, MatchingConstants.EQUAL, Constants.ON_NOT_LEAVE);
        predicateOnNotLeave.setNumberOfDetection(1);
        query[0] = new EventDetectionRequest(replyIntentOnNotLeave, predicateOnNotLeave);

        // Leave:ON_LEAVE(1) が検知されたら"放置"
        Intent replyIntentOnLeave = new Intent(BroadcastIntentAction.EVENT_ON_LEAVE);
        replyIntentOnLeave.putExtra("text", "\"放置\" 状態です。");
        CompareOperation predicateOnLeave = CompareOperation.create(
                MatchingConstants.DT_INT, MatchingConstants.SN_LEAVE,
                MatchingConstants.VN_STATE, MatchingConstants.EQUAL, Constants.ON_LEAVE);
        predicateOnLeave.setNumberOfDetection(1); // (1|MatchingConstants.KEEP)
        query[1] = new EventDetectionRequest(replyIntentOnLeave, predicateOnLeave);

        // 測位結果の Accuracy が ACCURACY_THRES 以下の GPS が取得されたら"GPS 受信良好"
        Intent replyIntentGpsFine = new Intent(BroadcastIntentAction.EVENT_GPS_LISTEN_FINE);
        replyIntentGpsFine.putExtra("text", "GPS 受信良好です。");
        CompareOperation predicateGpsFine = CompareOperation.create(
                MatchingConstants.DT_FLOAT, MatchingConstants.SN_LOCATION,
                MatchingConstants.VN_ACCURACY, MatchingConstants.SMALLER_THAN, Constants.ACCURACY_THRES);
        predicateGpsFine.setNumberOfDetection(1);
        query[2] = new EventDetectionRequest(replyIntentGpsFine, predicateGpsFine);

        // 測位結果の Accuracy が ACCURACY_THRES 以上の GPS が取得されたら"GPS 受信不良"
        Intent replyIntentGpsNotFine = new Intent(BroadcastIntentAction.EVENT_GPS_LISTEN_NOT_FINE);
        replyIntentGpsNotFine.putExtra("text", "GPS 受信不良です。");
        CompareOperation predicateGpsNotFine = CompareOperation.create(
                MatchingConstants.DT_FLOAT, MatchingConstants.SN_LOCATION,
                MatchingConstants.VN_ACCURACY, MatchingConstants.LARGER_THAN, Constants.ACCURACY_THRES);
        predicateGpsNotFine.setNumberOfDetection(1);
        query[3] = new EventDetectionRequest(replyIntentGpsNotFine, predicateGpsNotFine);

        // 測位結果の Lat, Lng が latlng の範囲内の場合""
        double latlng[] = {34.9794,135.9639,34.9797,135.9650};
        Intent locReplayIntent = new Intent(BroadcastIntentAction.EVENT_ON_CHANGE_BUILDING);
        locReplayIntent.putExtra("text", "測位結果の Lat, Lng が latlng の範囲内です。");
        CompareOperation predicateChangeBuilding = CompareOperation.create(
                MatchingConstants.DT_DOUBLE, MatchingConstants.SN_LOCATION,
                MatchingConstants.VN_LAT_LNG, MatchingConstants.SMALLER_THAN, latlng);
        predicateChangeBuilding.setNumberOfDetection(1);
        query[4] = new EventDetectionRequest(locReplayIntent, predicateChangeBuilding);

        Intent eventDetectionRequestIntent = new Intent(MatchingConstants.QUERY);
        String key = MatchingConstants.getKey(MatchingConstants.NOTIFICATION);
        eventDetectionRequestIntent.putExtra(key, query);
        context.sendBroadcast(eventDetectionRequestIntent);
    }

    public static class BroadcastIntentAction {
        public static final String EVENT_ON_LEAVE = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_on_leave";
        public static final String EVENT_ON_NOT_LEAVE = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_not_leave";
        public static final String EVENT_GPS_LISTEN = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_gps_listen";
        public static final String EVENT_GPS_NOT_LISTEN = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_gps_not_listen";
        public static final String EVENT_GPS_LISTEN_FINE = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_gps_listen_fine";
        public static final String EVENT_GPS_LISTEN_NOT_FINE = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_gps_listen_not_fine";

        public static final String LOCATION_PROVIDER_CHANGED = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.location_provider_changed";
        public static final String SERVICE_ON_CREATE_OR_DESTROY = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.service_on_create_or_destroy";
        public static final String SENSING_STATE_CHANGED = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.sensing_state_changed";
        public static final String ALARM_MANAGER_POLLING = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.alarm_manager_polling";
        public static final String REQUEST_WIFI_RESTART = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.request_wifi_restart";
        public static final String REQUEST_SERVICE_RESTART = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.request_service_restart";

        public static final String BROADCAST_CACHE_REQUEST = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.broadcast_cache_request";
        public static final String START_SERVICE_REQUEST = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.start_service_request";
        public static final String STOP_SERVICE_REQUEST = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.stop_service_request";
        public static final String RELOAD_SETTINGS_REQUEST = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.reaload_settings_request";

        public static final String EVENT_ON_CHANGE_BUILDING = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.bintent.event_on_change_building";
    }

    public static class Constants {
        public static final String SHARED_PREFERENCE_NAME = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.preference";
        public static final String STATE_PREF_KEY = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.preference.state";
        public static final String ISRUNNING_PREF_KEY = "jp.ac.ritsumei.cs.ubi.ats.testtpoapp.preference.isrunning";

        public static final String TXT_EXTENSION = ".txt";
        public static final String STATE_LOG_FILE_NAME = "stateLog";
        public static final String INVALID_EVENT_LOG_FILE_NAME = "invalidEventLog";
        public static final String FILE_NAME = "state_log.txt";
        public static final SimpleDateFormat SHARED_SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

        public static final int ON_LEAVE = 1;
        public static final int ON_NOT_LEAVE = 0;
        public static final float ACCURACY_THRES = 64;
        public static final long GPS_LISTEN_TIME_THRESHOLD = 60 * 1000;
    }
}
