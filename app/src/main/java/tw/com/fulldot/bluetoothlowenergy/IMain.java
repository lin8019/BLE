package tw.com.fulldot.bluetoothlowenergy;


import android.bluetooth.BluetoothGatt;

public interface IMain
{
    public void setBluetoothGatt(BluetoothGatt bluetoothGatt);
    public void stopScan();
}
