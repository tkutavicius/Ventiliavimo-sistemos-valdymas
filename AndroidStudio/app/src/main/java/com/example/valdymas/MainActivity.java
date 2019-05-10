package com.example.valdymas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket = null;
    private ConnectedThread mConnectedThread;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address;

    private String path = Environment.getExternalStorageDirectory().toString() + File.separator + "Android" + File.separator + "data" + File.separator + "com.example.valdymas";
    private File hFile = new File(path + File.separator + "history.txt");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Switch onOffSwitch = findViewById(R.id.onOff);
        final FloatingActionButton fab = findViewById(R.id.fab);
        final TextView progress = findViewById(R.id.progress);
        final SeekBar seekBar = findViewById(R.id.seekBar);
        final Button history = findViewById(R.id.history);

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
                    Write(hFile, "", 0);
                    Snackbar.make(findViewById(R.id.fab), "Sistema įjungta!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    seekBar.setEnabled(true);
                    fab.setEnabled(true);
                    mConnectedThread.write("F");
                    Write(hFile, "", 1);
                    Snackbar.make(findViewById(R.id.fab), "Sistema išjungta!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Write(hFile, Integer.toString(seekBar.getProgress()), 2);
                mConnectedThread.write(Integer.toString(seekBar.getProgress()));
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
            Write(hFile, "", 3);
        } catch (IOException e) {
            try
            {
                btSocket.close();
                Write(hFile, "", 4);
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
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            OutputStream tmpOut = null;

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmOutStream = tmpOut;
            Toast.makeText(MainActivity.this,"Prisijungimas sėkmingas!",Toast.LENGTH_LONG).show();
        }

        public void run() {
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

    public static void Write(File file, String temp, int stat)
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
                info = dateformat.format(c.getTime()) + ": Nustatyta temperatūra +" + temp + "\u00B0C";
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
}
