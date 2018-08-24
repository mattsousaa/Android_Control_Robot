package com.example.allis.robo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket =  null;

    ConnectedThread connectedThread;

    Handler mHandler;
    StringBuilder dadosBluetooth = new StringBuilder();

    Button btnLigar, btnDesligar, btnConexao;
    boolean conexao = false;
    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static String END = null;
    private static final int MESSAGE_READ = 3;

    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLigar = (Button)findViewById(R.id.btnLigar);
        btnDesligar = (Button)findViewById(R.id.btnDesligar);
        btnConexao = (Button)findViewById(R.id.btnConexao);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (meuBluetoothAdapter == null) {

            Toast.makeText(getApplicationContext(), "Seu dispositivo não possui Bluetooth",Toast.LENGTH_LONG).show();

        } else if (!meuBluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
        }

        btnConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(conexao){
                    //desconetar
                    try{

                        meuSocket.close();
                        conexao = false;
                        btnConexao.setText("Conectar");
                        Toast.makeText(getApplicationContext(), "Bluetooth foi desconectado.", Toast.LENGTH_LONG).show();

                    }catch(IOException erro){

                        Toast.makeText(getApplicationContext(), "Erro: "+erro, Toast.LENGTH_LONG).show();
                    }
                }else{
                    //conectar
                    Intent abreLista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(abreLista, SOLICITA_CONEXAO);
                }
            }
        });
        btnLigar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(conexao){
                    connectedThread.enviar("1");
                }else{
                    Toast.makeText(getApplicationContext(), "Bluetooth não está conectado.", Toast.LENGTH_LONG).show();
                }

            }
        });
        btnDesligar.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {

                if(conexao){
                    connectedThread.enviar("0");
                }else{
                    Toast.makeText(getApplicationContext(), "Bluetooth não está conectado.", Toast.LENGTH_LONG).show();
                }

            }
        });

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if(msg.what == MESSAGE_READ){

                    String recebidos = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), "Bora dar "+recebidos+" cheiro na morena.", Toast.LENGTH_LONG).show();
                }

            }
        };
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){

            case SOLICITA_ATIVACAO:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(), "O Bluetooth foi ativado.",Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "O Bluetooth não foi ativado, o app será encerrado.",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK){

                    END = data.getExtras().getString(ListaDispositivos.ENDERECO);
               //     Toast.makeText(getApplicationContext(), "Endereco final: "+END, Toast.LENGTH_LONG).show();
                    meuDevice = meuBluetoothAdapter.getRemoteDevice(END);

                    try{

                        meuSocket = meuDevice.createRfcommSocketToServiceRecord(MEU_UUID);

                        meuSocket.connect();

                        connectedThread = new ConnectedThread(meuSocket);
                        connectedThread.start();

                        conexao = true;
                        btnConexao.setText("Desconectar");
                        Toast.makeText(getApplicationContext(), "Você foi conectado com: "+END, Toast.LENGTH_LONG).show();
                    } catch(IOException erro){
                        conexao = false;
                        Toast.makeText(getApplicationContext(), "Final: "+erro, Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "Falha ao receber o endereço.",Toast.LENGTH_LONG).show();

                }



        }
    }

    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String dadosBt = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dadosBt).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }

        }

        public void enviar(String dadosEnviar) {
            byte[] msgBuffer = dadosEnviar.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) { }
        }

    }
}
