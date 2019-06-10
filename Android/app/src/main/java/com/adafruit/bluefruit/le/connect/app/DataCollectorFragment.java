package com.adafruit.bluefruit.le.connect.app;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.adafruit.bluefruit.le.connect.R;
import com.adafruit.bluefruit.le.connect.ble.BleUtils;
import com.adafruit.bluefruit.le.connect.ble.UartPacket;
import com.adafruit.bluefruit.le.connect.ble.UartPacketManagerBase;
import com.adafruit.bluefruit.le.connect.ble.central.BlePeripheral;
import com.adafruit.bluefruit.le.connect.ble.central.BlePeripheralUart;
import com.adafruit.bluefruit.le.connect.ble.central.BleScanner;
import com.adafruit.bluefruit.le.connect.ble.central.UartPacketManager;
import com.adafruit.bluefruit.le.connect.mqtt.MqttManager;
import com.adafruit.bluefruit.le.connect.mqtt.MqttSettings;
import com.adafruit.bluefruit.le.connect.style.UartStyle;
import com.adafruit.bluefruit.le.connect.utils.DialogUtils;
import com.adafruit.bluefruit.le.connect.utils.DuoSignalPoint;
import com.adafruit.bluefruit.le.connect.utils.KeyboardUtils;
import com.adafruit.bluefruit.le.connect.utils.SignalPoint;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataCollectorFragment extends ConnectedPeripheralFragment implements UartPacketManagerBase.Listener {
    // Log
    private final static String TAG = DataCollectorFragment.class.getSimpleName();

    // Constants
    private final static String kPreferences = "UartActivity_prefs";
    private final static String kPreferences_eol = "eol";
    private final static String kPreferences_eolCharactersId = "eolCharactersId";

    // UI
    private GraphView graphView;
    private LineGraphSeries<DataPoint> mSeriesX1;
    private LineGraphSeries<DataPoint> mSeriesY1;
    private LineGraphSeries<DataPoint> mSeriesZ1;

    private LineGraphSeries<DataPoint> mSeriesX2;
    private LineGraphSeries<DataPoint> mSeriesY2;
    private LineGraphSeries<DataPoint> mSeriesZ2;

    private Button graphButton;
    private EditText dataLabel;

    private int GRAPH_SIZE = 200;

    // Data
    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());
    protected UartPacketManagerBase mUartData;
    protected List<BlePeripheralUart> mBlePeripheralsUart = new ArrayList<>();
    private Map<String, Integer> mColorForPeripheral = new HashMap<>();

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
    private boolean collecting;

    // region Fragment Lifecycle
    public static DataCollectorFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        DataCollectorFragment fragment = new DataCollectorFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        return fragment;
    }

    public DataCollectorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes
        setRetainInstance(true);

        signals = new LinkedList<SignalPoint>();
        duosignals = new LinkedList<DuoSignalPoint>();
        signalX1 = new LinkedList<DataPoint>();
        signalY1 = new LinkedList<DataPoint>();
        signalZ1 = new LinkedList<DataPoint>();

        signalX2 = new LinkedList<DataPoint>();
        signalY2 = new LinkedList<DataPoint>();
        signalZ2 = new LinkedList<DataPoint>();

        packetParseBuffer = new StringBuffer();
        collecting = false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_datacollector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataLabel = (EditText) view.findViewById(R.id.labelEditText);

        // Connect data structures to graph view with proper styling
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

        // Setup button to collect data
        graphButton = (Button) view.findViewById(R.id.graphButton);
        graphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDataCollection();
            }
        });

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
                if(collecting) {
                    duosignals.add(nSignal);
                }
                List<DataPoint> graphPoints = nSignal.getDataPoints();
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
            }
            packetParseBuffer.delete(0, signalEnd + 1);
            parsePacketBufferForSignal();
            return;
        }
        //clear out piece of buffer with no start (this shouldn't happen
        if(signalEnd < signalStart) {
            packetParseBuffer.delete(0, signalStart);
        }
    }

    // Handle start and end of data collection
    private void setDataCollection() {
        // At start of data collection clear all data structures and update text
        if(!collecting) {
            collecting = true;
            graphButton.setText(R.string.datacollector_collect_end_action);
            signals.clear();
            duosignals.clear();
            signalX1.clear();
            signalY1.clear();
            signalZ1.clear();
            signalX2.clear();
            signalY2.clear();
            signalZ2.clear();
        }
        // ending data collection
        else {
            collecting = false;
            graphButton.setText(R.string.datacollector_collect_start_action);
            String dataName = dataLabel.getText().toString();

            try {
                File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "GestureData");
                if (!root.exists()) {
                    root.mkdirs();
                }
                File gpxfile = new File(root, dataName + ".txt");
                FileWriter writer = new FileWriter(gpxfile);
                // check if single accelerometer signal received
                if(signals.size() > 0) {
                    for (SignalPoint signal : signals) {
                        writer.append(signal.toString() + "\n");
                    }
                } else {
                    for (DuoSignalPoint signal : duosignals) {
                        writer.append(signal.toString() + "\n");
                    }
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            signals.clear();
        }
    }
}
