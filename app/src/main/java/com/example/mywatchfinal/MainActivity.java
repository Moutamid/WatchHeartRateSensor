package com.example.mywatchfinal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.example.mywatchfinal.databinding.ActivityMainBinding;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

//Moutamid's Galaxy A51: A8:30:BC:AF:6B:70
public class MainActivity extends Activity implements MySensor.OnSensorChangeListener, AdapterView.OnItemClickListener {
    private static final int REQUEST_SENSOR_PERMISSION = 1;
    private MySensor mySensor;
    private TextView bpmReadingTextview;
//    private TextView topTitleTv;
//    private TextView stateSensorTv;
//    private TextView stateBluetoothTv;

    private ActivityMainBinding binding;

    //    private BluetoothManager mBluetoothManager;
//    private BluetoothAdapter mBluetoothAdapter;
//    private BluetoothSocket mBluetoothSocket;
//    private BluetoothServerSocket mBluetoothServerSocket;
//    BluetoothDevice mDevice;
//    private OutputStream mOutputStream;
//    private InputStream mInputStream;
//    private UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//    String mDeviceAddress = "64:A2:F9:66:72:CB";
//    ConnectedThread Streams;
//    Thread mBluetoothThread;
    String mHeartRateStr;
    byte[] mHeartRateBytes = new byte[1024];

    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;

    BluetoothConnectionService mBluetoothConnection;

    Button btnStartConnection, discoverDevices;
//    Button btnSend;

//    EditText etSend;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    BluetoothDevice mBTDevice;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    public DeviceListAdapter mDeviceListAdapter;

    ListView lvNewDevices;


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };


    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    @SuppressLint("MissingPermission")
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
//                Toast.makeText(MainActivity.this, "" + device.getName() + ": " + device.getAddress(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    @SuppressLint("MissingPermission")
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        //mBluetoothAdapter.cancelDiscovery();
    }

//    TextView incomingTextView;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bpmReadingTextview = binding.bpmReadingTextview;
//        topTitleTv = (TextView) findViewById(R.id.topTitleTv);
//        stateSensorTv = (TextView) findViewById(R.id.stateSensorTv);
//        stateBluetoothTv = (TextView) findViewById(R.id.stateBluetoothTv);

        mySensor = new MySensor(this, this);
//        stateBluetoothTv.setText("INIT BLUETOOTH");
//        mInitializeBluetooth();
//        stateBluetoothTv.setText("INIT BLUETOOTH OK");
//        Streams = new ConnectedThread(mBluetoothSocket);
//        mInputStream = Streams.mmInStream;
//        mOutputStream = Streams.mmOutStream;
//        if(mInputStream !=null && mOutputStream!=null){
//            stateBluetoothTv.setText("INIT STREAMS BT OK");
//        }
//        else {
//            stateBluetoothTv.setText("ERROR STREAMS BT");
//        }


        btnEnableDisable_Discoverable = (Button) findViewById(R.id.enableDiscoverable);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        btnStartConnection = (Button) findViewById(R.id.startConnection);
        discoverDevices = (Button) findViewById(R.id.discoverDevices);

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        lvNewDevices.setOnItemClickListener(MainActivity.this);

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                btnStartConnection.setVisibility(View.GONE);
                startConnection();
            }
        });

        // ENABLING BLUETOOTH AUTOMATICALLY IF DISABLED
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    boolean isConnectionStarted = false;

    //create method for starting connection
//***remember the conncction will fail and app will crash if you haven't paired first
    public void startConnection() {
        isConnectionStarted = true;
        startBTConnection(mBTDevice, MY_UUID_INSECURE);

        /*new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                String str = "" + value++;
                mBluetoothConnection.write(str.getBytes(Charset.defaultCharset()));

            }
        }, 1000, 1000);*/
    }

    int value = 0;

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
    }


    @SuppressLint("MissingPermission")
    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    @SuppressLint("MissingPermission")
    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        btnEnableDisable_Discoverable.setVisibility(View.GONE);
        discoverDevices.setVisibility(View.VISIBLE);
//        Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);

    }

    @SuppressLint("MissingPermission")
    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        // IT IS CANCELLING THE DISCOVERY
        /*if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }*/
        if (!mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }

//        Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();

        discoverDevices.setVisibility(View.GONE);
        lvNewDevices.setVisibility(View.VISIBLE);

    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
//        Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();

        lvNewDevices.setVisibility(View.GONE);
        btnStartConnection.setVisibility(View.VISIBLE);

    }

    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            mySensor.start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, REQUEST_SENSOR_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySensor.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SENSOR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mySensor.start();
            }
        }
    }

    @Override
    public void onSensorStarted() {
//        stateSensorTv.setText("OK SENSOR");
    }

    @Override
    public void onSensorStopped() {
        // Do nothing
    }

    @Override
    public void onSensorNotAvailable() {
//        stateSensorTv.setText("PAS DE SENSOR");
    }

    @Override
    public void onSensorChanged(int mHeartRate) {
        bpmReadingTextview.setText(String.valueOf(mHeartRate));
        mHeartRateStr = String.valueOf(mHeartRate);
        mHeartRateBytes = mHeartRateStr.getBytes(Charset.defaultCharset());

        if (isConnectionStarted) {
            mBluetoothConnection.write(mHeartRateBytes);
//            bpmReadingTextview.setText(String.valueOf(mHeartRate) + " sent");
        }

        //Streams.write(mHeartRateBytes);
    }

    /*private static final int REQUEST_ENABLE_BT = 2;
    @SuppressLint("MissingPermission")
    private void mInitializeBluetooth() {

        mBluetoothManager = getSystemService(BluetoothManager.class);
        // Initialiser la classe BluetoothAdapter
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // Connecter à un périphérique Bluetooth
        mDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);

        //autoriser le bluetooth par l'utilisateur
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        BluetoothServerSocket tmp1 = null;
        try {
            tmp1 = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("SE", SPP_UUID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mBluetoothServerSocket = tmp1;

        BluetoothSocket tmp;
        try {
            tmp = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mBluetoothSocket = tmp;
    }
    private void mConnectToBluetooth() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mBluetoothSocket = mBluetoothServerSocket.accept();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
    */
}