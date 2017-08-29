package tw.com.fulldot.bluetoothlowenergy;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.LocaleDisplayNames;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_INVALID_OFFSET;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class MainActivity extends AppCompatActivity implements IMain {

    private String TAG = "Message";

    private boolean OpenBT = false;
    private boolean Discoverable_BT = false;

    // this REQUEST_ENABLE_BT must greater than 0
    private static final int REQUEST_ENABLE_BT = 1;

    // CheckPermissionCode
    private int CheckPermissionCode = 0;

    // REQUEST_DISCOVERABLE
    private static final int REQUEST_DISCOVERABLE_BT = 2;
    private int EXTRA_DISCOVERABLE_DURATION_TIME = 300;

    // _BluetoothAdapter
    private BluetoothAdapter _BluetoothAdapter;
    private ScanCallback _ScanCallback;
    private BluetoothLeScanner _BluetoothLeScanner;
    private BluetoothGattCallback _BluetoothGattCallback ;


    // ViewHolder_Data
    private List<ScanResult> BluetoothScanResultList = new ArrayList<>();

    // GattService / GattCharacteristic
    private BluetoothGatt _BluetoothGatt;
    private List<BluetoothGattService> _BluetoothGattServiceList;
    private List<BluetoothGattCharacteristic> _GattCharacteristicList;
    private List<BluetoothGattDescriptor> _BluetoothGattDescriptor;
    private BluetoothGattCharacteristic Read_Write_Characteristic = null;

    // RecyclerView / Adapter
    private RecyclerView _RecyclerView;
    private RecyclerView.Adapter BLE_Adapter;

    private TextView SupportBLE_Adv;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////  Check Bluetooth is Supported on the Device  /////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////

        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////  Ask Permission  /////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////

        List<String> request_permisson_lsit = new ArrayList<>();

        int ACCESS_COARSE_LOCATION_Permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int BLUETOOTH_Permission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        int BLUETOOTH_ADMIN_Permission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);

        if (ACCESS_COARSE_LOCATION_Permission != PackageManager.PERMISSION_GRANTED) {
            request_permisson_lsit.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (BLUETOOTH_Permission != PackageManager.PERMISSION_GRANTED) {
            request_permisson_lsit.add(Manifest.permission.BLUETOOTH);
        }
        if (BLUETOOTH_ADMIN_Permission != PackageManager.PERMISSION_GRANTED) {
            request_permisson_lsit.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (request_permisson_lsit.size() > 0) {
            ActivityCompat.requestPermissions
                    (this, request_permisson_lsit.toArray(new String[request_permisson_lsit.size()]), CheckPermissionCode);
        } else { /* 權限都已經授權了 */ }

        // 要求使用者允許此裝置藍芽可以被搜尋
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, EXTRA_DISCOVERABLE_DURATION_TIME);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);


        ////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////  Setting Up BLE  /////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        _BluetoothAdapter = bluetoothManager.getAdapter();
