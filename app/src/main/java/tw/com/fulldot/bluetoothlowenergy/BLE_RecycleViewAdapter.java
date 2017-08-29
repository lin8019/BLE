package tw.com.fulldot.bluetoothlowenergy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class BLE_RecycleViewAdapter extends RecyclerView.Adapter<BLE_ViewHolder>{
    private String TAG = "Message";
    private Context _context;
    private IMain _iMain;
    private static List<ScanResult> _scanResultList ;
    private BluetoothAdapter _bluetoothAdapter;
    private static BluetoothGattCallback _bluetoothGattCallback;

    public BLE_RecycleViewAdapter(IMain iMain,List<ScanResult> scanResults, BluetoothAdapter bluetoothAdapter,BluetoothGattCallback bluetoothGattCallback)
    {
        _iMain = iMain;
        _scanResultList = scanResults;
        _bluetoothAdapter = bluetoothAdapter;
        _bluetoothGattCallback = bluetoothGattCallback;
    }

    @Override
    public BLE_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG,"onCreateViewHolder");

        _context = parent.getContext();
        View row_view = LayoutInflater.from(_context).inflate(R.layout.row_scan_result,parent,false);
        BLE_ViewHolder myViewHolder = new BLE_ViewHolder(row_view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(BLE_ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder");
        holder.textView.setText(_scanResultList.get(position).getDevice().getName()+"/"
        +_scanResultList.get(position).getDevice().getAddress()+"/"
        +System.currentTimeMillis()/(1000*3600*24*365)+"/"
        +_scanResultList.get(position).getRssi());

        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 準備連線指定的裝置前,先停止搜尋裝置
                Log.d(TAG,"停止藍芽繼續搜尋");
                _iMain.stopScan();

                Log.d(TAG,"點擊位置 : "+String.valueOf(position));
                Log.d(TAG,"Name : "+_scanResultList.get(position).getDevice().getName());
                Log.d(TAG,"Address : "+_scanResultList.get(position).getDevice().getAddress());

                BluetoothGatt bluetoothGatt = _scanResultList.get(position).getDevice().connectGatt(_context,true,_bluetoothGattCallback);
                Log.d(TAG,"BluetoothGatt is null : "+String.valueOf(null == bluetoothGatt));
                if(bluetoothGatt != null) {
                    _iMain.setBluetoothGatt(bluetoothGatt);
                    Log.d(TAG,"setBluetoothGatt_finish");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG,"getItemCount");
        return _scanResultList.size();
    }

    public void changeData(List<ScanResult> scanResultList)
    {
        _scanResultList.clear();
        _scanResultList.addAll(scanResultList);
        notifyDataSetChanged();
    }
}
