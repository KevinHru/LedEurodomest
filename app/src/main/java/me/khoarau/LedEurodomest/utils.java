package me.khoarau.LedEurodomest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;


public class utils {

    public final static int REQUEST_ENABLE_BT = 1;
    public final static int REQUEST_ENABLE_GPS = 2;

    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final int PERMISSION_REQUEST_FINE_LOCATION = 2;

    public static void toast(Activity activity, String text){
        activity.runOnUiThread(()->{
            Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();
        });
    }

    public static Boolean requestPermissions(Activity activity, BluetoothAdapter btAdapter){

        Boolean allPermissionsGranted = true;

        final LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if (btAdapter != null && !btAdapter.isEnabled()) {
            allPermissionsGranted = false;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
        else if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            allPermissionsGranted = false;
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("This app needs location");
            builder.setMessage("Please enable location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> {
                Intent enableIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivityForResult(enableIntent,REQUEST_ENABLE_GPS);
            });
            builder.show();
        }
        // Make sure we have access fine location enabled, if not, prompt the user to enable it
        else if(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            allPermissionsGranted = false;
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION));
            builder.show();
        }
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        else if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            allPermissionsGranted = false;
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(dialog -> activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION));
            builder.show();
        }

        return(allPermissionsGranted);
    }

    public static void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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


    public static BluetoothGatt write(Activity activity, BluetoothAdapter btAdapter, Runnable callback, String address, String hexvalue) {

        BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.v("ble", "STATE_CONNECTED: " + gatt.getDevice().getAddress());
                    gatt.discoverServices();
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.v("ble", "STATE_DISCONNECTED: " + gatt.getDevice().getAddress());
                    sleep(100);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                Log.v("ble", "onServicesDiscovered status: " + status);

                UUID sUuid = UUID.fromString("0000cc02-0000-1000-8000-00805f9b34fb");
                UUID cUuid = UUID.fromString("0000ee03-0000-1000-8000-00805f9b34fb");

                BluetoothGattService service = gatt.getService(sUuid);
                if(service!=null) {
                    BluetoothGattCharacteristic characteristic = gatt.getService(sUuid).getCharacteristic(cUuid);
                    characteristic.setValue(hexStringToByteArray(hexvalue));
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    gatt.writeCharacteristic(characteristic);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.v("ble", "onCharacteristicWrite: " + byteArrayToHexString(characteristic.getValue()));
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.close();
                    callback.run();
                }
            }
        };

        BluetoothGatt gatt = btAdapter.getRemoteDevice(address).connectGatt(activity, true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);

        return(gatt);
    }
}