//        _BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Ensures Bluetooth is available on the device and it is enabled.
        // If not, displays a dialog requesting user permission to enable Bluetooth.
        if (_BluetoothAdapter == null || !_BluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // The REQUEST_ENABLE_BT constant passed to startActivityForResult(android.content.Intent, int)
        // REQUEST_ENABLE_BT is a locally-defined integer(which must be greater than 0)
        // that the system passes back to you in your onActivityResult(int, int, android.content.Intent) implementation as the requestCode parameter


        ////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////  Finding BLE Devices  ////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////


        _BluetoothGattCallback =  new BluetoothGattCallback()
        {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.d(TAG,"onConnectionStateChange");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"onConnectionStateChange",Toast.LENGTH_SHORT).show();
                    }
                });

                if(status == GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED)
                {
                    Log.d(TAG,"連線到BLE成功");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"連線到BLE成功",Toast.LENGTH_SHORT).show();
                        }
                    });

                    // 連線BLE設備後，停止藍芽繼續搜尋
                    //_BluetoothLeScanner.stopScan(_ScanCallback);
                    //Log.d(TAG,"停止藍芽繼續搜尋");
                    //Toast.makeText(MainActivity.this,"停止藍芽繼續搜尋",Toast.LENGTH_SHORT).show();

                    // 連線BLE設備後，取得所有Service
                    _BluetoothGatt.discoverServices();
                    Log.d(TAG,"取得所有Service");
                    //Toast.makeText(MainActivity.this,"取得所有Service",Toast.LENGTH_SHORT).show();
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log.d(TAG,"連線到BLE失敗");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"連線到BLE失敗",Toast.LENGTH_SHORT).show();
                        }
                    });

                    // 中斷連線
                    _BluetoothGatt.disconnect();
                    _BluetoothGatt.close();
                    _BluetoothGatt = null;
                }
                else if(status == 133)
                {
                    Log.d(TAG,"連線status = 133");
                    //Toast.makeText(MainActivity.this,"連線status = 133",Toast.LENGTH_SHORT).show();

                    // 中斷連線
                    _BluetoothGatt.disconnect();
                    _BluetoothGatt.close();
                    _BluetoothGatt = null;
                }

                //super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.d(TAG,"Write_Descriptor : "+ new String(descriptor.getValue(),0,descriptor.getValue().length));
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                Log.d(TAG,"onServicesDiscovered");
                //Toast.makeText(MainActivity.this,"onServicesDiscovered",Toast.LENGTH_SHORT).show();
                // 取得 Server 中所有的 Services
                _BluetoothGattServiceList = _BluetoothGatt.getServices();
                for(BluetoothGattService service : _BluetoothGattServiceList)
                {
                    Log.d(TAG,"迴圈尋找 BluetoothGattService");
                    //Toast.makeText(MainActivity.this,"迴圈尋找 BluetoothGattService",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"BluetoothGattService_UUID : "+service.getUuid().toString());
                    // 找出指定的 Service
                    if(service.getUuid().compareTo(UUID.fromString(BLEDefine.Service_UUID)) == 0)
                    //if(service.getUuid().toString().equals(BLEDefine.Service_UUID))
                    {
                        Log.d(TAG,"找到指定的 BluetoothGattService");
                        //Toast.makeText(MainActivity.this,"找到指定的 BluetoothGattService",Toast.LENGTH_SHORT).show();
                        // 取得 Services 中所有的 GattCharacteristic
                        _GattCharacteristicList = service.getCharacteristics();
                        for(BluetoothGattCharacteristic gattCharacteristic : _GattCharacteristicList)
                        {
                            Log.d(TAG,"迴圈尋找 BluetoothGattCharacteristic");
                            //Toast.makeText(MainActivity.this,"迴圈尋找 BluetoothGattCharacteristic",Toast.LENGTH_SHORT).show();
                            Log.d(TAG,"BluetoothGattCharacteristic_UUID : "+gattCharacteristic.getUuid().toString());
                            // 找出指定的 GattCharacteristic
                            // BLEDefine.Characteristics_UUID 這個 GattCharacteristic 的 Properties 為 Read / Write / Notify / Indicate
                            if(gattCharacteristic.getUuid().compareTo(UUID.fromString(BLEDefine.Characteristics_UUID)) == 0)
                            //if(gattCharacteristic.getUuid().toString().equals(BLEDefine.Characteristics_UUID))
                            {
                                Log.d(TAG,"找到指定的 gattCharacteristic");
                                //Toast.makeText(MainActivity.this,"找到指定的 gattCharacteristic",Toast.LENGTH_SHORT).show();
                                Read_Write_Characteristic = gattCharacteristic;

                                _BluetoothGatt.setCharacteristicNotification(Read_Write_Characteristic,true);

//                                client characteristic configuration descriptor 0x2902 (應該是藍牙技術聯盟定義的公用碼)
//                                // 0x2902 org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
//                                UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//                                BluetoothGattDescriptor descriptor = Read_Write_Characteristic.getDescriptor(uuid);
//                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                _BluetoothGatt.writeDescriptor(descriptor);


                                _BluetoothGattDescriptor = Read_Write_Characteristic.getDescriptors();
                                for(BluetoothGattDescriptor descriptor:_BluetoothGattDescriptor)
                                {
                                    Log.d(TAG,"迴圈尋找 BluetoothGattDescriptor");
                                    Log.d(TAG,"BluetoothGattDescriptor_UUID : "+descriptor.getUuid().toString());
                                    if(descriptor.getUuid().compareTo(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) == 0)
                                    {
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        Log.d(TAG,"descriptor.setValue()");
                                        _BluetoothGatt.writeDescriptor(descriptor);
                                        Log.d(TAG,"BluetoothGatt.writeDescriptor");
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                }

//                if(Read_Write_Characteristic != null)
//                {
//                    Log.d(TAG,"呼叫 BluetoothGatt 的 readCharacteristic() method, 這時才會真的去 remote device 讀資料");
//                    _BluetoothGatt.readCharacteristic(Read_Write_Characteristic);
//                }
                //super.onServicesDiscovered(gatt, status);
            }



            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG,"系統透過指定的 GattCharacteristic 發出　Notification");
                Log.d(TAG,"收取 Notification 成功");
                final byte[] mBuffer = characteristic.getValue();
                Log.d(TAG,"收到 Notification 的訊息為 : "+ new String(mBuffer));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"收到 Notification 的訊息為 : "+ new String(mBuffer),Toast.LENGTH_SHORT ).show();
                    }
                });

                //Toast.makeText(MainActivity.this,"找到指定的GattCharacteristic ， 系統發出通知",Toast.LENGTH_SHORT).show();
                //super.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG,"onCharacteristicRead( ) 被呼叫到, 表示 read remote device 結束");

                if(status == GATT_SUCCESS) {
                    Log.d(TAG,"讀取資料成功");
                    final byte[] mBuffer = characteristic.getValue();
                    Log.d(TAG,"讀取的資料為 : "+ new String(mBuffer));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"讀取的資料為 : "+ new String(mBuffer),Toast.LENGTH_SHORT).show();
                            _BluetoothGatt.disconnect();
                            _BluetoothGatt.close();
                        }
                    });

                }
                else {
                    Log.d(TAG,"讀取資料失敗");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"讀取資料失敗",Toast.LENGTH_SHORT).show();
                        }
                    });
                    _BluetoothGatt.disconnect();
                    _BluetoothGatt.close();
                }
                //super.onCharacteristicRead(gatt, characteristic, status);
            }
        };

        _ScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.d(TAG,"onScanResult");
                BluetoothDevice device = result.getDevice();
                if (device.getName() == null || device.getName().length()== 0)
                    return;

                boolean repeat = false;
                Log.d(TAG,device.getName());
                for(int i=0;i<BluetoothScanResultList.size();i++)
                {
                    Log.d(TAG,BluetoothScanResultList.get(i).getDevice().getName());
                    if(BluetoothScanResultList.get(i).getDevice().getName().equals(device.getName()))
                    {
                        repeat = true;
                        Log.d(TAG,String.valueOf(repeat));
                        break;
                    }
                }
                if (!repeat)
                {
                    BluetoothScanResultList.add(result);
                    Log.d(TAG,String.valueOf(BluetoothScanResultList.size()));
                    BLE_Adapter.notifyDataSetChanged();
                }
                super.onScanResult(callbackType, result);
            }
        };


        init();
    }

    private void init()
    {
        _RecyclerView = (RecyclerView)findViewById(R.id.myRecyclerView);
        BLE_Adapter = new BLE_RecycleViewAdapter(this,BluetoothScanResultList,_BluetoothAdapter,_BluetoothGattCallback);
        _RecyclerView.setAdapter(BLE_Adapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        _RecyclerView.setLayoutManager(linearLayoutManager);


        Button StartScan = (Button)findViewById(R.id.ScanButton);
        StartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothScanResultList.clear();

                if(_BluetoothLeScanner == null)
                _BluetoothLeScanner = _BluetoothAdapter.getBluetoothLeScanner();

                _BluetoothLeScanner.startScan(_ScanCallback);
            }
        });

        Button StopScan = (Button)findViewById(R.id.StopButton);
        StopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(_BluetoothGatt != null)
                {
                    _BluetoothGatt.disconnect();
                    _BluetoothGatt.close();
                    _BluetoothGatt = null;

                    Read_Write_Characteristic = null;
                }
                if(_BluetoothLeScanner != null)
                {
                    _BluetoothLeScanner.stopScan(_ScanCallback);
                    _BluetoothLeScanner = null;
                }
            }
        });

        SupportBLE_Adv = (TextView)findViewById(R.id.SupportBLE_Adv);
        if(_BluetoothAdapter.isMultipleAdvertisementSupported()) {
            SupportBLE_Adv.setText("Support_BLE_Advertisement");
        }
        else {
            SupportBLE_Adv.setText("NotSupport_BLE_Advertisement");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // enabling Bluetooth succeeds
                    Log.d(TAG, "同意開啟藍芽");
                    OpenBT = true;
                    Toast.makeText(MainActivity.this, "同意開啟藍芽", Toast.LENGTH_SHORT).show();
                    break;
                } else if (resultCode == RESULT_CANCELED) {
                    // Bluetooth was not enabled due to an error (or the user responded "No")
                    Log.d(TAG, "不同意開啟藍芽");
                    OpenBT = false;
                    Toast.makeText(MainActivity.this, "不同意開啟藍芽", Toast.LENGTH_SHORT).show();
                    break;
                }

            case REQUEST_DISCOVERABLE_BT:
                // If the user responds "Yes," then the device becomes discoverable for the specified amount of time.
                // Your activity then receives a call to the onActivityResult() callback,
                // with the result code equal to the duration that the device is discoverable
                if (resultCode == EXTRA_DISCOVERABLE_DURATION_TIME) {
                    // enabling Bluetooth Discoverable Succeeds
                    Log.d(TAG, "同意藍芽被搜尋");
                    Discoverable_BT = true;
                    Toast.makeText(MainActivity.this, "同意藍芽被搜尋", Toast.LENGTH_SHORT).show();
                    break;
                }
                // If the user responded "No," or if an error occurred, the result code will be RESULT_CANCELED.
                else if (resultCode == RESULT_CANCELED) {
                    // Bluetooth_Discoverable was not enabled due to an error (or the user responded "No")
                    Log.d(TAG, "不同意藍芽被搜尋");
                    Discoverable_BT = false;
                    Toast.makeText(MainActivity.this, "不同意藍芽被搜尋", Toast.LENGTH_SHORT).show();
                    break;
                }
        }
    }

    @Override
    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        _BluetoothGatt = bluetoothGatt;
    }

    @Override
    public void stopScan() {
        _BluetoothLeScanner.stopScan(_ScanCallback);
    }
}
