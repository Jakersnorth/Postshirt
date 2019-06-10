package com.adafruit.bluefruit.le.connect.app;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.adafruit.bluefruit.le.connect.R;
import com.adafruit.bluefruit.le.connect.ble.UartPacket;
import com.adafruit.bluefruit.le.connect.ble.UartPacketManagerBase;
import com.adafruit.bluefruit.le.connect.ble.central.BlePeripheral;
import com.adafruit.bluefruit.le.connect.ble.central.BlePeripheralUart;
import com.adafruit.bluefruit.le.connect.ble.central.BleScanner;
import com.adafruit.bluefruit.le.connect.ble.central.UartPacketManager;
import com.adafruit.bluefruit.le.connect.style.UartStyle;
import com.adafruit.bluefruit.le.connect.utils.DialogUtils;
import com.adafruit.bluefruit.le.connect.utils.DuoSignalPoint;
import com.adafruit.bluefruit.le.connect.utils.SVM;
import com.adafruit.bluefruit.le.connect.utils.SignalPoint;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PostureDetectorFragment extends ConnectedPeripheralFragment implements UartPacketManagerBase.Listener {
    // Log
    private final static String TAG = DataCollectorFragment.class.getSimpleName();

    // Constants
    private final static String kPreferences = "UartActivity_prefs";
    private final static String kPreferences_eol = "eol";
    private final static String kPreferences_eolCharactersId = "eolCharactersId";

    // Graph UI
    private GraphView graphView;
    private LineGraphSeries<DataPoint> mSeriesX1;
    private LineGraphSeries<DataPoint> mSeriesY1;
    private LineGraphSeries<DataPoint> mSeriesZ1;

    private LineGraphSeries<DataPoint> mSeriesX2;
    private LineGraphSeries<DataPoint> mSeriesY2;
    private LineGraphSeries<DataPoint> mSeriesZ2;

    // Test UI
    private TextView alertText;
    private TextView walkingText;

    // Number of data points to display in graph
    private int GRAPH_SIZE = 200;

    // Channel for pushing notifications
    private String CHANNEL_ID = "Posture alert";

    // Data
    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());
    protected UartPacketManagerBase mUartData;
    protected List<BlePeripheralUart> mBlePeripheralsUart = new ArrayList<>();
    private Map<String, Integer> mColorForPeripheral = new HashMap<>();
    private String mMultiUartSendToPeripheralIdentifier = null;     // null = all peripherals

    // Signal Collection
    private List<SignalPoint> signals;
    private List<DuoSignalPoint> duosignals;
    private List<DataPoint> signalX1;
    private List<DataPoint> signalY1;
    private List<DataPoint> signalZ1;

    private List<DataPoint> signalX2;
    private List<DataPoint> signalY2;
    private List<DataPoint> signalZ2;
    private StringBuffer packetParseBuffer;

    // Posture classifier
    private SVM postureclf;

    double[][] p_vectors = {{466.0, 421.0, 483.0, 545.0, 406.0, 500.0}, {479.0, 419.0, 488.0, 543.0, 399.0, 502.0}, {482.0, 418.0, 491.0, 524.0, 394.0, 504.0}, {496.0, 426.0, 490.0, 539.0, 399.0, 500.0}, {461.0, 412.0, 480.0, 557.0, 397.0, 491.0}, {483.0, 419.0, 487.0, 544.0, 399.0, 502.0}, {497.0, 426.0, 490.0, 539.0, 398.0, 500.0}, {483.0, 419.0, 488.0, 543.0, 399.0, 501.0}, {498.0, 426.0, 491.0, 538.0, 398.0, 500.0}, {496.0, 426.0, 491.0, 539.0, 398.0, 501.0}, {487.0, 419.0, 488.0, 545.0, 400.0, 503.0}, {463.0, 423.0, 501.0, 550.0, 408.0, 509.0}, {498.0, 426.0, 491.0, 538.0, 398.0, 500.0}, {463.0, 423.0, 475.0, 544.0, 405.0, 480.0}, {483.0, 409.0, 487.0, 528.0, 397.0, 505.0}, {496.0, 425.0, 490.0, 540.0, 398.0, 501.0}, {496.0, 426.0, 490.0, 539.0, 398.0, 500.0}, {497.0, 428.0, 491.0, 540.0, 398.0, 501.0}, {487.0, 404.0, 486.0, 531.0, 393.0, 504.0}, {466.0, 432.0, 480.0, 544.0, 411.0, 485.0}, {468.0, 420.0, 470.0, 547.0, 407.0, 483.0}, {495.0, 420.0, 490.0, 540.0, 400.0, 504.0}, {458.0, 420.0, 501.0, 550.0, 407.0, 503.0}, {491.0, 416.0, 489.0, 538.0, 398.0, 505.0}, {500.0, 427.0, 492.0, 537.0, 398.0, 501.0}, {498.0, 426.0, 491.0, 538.0, 398.0, 501.0}, {467.0, 419.0, 504.0, 550.0, 408.0, 507.0}, {498.0, 427.0, 491.0, 539.0, 398.0, 501.0}, {486.0, 416.0, 488.0, 543.0, 400.0, 502.0}, {480.0, 417.0, 485.0, 522.0, 394.0, 508.0}, {487.0, 414.0, 489.0, 536.0, 397.0, 504.0}, {498.0, 426.0, 491.0, 538.0, 398.0, 500.0}, {455.0, 404.0, 488.0, 556.0, 393.0, 495.0}, {496.0, 427.0, 490.0, 540.0, 398.0, 501.0}, {464.0, 413.0, 500.0, 545.0, 402.0, 503.0}, {496.0, 426.0, 491.0, 539.0, 398.0, 501.0}, {461.0, 418.0, 496.0, 551.0, 402.0, 501.0}, {478.0, 417.0, 488.0, 541.0, 399.0, 501.0}, {463.0, 410.0, 492.0, 557.0, 397.0, 501.0}, {460.0, 403.0, 493.0, 558.0, 393.0, 498.0}, {493.0, 406.0, 487.0, 540.0, 396.0, 504.0}, {480.0, 419.0, 488.0, 544.0, 399.0, 502.0}, {481.0, 417.0, 487.0, 541.0, 399.0, 502.0}, {483.0, 418.0, 490.0, 526.0, 397.0, 504.0}, {466.0, 417.0, 492.0, 547.0, 406.0, 503.0}, {496.0, 426.0, 490.0, 539.0, 399.0, 501.0}, {496.0, 428.0, 491.0, 540.0, 398.0, 501.0}, {477.0, 419.0, 487.0, 541.0, 398.0, 501.0}, {496.0, 426.0, 491.0, 539.0, 398.0, 501.0}, {479.0, 419.0, 487.0, 543.0, 399.0, 501.0}, {498.0, 427.0, 491.0, 538.0, 398.0, 500.0}, {465.0, 419.0, 497.0, 551.0, 407.0, 505.0}, {469.0, 424.0, 468.0, 546.0, 411.0, 485.0}, {465.0, 421.0, 489.0, 548.0, 407.0, 499.0}, {465.0, 419.0, 475.0, 544.0, 406.0, 483.0}, {467.0, 417.0, 501.0, 552.0, 408.0, 511.0}, {497.0, 427.0, 491.0, 538.0, 398.0, 501.0}, {460.0, 417.0, 476.0, 551.0, 399.0, 489.0}, {486.0, 419.0, 488.0, 545.0, 399.0, 502.0}, {484.0, 415.0, 489.0, 531.0, 398.0, 504.0}, {464.0, 424.0, 480.0, 548.0, 408.0, 487.0}, {497.0, 428.0, 491.0, 540.0, 398.0, 501.0}, {496.0, 426.0, 491.0, 539.0, 398.0, 501.0}, {480.0, 414.0, 490.0, 523.0, 394.0, 507.0}, {499.0, 428.0, 492.0, 538.0, 398.0, 501.0}, {495.0, 425.0, 490.0, 539.0, 399.0, 501.0}, {478.0, 419.0, 487.0, 541.0, 399.0, 501.0}, {464.0, 428.0, 477.0, 544.0, 410.0, 483.0}, {481.0, 419.0, 487.0, 543.0, 399.0, 501.0}, {496.0, 426.0, 490.0, 539.0, 398.0, 500.0}, {481.0, 417.0, 489.0, 525.0, 395.0, 501.0}, {498.0, 426.0, 491.0, 538.0, 398.0, 501.0}, {511.0, 438.0, 488.0, 539.0, 404.0, 518.0}, {496.0, 428.0, 491.0, 540.0, 398.0, 501.0}, {463.0, 417.0, 498.0, 549.0, 401.0, 500.0}, {497.0, 426.0, 490.0, 538.0, 398.0, 500.0}, {483.0, 420.0, 490.0, 542.0, 399.0, 505.0}, {487.0, 423.0, 492.0, 543.0, 400.0, 506.0}, {482.0, 427.0, 490.0, 542.0, 400.0, 502.0}, {486.0, 422.0, 493.0, 541.0, 399.0, 507.0}, {486.0, 409.0, 455.0, 531.0, 404.0, 483.0}, {487.0, 422.0, 492.0, 543.0, 399.0, 506.0}, {468.0, 420.0, 510.0, 532.0, 406.0, 499.0}, {478.0, 421.0, 475.0, 537.0, 407.0, 470.0}, {491.0, 425.0, 491.0, 544.0, 400.0, 506.0}, {483.0, 423.0, 488.0, 544.0, 400.0, 502.0}, {490.0, 425.0, 492.0, 544.0, 400.0, 506.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 502.0}, {483.0, 419.0, 490.0, 543.0, 400.0, 505.0}, {487.0, 424.0, 493.0, 542.0, 400.0, 507.0}, {482.0, 424.0, 489.0, 543.0, 400.0, 503.0}, {483.0, 427.0, 488.0, 547.0, 401.0, 506.0}, {483.0, 426.0, 487.0, 547.0, 401.0, 505.0}, {475.0, 413.0, 492.0, 528.0, 404.0, 489.0}, {483.0, 426.0, 489.0, 547.0, 401.0, 506.0}, {470.0, 413.0, 470.0, 537.0, 397.0, 477.0}, {481.0, 427.0, 488.0, 548.0, 400.0, 505.0}, {492.0, 426.0, 492.0, 547.0, 403.0, 505.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 503.0}, {482.0, 423.0, 488.0, 543.0, 400.0, 503.0}, {476.0, 407.0, 500.0, 526.0, 398.0, 478.0}, {487.0, 424.0, 494.0, 542.0, 399.0, 508.0}, {481.0, 427.0, 489.0, 547.0, 401.0, 506.0}, {475.0, 396.0, 511.0, 532.0, 396.0, 506.0}, {488.0, 425.0, 491.0, 545.0, 401.0, 507.0}, {487.0, 424.0, 493.0, 543.0, 400.0, 509.0}, {482.0, 425.0, 488.0, 543.0, 399.0, 502.0}, {488.0, 424.0, 492.0, 542.0, 400.0, 508.0}, {482.0, 424.0, 487.0, 543.0, 399.0, 502.0}, {469.0, 407.0, 497.0, 524.0, 396.0, 480.0}, {482.0, 423.0, 488.0, 543.0, 400.0, 503.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 502.0}, {482.0, 423.0, 488.0, 543.0, 399.0, 502.0}, {486.0, 424.0, 493.0, 542.0, 400.0, 508.0}, {476.0, 409.0, 487.0, 527.0, 403.0, 484.0}, {487.0, 425.0, 490.0, 546.0, 401.0, 507.0}, {483.0, 423.0, 488.0, 544.0, 400.0, 503.0}, {487.0, 424.0, 493.0, 542.0, 400.0, 508.0}, {486.0, 423.0, 492.0, 544.0, 399.0, 508.0}, {488.0, 424.0, 491.0, 543.0, 400.0, 506.0}, {487.0, 424.0, 489.0, 546.0, 401.0, 503.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 503.0}, {478.0, 414.0, 490.0, 528.0, 403.0, 481.0}, {481.0, 427.0, 487.0, 547.0, 401.0, 506.0}, {483.0, 381.0, 475.0, 532.0, 383.0, 483.0}, {493.0, 395.0, 487.0, 529.0, 406.0, 478.0}, {483.0, 424.0, 488.0, 543.0, 400.0, 502.0}, {484.0, 420.0, 493.0, 544.0, 400.0, 506.0}, {470.0, 422.0, 497.0, 523.0, 409.0, 483.0}, {488.0, 421.0, 494.0, 543.0, 399.0, 510.0}, {455.0, 437.0, 526.0, 541.0, 410.0, 495.0}, {483.0, 420.0, 492.0, 542.0, 400.0, 506.0}, {482.0, 423.0, 488.0, 543.0, 399.0, 503.0}, {483.0, 421.0, 487.0, 543.0, 400.0, 502.0}, {483.0, 423.0, 489.0, 543.0, 400.0, 503.0}, {486.0, 424.0, 493.0, 542.0, 400.0, 507.0}, {490.0, 427.0, 488.0, 546.0, 400.0, 504.0}, {483.0, 425.0, 490.0, 546.0, 401.0, 508.0}, {487.0, 424.0, 493.0, 543.0, 400.0, 508.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 503.0}, {474.0, 424.0, 466.0, 523.0, 401.0, 462.0}, {483.0, 426.0, 488.0, 548.0, 401.0, 506.0}, {474.0, 423.0, 468.0, 531.0, 402.0, 473.0}, {485.0, 424.0, 485.0, 544.0, 400.0, 503.0}, {469.0, 381.0, 487.0, 535.0, 378.0, 480.0}, {483.0, 427.0, 489.0, 548.0, 401.0, 507.0}, {483.0, 424.0, 489.0, 545.0, 399.0, 506.0}, {482.0, 424.0, 488.0, 544.0, 399.0, 503.0}, {487.0, 398.0, 452.0, 530.0, 389.0, 473.0}, {483.0, 423.0, 489.0, 545.0, 400.0, 502.0}, {483.0, 425.0, 488.0, 543.0, 400.0, 502.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 502.0}, {482.0, 424.0, 488.0, 543.0, 400.0, 502.0}, {482.0, 426.0, 488.0, 547.0, 400.0, 504.0}};
    double[][] p_coefficients = {{-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.6418299958652596, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.531642108031466, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -0.9999936822665796, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, 1.0, 1.0, 1.0, 1.0, 0.765676927561057, 1.0, 0.008044392592342749, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.6080407999721852, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.025851737127746514, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7658519289099773, 1.0, 1.0, 1.0, 1.0, 1.0}};
    double[] p_intercepts = {-185.67060676549988};
    int[] p_weights = {76, 78};

    // Walking classifier
    private SVM walkingclf;

    double[][] w_vectors = {{3.0, 5.0, 8.0, 20.0, 5.0, 39.0}, {8.0, 4.0, 22.0, 9.0, 6.0, 25.0}, {21.0, 17.0, 6.0, 31.0, 11.0, 9.0}, {20.0, 26.0, 24.0, 26.0, 20.0, 27.0}};
    double[][] w_coefficients = {{-0.00014314813689451572, -0.001145955298572242, -0.0019227280693190347, 0.0032118315047857927}};
    double[] w_intercepts = {3.347961453223221};
    int[] w_weights = {3, 1};

    // Length of segments to classify (should be similar to amount used for training model)
    int segmentLength = 20;

    // region Fragment Lifecycle
    public static PostureDetectorFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        PostureDetectorFragment fragment = new PostureDetectorFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        return fragment;
    }

    public PostureDetectorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes
        setRetainInstance(true);

        // Initialize data structures for graphs
        signals = new LinkedList<SignalPoint>();
        duosignals = new LinkedList<DuoSignalPoint>();
        signalX1 = new LinkedList<DataPoint>();
        signalY1 = new LinkedList<DataPoint>();
        signalZ1 = new LinkedList<DataPoint>();

        signalX2 = new LinkedList<DataPoint>();
        signalY2 = new LinkedList<DataPoint>();
        signalZ2 = new LinkedList<DataPoint>();

        packetParseBuffer = new StringBuffer();

        // Initialize classifiers
        postureclf = new SVM(2, 2, p_vectors, p_coefficients, p_intercepts, p_weights, "linear", 0.5, 0.0, 3);
        walkingclf = new SVM(2, 2, w_vectors, w_coefficients, w_intercepts, w_weights, "linear", 0.5, 0.0, 3);

        // Notification channel must be created for later push notification
        createNotificationChannel();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_posturedetector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertText = (TextView) view.findViewById(R.id.alertText);
        walkingText = (TextView) view.findViewById(R.id.walkingText);

        // Assign data structures to graph view with useful styling
        graphView = (GraphView) view.findViewById(R.id.graph);
        mSeriesX1 = new LineGraphSeries<>(signalX1.toArray(new DataPoint[0]));
        mSeriesX1.setColor(Color.RED);
        mSeriesY1 = new LineGraphSeries<>(signalY1.toArray(new DataPoint[0]));
        mSeriesY1.setColor(Color.GREEN);
        mSeriesZ1 = new LineGraphSeries<>(signalZ1.toArray(new DataPoint[0]));
        mSeriesZ1.setColor(Color.BLUE);

        mSeriesX2 = new LineGraphSeries<>(signalX2.toArray(new DataPoint[0]));
        mSeriesX2.setColor(Color.CYAN);
        mSeriesY2 = new LineGraphSeries<>(signalY2.toArray(new DataPoint[0]));
        mSeriesY2.setColor(Color.MAGENTA);
        mSeriesZ2 = new LineGraphSeries<>(signalZ2.toArray(new DataPoint[0]));
        mSeriesZ2.setColor(Color.YELLOW);

        graphView.addSeries(mSeriesX1);
        graphView.addSeries(mSeriesY1);
        graphView.addSeries(mSeriesZ1);
        graphView.addSeries(mSeriesX2);
        graphView.addSeries(mSeriesY2);
        graphView.addSeries(mSeriesZ2);

        //mSeriesX = new LineGraphSeries<>(signalX.toArray(new DataPoint[0]));
        //graphView.addSeries(mSeriesX);

        final Context context = getContext();
        // Read local preferences
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();        // update options menu with current values
            }
        }

        // Setup Uart
        setupUart();
    }

    protected boolean isInMultiUartMode() {
        return mBlePeripheral == null;
    }

    protected void setupUart() {
        // Init
        Context context = getContext();
        if (context == null) {
            return;
        }
        mUartData = new UartPacketManager(context, this, true, null);

        // Colors assigned to peripherals
        final int[] colors = UartStyle.defaultColors();

        // Enable uart
        if (isInMultiUartMode()) {
            mColorForPeripheral.clear();        // Reset colors assigned to peripherals
            List<BlePeripheral> connectedPeripherals = BleScanner.getInstance().getConnectedPeripherals();
            for (int i = 0; i < connectedPeripherals.size(); i++) {
                final boolean isLastPeripheral = i == connectedPeripherals.size() - 1;
                BlePeripheral blePeripheral = connectedPeripherals.get(i);
                mColorForPeripheral.put(blePeripheral.getIdentifier(), colors[i % colors.length]);

                if (!BlePeripheralUart.isUartInitialized(blePeripheral, mBlePeripheralsUart)) {
                    BlePeripheralUart blePeripheralUart = new BlePeripheralUart(blePeripheral);
                    mBlePeripheralsUart.add(blePeripheralUart);
                    blePeripheralUart.uartEnable(mUartData, status -> {

                        String peripheralName = blePeripheral.getName();
                        if (peripheralName == null) {
                            peripheralName = blePeripheral.getIdentifier();
                        }

                        String finalPeripheralName = peripheralName;
                        mMainHandler.post(() -> {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                // Done
                                Log.d(TAG, "Uart enabled for: " + finalPeripheralName);

                            } else {
                                //WeakReference<BlePeripheralUart> weakBlePeripheralUart = new WeakReference<>(blePeripheralUart);
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                AlertDialog dialog = builder.setMessage(String.format(getString(R.string.uart_error_multipleperiperipheralinit_format), finalPeripheralName))
                                        .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                                        /*
                                            BlePeripheralUart strongBlePeripheralUart = weakBlePeripheralUart.get();
                                        if (strongBlePeripheralUart != null) {
                                            strongBlePeripheralUart.disconnect();
                                        }*/
                                        })
                                        .show();
                                DialogUtils.keepDialogOnOrientationChanges(dialog);
                            }
                        });

                    });
                }
            }
        } else {
            if (!BlePeripheralUart.isUartInitialized(mBlePeripheral, mBlePeripheralsUart)) { // If was not previously setup (i.e. orientation change)
                mColorForPeripheral.clear();        // Reset colors assigned to peripherals
                mColorForPeripheral.put(mBlePeripheral.getIdentifier(), colors[0]);
                BlePeripheralUart blePeripheralUart = new BlePeripheralUart(mBlePeripheral);
                mBlePeripheralsUart.add(blePeripheralUart);
                blePeripheralUart.uartEnable(mUartData, status -> mMainHandler.post(() -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // Done
                        Log.d(TAG, "Uart enabled");
                    } else {
                        WeakReference<BlePeripheralUart> weakBlePeripheralUart = new WeakReference<>(blePeripheralUart);
                        Context context1 = getContext();
                        if (context1 != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context1);
                            AlertDialog dialog = builder.setMessage(R.string.uart_error_peripheralinit)
                                    .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                                        BlePeripheralUart strongBlePeripheralUart = weakBlePeripheralUart.get();
                                        if (strongBlePeripheralUart != null) {
                                            strongBlePeripheralUart.disconnect();
                                        }
                                    })
                                    .show();
                            DialogUtils.keepDialogOnOrientationChanges(dialog);
                        }
                    }
                }));
            }
        }
    }

    // On reception of packets convert to a string, add to string buffer and parse for complete signals
    @Override
    public void onUartPacket(UartPacket packet) {
        //Log.d(TAG, "packet received");
        packet.getData();
        String sPacket = new String(packet.getData());
        //Log.d(TAG, sPacket);
        packetParseBuffer.append(sPacket);
        parsePacketBufferForSignal();
    }

    // Parse the ongoing string buffer for complete signals and add them to list
    private void parsePacketBufferForSignal() {
        int signalStart = -1;
        int signalEnd = -1;
        // check for start and end identifiers of signals
        for(int i = 0; i < packetParseBuffer.length(); i++) {
            if(packetParseBuffer.charAt(i) == '>' && signalStart == -1) {
                signalStart = i;
            }
            if(packetParseBuffer.charAt(i) == '<' && signalEnd == -1) {
                signalEnd = i;
            }
        }
        // check if complete signal found and restructure for classification
        if(signalStart != -1 && signalEnd != -1 && signalStart < signalEnd) {
            String segment = packetParseBuffer.substring(signalStart + 1, signalEnd);
            String[] data = segment.split(",");
            // check for single accelerometer signal
            if(data.length == 4) {
                SignalPoint nSignal = new SignalPoint(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]));
                //update graph view
                signals.add(nSignal);
                signalX1.add(nSignal.getXDP());
                signalY1.add(nSignal.getYDP());
                signalZ1.add(nSignal.getZDP());
                int minGX = signalX1.size() - GRAPH_SIZE < 0 ? 0 : signalX1.size() - GRAPH_SIZE;
                mSeriesX1.resetData(signalX1.subList(minGX, signalX1.size()).toArray(new DataPoint[0]));
                mSeriesY1.resetData(signalY1.subList(minGX, signalY1.size()).toArray(new DataPoint[0]));
                mSeriesZ1.resetData(signalZ1.subList(minGX, signalZ1.size()).toArray(new DataPoint[0]));
            }
            // two accelerometers
            else {
                DuoSignalPoint nSignal = new DuoSignalPoint(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]), Integer.parseInt(data[5]), Integer.parseInt(data[6]));
                duosignals.add(nSignal);
                List<DataPoint> graphPoints = nSignal.getDataPoints();
                // update graph view
                signalX1.add(graphPoints.get(0));
                signalY1.add(graphPoints.get(1));
                signalZ1.add(graphPoints.get(2));
                signalX2.add(graphPoints.get(3));
                signalY2.add(graphPoints.get(4));
                signalZ2.add(graphPoints.get(5));
                int minGX = signalX1.size() - GRAPH_SIZE < 0 ? 0 : signalX1.size() - GRAPH_SIZE;
                mSeriesX1.resetData(signalX1.subList(minGX, signalX1.size()).toArray(new DataPoint[0]));
                mSeriesY1.resetData(signalY1.subList(minGX, signalY1.size()).toArray(new DataPoint[0]));
                mSeriesZ1.resetData(signalZ1.subList(minGX, signalZ1.size()).toArray(new DataPoint[0]));
                mSeriesX2.resetData(signalX2.subList(minGX, signalX2.size()).toArray(new DataPoint[0]));
                mSeriesY2.resetData(signalY2.subList(minGX, signalY2.size()).toArray(new DataPoint[0]));
                mSeriesZ2.resetData(signalZ2.subList(minGX, signalZ2.size()).toArray(new DataPoint[0]));

                // check if enough signals have been received for prediction
                if(duosignals.size() == segmentLength) {
                    // calculate posture by averaging predictions over segment
                    int postureAvg = 0;
                    for(DuoSignalPoint dp: duosignals) {
                        postureAvg += postureclf.predict(dp.getFeatures());
                    }
                    // check if good posture, update text, and remove notification if present
                    if (postureAvg > segmentLength / 2) {
                        alertText.setText(R.string.posturedetector_good_posture);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.getContext());

                        // notificationId is a unique int for each notification that you must define
                        notificationManager.cancel(58);
                    }
                    // bad posture detected, change text view and push notification
                    else {
                        alertText.setText(R.string.posturedetector_bad_posture);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getContext(), CHANNEL_ID)
                                .setSmallIcon(R.drawable.tab_posturedetector_icon)
                                .setContentTitle(getString(R.string.posturedetector_posture_alert))
                                .setContentText(getString(R.string.posturedetector_bad_posture))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                .setAutoCancel(true);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.getContext());

                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(58, builder.build());
                    }
                    // check for current movement
                    if(walkingclf.predict(collectDiffsFeatures(duosignals)) == 1) {
                        walkingText.setText(R.string.posturedetector_walking);
                    } else {
                        walkingText.setText(R.string.posturedetector_sitting_still);
                    }
                    duosignals.clear();
                }
            }
            // prevent memory overflow by clearing memory when over twice graph size
            if(signalX1.size() > 2 * GRAPH_SIZE) {
                signalX1.subList(0, GRAPH_SIZE).clear();
                signalY1.subList(0, GRAPH_SIZE).clear();
                signalZ1.subList(0, GRAPH_SIZE).clear();
                signalX2.subList(0, GRAPH_SIZE).clear();
                signalY2.subList(0, GRAPH_SIZE).clear();
                signalZ2.subList(0, GRAPH_SIZE).clear();
            }
            // clear parsed section of packet data
            packetParseBuffer.delete(0, signalEnd + 1);
            parsePacketBufferForSignal();
            return;
        }
        //clear out piece of buffer with no start (this shouldn't happen
        if(signalEnd < signalStart) {
            packetParseBuffer.delete(0, signalStart);
        }
    }

    // Required for pushing notifications
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Posture alert";
            String description = "Posture alert";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setVibrationPattern(new long[]{1, 1, 1});
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = this.getContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Calculate differences between min and max of each component in list of signals, required to match training feature extraction
    private double[] collectDiffsFeatures(List<DuoSignalPoint> signals) {
        double[] features = new double[6];
        double x1_max = signals.get(0).getFeatures()[0];
        double x1_min = x1_max;

        double y1_max = signals.get(0).getFeatures()[1];
        double y1_min = y1_max;

        double z1_max = signals.get(0).getFeatures()[2];
        double z1_min = z1_max;

        double x2_max = signals.get(0).getFeatures()[3];
        double x2_min = x2_max;

        double y2_max = signals.get(0).getFeatures()[4];
        double y2_min = y2_max;

        double z2_max = signals.get(0).getFeatures()[5];
        double z2_min = z2_max;


        for(DuoSignalPoint d_p : signals) {
            double[] comp = d_p.getFeatures();
            x1_max = comp[0] > x1_max ? comp[0] : x1_max;
            x1_min = comp[0] < x1_min ? comp[0] : x1_min;

            y1_max = comp[1] > y1_max ? comp[1] : y1_max;
            y1_min = comp[1] < y1_min ? comp[1] : y1_min;

            z1_max = comp[2] > z1_max ? comp[2] : z1_max;
            z1_min = comp[2] < z1_min ? comp[2] : z1_min;

            x2_max = comp[3] > x2_max ? comp[3] : x2_max;
            x2_min = comp[3] < x2_min ? comp[3] : x2_min;

            y2_max = comp[4] > y2_max ? comp[4] : y2_max;
            y2_min = comp[4] < y2_min ? comp[4] : y2_min;

            z2_max = comp[5] > z2_max ? comp[5] : z2_max;
            z2_min = comp[5] < z2_min ? comp[5] : z2_min;
        }
        features[0] = x1_max - x1_min;
        features[1] = y1_max - y1_min;
        features[2] = z1_max - z1_min;
        features[3] = x2_max - x2_min;
        features[4] = y2_max - y2_min;
        features[5] = z2_max - z2_min;
        return features;
    }
}
