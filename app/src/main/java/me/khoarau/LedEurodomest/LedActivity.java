package me.khoarau.LedEurodomest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class LedActivity extends AppCompatActivity implements DialogInterface.OnClickListener {


    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothGatt gatt = null;
    int brightness = -1;
    String name;
    String address;
    String rename=null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        address = getIntent().getStringExtra("address");
        name = getIntent().getStringExtra("name");

        setContentView(R.layout.activity_main);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.removeAllViews();

        View.inflate(this,R.layout.led,linearLayout);
        ConstraintLayout row = (ConstraintLayout) linearLayout.getChildAt(linearLayout.getChildCount()-1);

        ((TextView) row.findViewById(R.id.led_name1)).setText("Turn on");
        ((ImageView) row.findViewById(R.id.led_img1)).setImageResource(R.drawable.led_on);
        row.findViewById(R.id.led1).setOnClickListener(view -> turnOn());

        ((TextView) row.findViewById(R.id.led_name2)).setText("Turn off");
        ((ImageView) row.findViewById(R.id.led_img2)).setImageResource(R.drawable.led_off);
        row.findViewById(R.id.led2).setOnClickListener(view -> turnOff());

        View.inflate(this,R.layout.led,linearLayout);
        row = (ConstraintLayout) linearLayout.getChildAt(linearLayout.getChildCount()-1);

        ((TextView) row.findViewById(R.id.led_name1)).setText("Brightness");
        ((ImageView) row.findViewById(R.id.led_img1)).setImageResource(R.drawable.brightness);
        row.findViewById(R.id.led1).setOnClickListener(view -> setBrightness());

        ((TextView) row.findViewById(R.id.led_name2)).setText("Reading mode");
        ((ImageView) row.findViewById(R.id.led_img2)).setImageResource(R.drawable.reading);
        row.findViewById(R.id.led2).setOnClickListener(view -> setBrightnessReading());

        View.inflate(this,R.layout.led,linearLayout);
        row = (ConstraintLayout) linearLayout.getChildAt(linearLayout.getChildCount()-1);

        ((TextView) row.findViewById(R.id.led_name1)).setText("Color");
        ((ImageView) row.findViewById(R.id.led_img1)).setImageResource(R.drawable.color);
        row.findViewById(R.id.led1).setOnClickListener(view -> setColor());

        ((TextView) row.findViewById(R.id.led_name2)).setText("Color auto");
        ((ImageView) row.findViewById(R.id.led_img2)).setImageResource(R.drawable.color_auto);
        row.findViewById(R.id.led2).setOnClickListener(view -> setAuto());

        View.inflate(this,R.layout.led,linearLayout);
        row = (ConstraintLayout) linearLayout.getChildAt(linearLayout.getChildCount()-1);

        ((TextView) row.findViewById(R.id.led_name1)).setText("Rename");
        ((ImageView) row.findViewById(R.id.led_img1)).setImageResource(R.drawable.rename);
        row.findViewById(R.id.led1).setOnClickListener(view -> setName());

        ((TextView) row.findViewById(R.id.led_name2)).setText("Info");
        ((ImageView) row.findViewById(R.id.led_img2)).setImageResource(R.drawable.info);
        row.findViewById(R.id.led2).setOnClickListener(view -> showInfo());

    }

    public void showInfo(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Info");
        builder.setMessage("Device address: " + address);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    public void turnOn(){
        writeDialog("000000000000000001ff");
    }

    public void turnOff(){
        writeDialog("00000000000000000000");
    }

    public void setBrightnessReading(){
        String b = String.format("%02x", 20*255/100);
        writeDialog("000000000000000001"+b);
    }

    public void setAuto(){
        writeDialog("55");
    }


    public void setColor() {
        new ColorPickerDialog.Builder(this)
            .setTitle("ColorPicker Dialog")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton("Ok",
                    new ColorEnvelopeListener() {
                        @Override
                        public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                            Log.v("color", "A:"+envelope.getArgb()[0]);
                            Log.v("color", "R:"+envelope.getArgb()[1]);
                            Log.v("color", "G:"+envelope.getArgb()[2]);
                            Log.v("color", "B:"+envelope.getArgb()[3]);

                            String r = String.format("%02x", envelope.getArgb()[1]);
                            String g = String.format("%02x", envelope.getArgb()[2]);
                            String b = String.format("%02x", envelope.getArgb()[3]);

                            writeDialog("01"+g+"0000"+"01"+b+"01"+r+"0000");

                        }
                    })
            .setNegativeButton("Cancel",null)
            .attachAlphaSlideBar(false) // the default value is true.
            .attachBrightnessSlideBar(true)  // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
            .show();
    }

    public void setName() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename");
        builder.setPositiveButton("Ok", this);
        builder.setNegativeButton("Cancel", null);
        builder.setView(R.layout.name);
        AlertDialog dialog = builder.show();

        EditText editText = (EditText) dialog.findViewById(R.id.editText);
        editText.setText(name);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                rename = editable.toString();
            }
        });

    }

    public void setBrightness(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select brightness");
        builder.setPositiveButton("Ok", this);
        builder.setNegativeButton("Cancel", null);
        builder.setView(R.layout.brightness);
        AlertDialog dialog = builder.show();

        TextView textView = (TextView) dialog.findViewById(R.id.brightness);
        SeekBar seekbar = (SeekBar) dialog.findViewById(R.id.seekBarBrightness);

        seekbar.setMax(100);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textView.setText(i+ " %");
                brightness = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekbar.setProgress(50);

        dialog.findViewById(R.id.button20).setOnClickListener(view -> seekbar.setProgress(20));
        dialog.findViewById(R.id.button40).setOnClickListener(view -> seekbar.setProgress(40));
        dialog.findViewById(R.id.button60).setOnClickListener(view -> seekbar.setProgress(60));
        dialog.findViewById(R.id.button80).setOnClickListener(view -> seekbar.setProgress(80));

    }

    public void writeDialog(String hexvalue) {

        if(gatt!=null) {
            gatt.close();
            utils.sleep(100);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sending command...");
        builder.setPositiveButton("Cancel", this);
        AlertDialog dialog = builder.show();

        Runnable callback = () -> dialog.dismiss();

        gatt = utils.write(this, btAdapter, callback, address, hexvalue);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {

        if(gatt!=null) {
            gatt.close();
        }

        if(brightness!=-1){
            String b = String.format("%02x", brightness*255/100);
            writeDialog("000000000000000001"+b);
            brightness=-1;
        }

        if(rename!=null){
            name = rename;
            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_device_name), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(address, rename);
            editor.apply();
            rename = null;
        }



    }
}