package com.example.valdymas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket = null;
    private ConnectedThread mConnectedThread;
    Handler bluetoothIn;
    final int handlerState = 0;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address;
    private StringBuilder recDataString = new StringBuilder();
    private String sensor;

    private String path = Environment.getExternalStorageDirectory().toString() + File.separator + "Android" + File.separator + "data" + File.separator + "com.example.valdymas";
    private File hFile = new File(path + File.separator + "history.txt");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Switch onOffSwitch = findViewById(R.id.onOff);
        final FloatingActionButton fab = findViewById(R.id.fab);
        final TextView progress = findViewById(R.id.progress);
        final TextView temperature = findViewById(R.id.temp);
        final SeekBar seekBar = findViewById(R.id.seekBar);
        final Button history = findViewById(R.id.history);

        SharedPreferences mPrefs = getSharedPreferences("Settings", 0);
        final SharedPreferences.Editor mEditor = mPrefs.edit();
        boolean status = mPrefs.getBoolean("st", false);
        int temp = mPrefs.getInt("tmp", 20);
        if(status)
        {
            seekBar.setEnabled(false);
            fab.setEnabled(false);
        }
        onOffSwitch.setChecked(status);
        seekBar.setProgress(temp);
        progress.setText("Nustatyta: + " + Integer.toString(temp) + "\u00B0C");

        File dir = new File(path);
        if(!dir.exists() && !dir.isDirectory())
        {
            dir.mkdirs();
        }
        if(!hFile.exists())
        {
            try {
                hFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");
                    if (endOfLineIndex > 0) {
                        if (recDataString.charAt(0) == '#')
                        {
                            sensor = recDataString.substring(1, 6);
                            temperature.setText("Aplinkos temperatūra: +" + sensor + "\u00B0C");
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int prog,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                progress.setText("Nustatyta: + " + String.valueOf(prog) + "\u00B0C");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    seekBar.setEnabled(false);
                    fab.setEnabled(false);
                    mConnectedThread.write("N");
                    Write(hFile, "", sensor, 0);
                    mEditor.putBoolean("st", true).commit();
                    Snackbar.make(findViewById(R.id.fab), "Sistema įjungta!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    seekBar.setEnabled(true);
                    fab.setEnabled(true);
                    mConnectedThread.write("F");
                    Write(hFile, "", sensor, 1);
                    mEditor.putBoolean("st", false).commit();
                    Snackbar.make(findViewById(R.id.fab), "Sistema išjungta!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Write(hFile, Integer.toString(seekBar.getProgress()), sensor, 2);
                mConnectedThread.write(Integer.toString(seekBar.getProgress()));
                mEditor.putInt("tmp", seekBar.getProgress()).commit();
                Snackbar.make(view, "Duomenys išsiųsti! Nustatyta temperatūra: " + Integer.toString(seekBar.getProgress()) + "\u00B0C", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.connection_bt) {
            Intent moveActivity = new Intent(this, BluetoothActivity.class);
            startActivity(moveActivity);
        }
        return super.onOptionsItemSelected(item);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        address = intent.getStringExtra(BluetoothActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Klaida!", Toast.LENGTH_LONG).show();
        }
        try
        {
            btSocket.connect();
            Write(hFile, "", sensor, 3);
        } catch (IOException e) {
            try
            {
                btSocket.close();
                Write(hFile, "", sensor, 4);
            }
            catch (IOException e2)
            {
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close();
        } catch (IOException e2) {
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Toast.makeText(MainActivity.this,"Prisijungimas sėkmingas!",Toast.LENGTH_LONG).show();
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Prisijungimas nepavyko", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

    public static void Write(File file, String temp, String sensor, int stat)
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] data;
        String info;
        switch(stat){
            case 0:
                info = dateformat.format(c.getTime()) + ": Sistema įjungta rankiniu būdu";
                data = info.split("\n");
                Save(file, data);
                break;
            case 1:
                info = dateformat.format(c.getTime()) + ": Sistema išjungta rankiniu būdu";
                data = info.split("\n");
                Save(file, data);
                break;
            case 2:
                info = dateformat.format(c.getTime()) + ": Nustatyta temperatūra +" + temp + "\u00B0C. Aplinkos temperatūra: +" +
                        sensor + "\u00B0C";
                data = info.split("\n");
                Save(file, data);
                break;
            case 3:
                info = dateformat.format(c.getTime()) + ": Sėkmingai prisijungta prie sistemos";
                data = info.split("\n");
                Save(file, data);
                break;
            case 4:
                info = dateformat.format(c.getTime()) + ": Inicijuotas nesėkmingas prisijungimas prie sistemos";
                data = info.split("\n");
                Save(file, data);
                break;
            case 5:
                info = dateformat.format(c.getTime()) + ": Prarastas ryšys su Bluetooth įrenginiu";
                data = info.split("\n");
                Save(file, data);
                break;
            default:
                break;
        }
    }

    public static void Save(File file, String[] data)
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(file, true);
        }
        catch (FileNotFoundException e) {e.printStackTrace();}
        try
        {
            try
            {
                for (int i = 0; i<data.length; i++)
                {
                    fos.write(data[i].getBytes());
                    if (i < data.length)
                    {
                        fos.write("\n".getBytes());
                    }
                }
            }
            catch (IOException e) {e.printStackTrace();}
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch (IOException e) {e.printStackTrace();}
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (device.ACTION_ACL_DISCONNECTED.equals(action)) {
                Write(hFile, "", sensor, 5);
                AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                builder1.setTitle("Prarastas ryšys!");
                builder1.setMessage("Prarastas ryšys su prijungtu įrenginiu, prašome pasirinkti kitą!");
                builder1.setPositiveButton(
                        "Rinktis",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startBluetoothActivity();
                            }
                        });
                builder1.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        startBluetoothActivity();
                    }
                });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            }

        }
    };

    public void startBluetoothActivity()
    {
        Intent i = new Intent(MainActivity.this, BluetoothActivity.class);
        startActivity(i);
    }
}
