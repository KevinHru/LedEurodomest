package me.khoarau.LedEurodomest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class Device{
    String name;
    String address;

    public Device(String name, String address){
        this.name = name;
        this.address = address;
    }
}

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    Map<String,Device> devices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        // Test
        Boolean test = false;
        if(test){
            devices.put("251",new Device("Led1","251"));
            devices.put("252",new Device("Led2","252"));
            devices.put("253",new Device("Led3","253"));
            devices.put("254",new Device("Led4","254"));
            devices.put("255",new Device("Led5","255"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        utils.sleep(1000);
        startScanning();
    }

    protected void showDevices(){

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_device_name), Context.MODE_PRIVATE);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.removeAllViews();

        Object[] d_values = devices.values().toArray();

        for (int i=0;i<d_values.length;i++){

            Device device = (Device) d_values[i];
            String name = sharedPref.getString(device.address,device.name);

            if(i%2==0){
                View.inflate(this,R.layout.led,linearLayout);

                ConstraintLayout row = (ConstraintLayout) linearLayout.getChildAt(linearLayout.getChildCount()-1);
                row.findViewById(R.id.led2).setVisibility(View.INVISIBLE);

                ((TextView) row.findViewById(R.id.led_name1)).setText(name);
                row.findViewById(R.id.led1).setOnClickListener(view -> selectLed(device));

            }

            else{
                ConstraintLayout row = (ConstraintLayout) linearLayout.getChildAt(linearLayout.getChildCount()-1);
                row.findViewById(R.id.led2).setVisibility(View.VISIBLE);

                ((TextView) row.findViewById(R.id.led_name2)).setText(name);
                row.findViewById(R.id.led2).setOnClickListener(view -> selectLed(device));
            }
        }
    }

    public void selectLed(Device device){

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_device_name), Context.MODE_PRIVATE);
        String name = sharedPref.getString(device.address,device.name);

        Intent intent = new Intent().setClass(this, LedActivity.class);
        intent.putExtra("address",device.address);
        intent.putExtra("name", name);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //devices = new HashMap<>();
        showDevices();
        if(utils.requestPermissions(this, btAdapter))
            startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }


    public void startScanning() {
        Log.v("ble", "startScanning");

        List<ScanFilter> filters = new ArrayList<>();

        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000cc02-0000-1000-8000-00805f9b34fb")).build());

        ScanSettings settings = new ScanSettings.Builder().build();

        AsyncTask.execute(() -> {
            btScanner.startScan(filters, settings, leScanCallback);
        });
    }

    public void stopScanning() {
        if(btAdapter.isEnabled()) {
            Log.v("ble","stopScanning");
            AsyncTask.execute(() -> btScanner.stopScan(leScanCallback));
        }
    }

    // Device scan callback.
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            String name = result.getDevice().getName();
            String address = result.getDevice().getAddress();

            if (!devices.containsKey(result.getDevice().getAddress())) {
                devices.put(address, new Device(name, address));
                showDevices();
            }
        }
    };

    /*
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length*2];
        int v;

        for(int j=0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v>>>4];
            hexChars[j*2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public void write(String address, String serviceUUID, String characteristicUUID, String hexvalue){
        write(address, serviceUUID, characteristicUUID, hexvalue, 30);
    }

    public void toast(String text){
        runOnUiThread(()->{
            Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
        });
    }

    public void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void write(String address, String serviceUUID, String characteristicUUID, String hexvalue, int count) {

        AsyncTask.execute(() -> {
            stopScanning();
            sleep(100);

            devices.get(address).write_done = false;

            BluetoothGattCallback callback = new BluetoothGattCallback() {


                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if(gatt!=null) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.v("ble", "STATE_CONNECTED: " + gatt.getDevice().getAddress());
                            gatt.discoverServices();
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.v("ble", "STATE_DISCONNECTED: " + gatt.getDevice().getAddress());
                            //gatt.close();

                            if (!devices.get(address).write_done) {

                                if(count==10){
                                    btAdapter.disable();
                                    sleep(100);
                                    waitBluetoothEnabled();
                                }
                                sleep(200);

                                if (count > 0) {
                                    //sleep(100);
                                    Log.v("ble", "retry: " + count);

                                    write(address, serviceUUID, characteristicUUID, hexvalue, count - 1);
                                } else {
                                    toast("Failed to connect with the device");
                                    startScanning();
                                }
                            }

                        }
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);

                    Log.v("ble", "onServicesDiscovered status: " + status);

                    UUID sUuid = UUID.fromString(serviceUUID);
                    UUID cUuid = UUID.fromString(characteristicUUID);

                    BluetoothGattService service = gatt.getService(sUuid);
                    if(service!=null) {
                        BluetoothGattCharacteristic characteristic = gatt.getService(sUuid).getCharacteristic(cUuid);
                        characteristic.setValue(hexStringToByteArray(hexvalue));
                        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        gatt.writeCharacteristic(characteristic);


                    }
                    else{
                        gatt.disconnect();
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    Log.v("ble", "onCharacteristicWrite: " + byteArrayToHexString(characteristic.getValue()));
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        devices.get(address).write_done = true;
                        //gatt.disconnect();
                        sleep(100);
                        gatt.close();
                        sleep(100);
                        devices.get(address).gatt = null;
                        startScanning();

                        toast("Command received.");
                    }
                }
            };


            if (devices.get(address).gatt != null) {
                devices.get(address).gatt.close();
            }

            devices.get(address).gatt = btAdapter.getRemoteDevice(address).connectGatt(getApplicationContext(), false, callback, BluetoothDevice.TRANSPORT_LE);

        });
    }


    public void toogleDevice(String address) {

        String serviceUUID = "0000cc02-0000-1000-8000-00805f9b34fb";
        String characteristicUUID = "0000ee03-0000-1000-8000-00805f9b34fb";

        Device d = devices.get(address);

        if(d.IsOn){
            write(d.Address, serviceUUID, characteristicUUID, "00000000000000000000");
            d.IsOn = false;
        }
        else{
            write(d.Address, serviceUUID, characteristicUUID, "000000000000000001ff");
            d.IsOn = true;
        }

        toast("Command sent.");

        showDevices();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //devices = new HashMap<>();
        showDevices();
        startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    private void waitBluetoothEnabled(){
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
            while(!btAdapter.isEnabled()){
                sleep(200);
            }
        }
    }



    // Device scan callback.
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (!devices.containsKey(result.getDevice().getAddress())) {
                devices.put(result.getDevice().getAddress(), new Device(result.getDevice().getName(), result.getDevice().getAddress(), false, result.getDevice()));
                showDevices();

                Log.v("ble", "getManufacturerSpecificData: " +result.getDevice().getName() + result.toString());
            }

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app may not work properly.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {});
                    builder.show();
                }
            }
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app may not work properly.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {});
                    builder.show();
                }
            }
        }
    }



    public void startScanning() {
        if(btAdapter.isEnabled()) {
            Log.v("ble", "startScanning");

            List<ScanFilter> filters = new ArrayList<ScanFilter>();

            filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000cc02-0000-1000-8000-00805f9b34fb")).build());

            ScanSettings settings = new ScanSettings.Builder().build();

            AsyncTask.execute(() -> {
                waitBluetoothEnabled();
                btScanner.startScan(filters, settings, leScanCallback);
            });
        }
    }

    public void stopScanning() {
        if(btAdapter.isEnabled()) {
            Log.v("ble","stopScanning");
            AsyncTask.execute(() -> btScanner.stopScan(leScanCallback));
        }
    }
    */


}
