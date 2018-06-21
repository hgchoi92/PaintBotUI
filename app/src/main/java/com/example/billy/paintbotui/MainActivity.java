package com.example.billy.paintbotui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    TextView readingFL;
    TextView readingFR;
    TextView readingSL;
    TextView readingSR;
    Button onButton;
    Button offButton;
    Button getReadingsButton;
    final byte delimiter = 33;
    int readBufferPosition = 0;


    public void sendBtMsg(String msg2send){
        //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()){
                mmSocket.connect();
            }

            String msg = msg2send;
            //msg += "\n";
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void findViews(){

         readingFL = (TextView) findViewById(R.id.readingFL);
         readingFR = (TextView) findViewById(R.id.readingFR);
         readingSL = (TextView) findViewById(R.id.readingSL);
         readingSR = (TextView) findViewById(R.id.readingSR);
         onButton = (Button) findViewById(R.id.onButton);
         offButton = (Button) findViewById(R.id.offButton);
         getReadingsButton = (Button) findViewById(R.id.getReadingsButton);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

        final Handler handler = new Handler();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                sendBtMsg(btMsg);
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {

                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e("Aquarium recv bt","bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            //myLabel.setText(data);
                                        }
                                    });

                                    workDone = true;
                                    break;
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                            if (workDone == true){
                                mmSocket.close();
                                break;
                            }

                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        };


        // start temp button handler

        onButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                (new Thread(new workerThread("on"))).start();

            }
        });


        //end temp button handler

        //start light on button handler
        offButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                (new Thread(new workerThread("off"))).start();

            }
        });
        //end light on button handler

        //start light off button handler

        getReadingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on temp button click

                (new Thread(new workerThread("getReadings"))).start();

            }
        });

        // end light off button handler

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {

                if(device.getName().equals("raspberrypi")) //Note, you will need to change this to match the name of your device
                {
                    Log.e("PaintBot",device.getName());
                    mmDevice = device;
                    break;
                }
            }
        }


    }

}