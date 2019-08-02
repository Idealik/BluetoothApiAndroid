package com.example.bluetoothproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bluetooth1";

    private Button button1, button2;//Указываем id наших кнопок
    private Button connectBtn;
    private Button turnSignalBtn;
    private Button partyBtn;
    private Button stopTurn;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private Spinner mySpinner;
    private Spinner nameSpinner;
    private static Thread turnThread;
    private static Thread partyThread;
    private static boolean isTurning = false;
    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-адрес Bluetooth модуля
    private static String address = "00:00:00:00:00";  //Вместо “00:00” Нужно нудет ввести MAC нашего bluetooth
    private Set<BluetoothDevice> pairedDevices;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        button1 =  findViewById(R.id.onBtn); //Добавляем сюда имена наших кнопок
        button2 =  findViewById(R.id.offBtn);
        connectBtn = findViewById(R.id.connectBtn);
        turnSignalBtn = findViewById(R.id.turnSignalBtn);
        partyBtn = findViewById(R.id.partyBtn);
        stopTurn = findViewById(R.id.stopTurn);

        mySpinner = findViewById(R.id.mySpinner);
        nameSpinner = findViewById(R.id.nameSpinner);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        checkBTState();
        ArrayList<String> currenciesArray = new ArrayList<>();
        for(BluetoothDevice device : pairedDevices){
            currenciesArray.add(device.getAddress());
        }
        ArrayList<String> nameArray = new ArrayList<>();
        int i = 0;
        for(BluetoothDevice device : pairedDevices){
            i++;
            nameArray.add(i+ " " + device.getName());
        }

        ArrayAdapter<String> arrayAdapterAddress = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, currenciesArray);
        arrayAdapterAddress.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        mySpinner.setAdapter(arrayAdapterAddress);


        ArrayAdapter<String> arrayAdapterName= new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, nameArray);
        arrayAdapterName.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        nameSpinner.setAdapter(arrayAdapterName);

        button1.setOnClickListener(new View.OnClickListener()  //Если будет нажата кнопка 1 то
        {
            public void onClick(View v)
            {
                sendData("1");         // Посылаем цифру 1 по bluetooth
                Toast.makeText(getBaseContext(), "Включаем LED", Toast.LENGTH_SHORT).show();  //выводим на устройстве сообщение
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                sendData("0"); // Посылаем цифру 0 по bluetooth
                Toast.makeText(getBaseContext(), "Выключаем LED", Toast.LENGTH_SHORT).show();
            }
        });
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                address = mySpinner.getSelectedItem().toString();
                connect();
            }
        });

        turnSignalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnSignal();
            }
        });
        stopTurn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTurning = false;
                turnThread.interrupt();
            }
        });

        partyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                party();
            }
        });
    }

    private void party() {
        partyThread = new Thread(new Runnable() {
            Random rand = new Random();
            int sleepTime = 400;

            @Override
            public void run() {
                while (true){
                    try {
                        int randomNum = rand.nextInt(6);
                        sendData(String.valueOf(randomNum));
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        partyThread.start();
    }


    private void turnSignal() {

        turnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String onFirstPin = "1";
                String onSecondPin = "3";
                String onThirdPin = "5";

                String offFirstPin = "0";
                String offSecondPin = "2";
                String offThirdPin = "4";
                int sleepTime = 400;
                isTurning = true;
                while (isTurning){
                    try {
                        //включаем первый
                        Log.d(TAG, "включаем первый");
                        sendData(onFirstPin);
                        Thread.sleep(sleepTime);

                        //включаем второй
                        Log.d(TAG, "включаем второй");
                        sendData(onSecondPin);
                        Thread.sleep(sleepTime);

                        Log.d(TAG, "выключаем первый");
                        sendData(offFirstPin);
                        Thread.sleep(sleepTime);

                        Log.d(TAG, "включаем третий");
                        sendData(onThirdPin);
                        Thread.sleep(sleepTime);

                        Log.d(TAG, "выключаем второй");
                        sendData(offSecondPin);
                        Thread.sleep(sleepTime);

                        Log.d(TAG, "выключаем третий");
                        sendData(offThirdPin);
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        turnThread.start();
    }

    private void connect(){
        Log.d(TAG, "...onResume - попытка соединения...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d(TAG, "gg");
            Toast.makeText(getBaseContext(), "Не удалось SOCKET", Toast.LENGTH_SHORT).show();
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Соединяемся...");
        try {
            btSocket.connect();
            Log.d(TAG, "...");
            Toast.makeText(getBaseContext(), "Соединение установлено и готово к передачи данных...", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            try {
                btSocket.close();
                Toast.makeText(getBaseContext(), "Не удалось установить соединение", Toast.LENGTH_SHORT).show();
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "Не удалось установить соединение", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "gg");
            }
        }

        // Create a data stream so we can talk to server.
        //Toast.makeText(getBaseContext(), "Создание Socket", Toast.LENGTH_SHORT).show();

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "gg");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            //спиоок лютуз девайсов
            pairedDevices = btAdapter.getBondedDevices();
        }

    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Посылаем данные: " + message + "...");

        try {
            outStream.write(msgBuffer);

        } catch (IOException e) {
            if (address.equals("00:00:00:00:00:00"))
            Toast.makeText(getBaseContext(), "В переменной address у вас прописан 00:00:00:00:00:00, вам необходимо прописать реальный MAC-адрес Bluetooth модуля", Toast.LENGTH_SHORT).show();
        }
    }
}
