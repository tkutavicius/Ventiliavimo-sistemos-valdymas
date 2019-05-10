package com.example.valdymas;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class HistoryActivity extends AppCompatActivity {

    ArrayAdapter adapter;
    ListView historyList;
    private String path = Environment.getExternalStorageDirectory().toString() + File.separator + "Android" + File.separator + "data" + File.separator + "com.example.valdymas";
    File hFile = new File(path + File.separator + "history.txt");

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        String[] data = Load(hFile);
        String[] history = new String[data.length];
        for (int i = 0; i < data.length; i++ )
        {
            history[i] = data[i];
        }
        adapter = new ArrayAdapter<String>(this,
                R.layout.text_listview, history);
        historyList = findViewById(R.id.lv_history);
        historyList.setAdapter(adapter);
        View deleteHistory = findViewById(R.id.deleteHistory);
        deleteHistory.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                AlertDialog.Builder adb = new AlertDialog.Builder(HistoryActivity.this);
                adb.setTitle("Istorijos išvalymas");
                adb.setMessage("Ar tikrai norite išvalyti mobiliosios aplikacijos naudojimo istoriją?");
                adb.setPositiveButton("Išvalyti", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        hFile.delete();
                        try {
                            hFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        finish();
                        startActivity(getIntent());
                    }
                });
                adb.setNegativeButton("Atšaukti", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                adb.show();
            }
        });
    }

    public static String[] Load(File file)
    {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {e.printStackTrace();}
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        String test;
        int anzahl=0;
        try
        {
            while ((test=br.readLine()) != null)
            {
                anzahl++;
            }
        }
        catch (IOException e) {e.printStackTrace();}

        try
        {
            fis.getChannel().position(0);
        }
        catch (IOException e) {e.printStackTrace();}

        String[] array = new String[anzahl];

        String line;
        int i = 0;
        try
        {
            while((line=br.readLine())!=null)
            {
                array[i] = line;
                i++;
            }
        }
        catch (IOException e) {e.printStackTrace();}
        return array;
    }
}
