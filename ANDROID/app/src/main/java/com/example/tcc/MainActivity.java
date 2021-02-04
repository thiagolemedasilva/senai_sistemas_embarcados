package com.example.tcc;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

//Excel
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemClickListener{

    Button btSend;
    TextView tvMsgBox, tvStatus;
    TextView tvVal1, tvVal2, tvVal3,tvVal4;
    TextView tvIni, tvFim;
    ListView lvNewDevices;
    EditText editFator;
    View customLayout3;
    String str_msg = "";
    int n_rec = 0;
    boolean refresh_list = true;
    boolean main_cycle = false;
    float fatorConv = 1.0f;

    BluetoothDevice mBTDevice;
    BluetoothAdapter bluetoothAdapter;

    SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    private static final String APP_NAME = "BTC";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;

    //List Info
    private ExpandableListView lvInfo;
    private ExpandableListAdapter listAdapter;
    private List<String> listDataHeader;
    private HashMap<String,List<String>> listHash;
    private List<String> listDataHeader2;
    private HashMap<String,List<String>> listHash2;
    AlertDialog alInf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIdeas();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listDataHeader = new ArrayList<>();
        listHash = new HashMap<>();

        if(bluetoothAdapter != null){
            if(!bluetoothAdapter.isEnabled()){
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
            }
        }else{
            //Dispositivo não possuí Bluetooth
        }

        //Permissões para gravar arquivo Excel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                requestPermission();
            }
        }

        implementListeners();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String ft = preferences.getString("FATOR", "");
        if(ft.equalsIgnoreCase(""))
        {
            ft = "1.0";
        }
        fatorConv = Float.parseFloat(ft);
    }

    protected boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do your work
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void btnSincronize(View view){
        if(bluetoothAdapter == null){
            //Dispositivo não possuí Bluetooth
            Toast.makeText(this,"Dispositivo não possuí Bluetooth.", Toast.LENGTH_SHORT).show();
        }else{
            if(bluetoothAdapter.isEnabled()){
                Calendar c = Calendar.getInstance();
                SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
                String datetime = "t"+dateformat.format(c.getTime());
                sendReceive.write(datetime.getBytes());
            }else{
                Toast.makeText(this,"Bluetooth desabilitado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void btnDiscover(View view) {
        if(bluetoothAdapter == null){
            //Dispositivo não possuí Bluetooth
            Toast.makeText(this,"Dispositivo não possuí Bluetooth.", Toast.LENGTH_SHORT).show();
        }else{

            if(bluetoothAdapter.isEnabled()){
                if(bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();

                    //check BT permissions in manifest
                    checkBTPermissions();

                    Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
                    mBTDevices = new ArrayList<>();

                    if(bt.size()>0){
                        for(BluetoothDevice device : bt){
                            mBTDevices.add(device);
                        }

                        mDeviceListAdapter = new DeviceListAdapter(getApplicationContext(), R.layout.device_adapter_view, mBTDevices);
                        lvNewDevices.setAdapter(mDeviceListAdapter);
                    }

                }else{

                    //check BT permissions in manifest
                    checkBTPermissions();

                    Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
                    mBTDevices = new ArrayList<>();

                    if(bt.size()>0){
                        for(BluetoothDevice device : bt){
                            mBTDevices.add(device);
                        }

                        mDeviceListAdapter = new DeviceListAdapter(getApplicationContext(), R.layout.device_adapter_view, mBTDevices);
                        lvNewDevices.setAdapter(mDeviceListAdapter);
                    }

                }
            }else{
                Toast.makeText(this,"Bluetooth desabilitado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            //Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    private void implementListeners() {

        btSend.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {

                if(bluetoothAdapter == null){
                    //Dispositivo não possuí Bluetooth
                    Toast.makeText(getApplicationContext(),"Dispositivo não possuí Bluetooth.",Toast.LENGTH_SHORT).show();
                }else{
                    //startConnection();
                    if(mBTDevice == null){
                        Toast.makeText(getApplicationContext(),"Nenhum dispositivo selecionado.",Toast.LENGTH_SHORT).show();
                    }else{
                        String string = "f";
                        sendReceive.write(string.getBytes());
                        main_cycle = true;
                    }
                }

                //Atualização manual
                /*tvVal1.setText("30");
                tvVal2.setText("30");
                tvVal3.setText("4");
                tvVal4.setText("108");

                tvStatus.setText("Conectado");

                tvIni.setText("Início: 21/07/2020 - 14:00:02");
                tvFim.setText("Fim   : 21/07/2020 - 15:57:14");*/

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.item1);
        if(bluetoothAdapter == null){
            //Dispositivo não possuí Bluetooth
            item.setTitle("Ativar");
        }else{
            if(bluetoothAdapter.isEnabled()){
                item.setTitle("Desativar");
            }
            else{
                item.setTitle("Ativar");
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void carregarInfo(String s){

        JSONArray jsonArray = null;
        try {
            jsonArray = (JSONArray) new JSONObject(new JSONTokener("{data:"+s+"}")).get("data");

            if(refresh_list){
                listDataHeader = new ArrayList<>();
                listHash = new HashMap<String, List<String>>();
                tvMsgBox.setText("");
            }

            for(int i = 0; i < jsonArray.length();i++){

                if(jsonArray.getJSONArray(i).length() > 0){


                    listDataHeader.add("DIA: "+listHash.size());
                    List<String> a = new ArrayList<>();

                    a.add(
                            "Horário,Normal,Reverso,Falhas,Tensão"
                    );

                    for(int j = 0; j < jsonArray.getJSONArray(i).length();j++){

                        if(Integer.parseInt(jsonArray.getJSONArray(i).getJSONArray(j).get(0).toString()) != -1){
                            a.add(
                                    j+
                                            ","+jsonArray.getJSONArray(i).getJSONArray(j).get(0)+
                                            ","+jsonArray.getJSONArray(i).getJSONArray(j).get(1)+
                                            ","+jsonArray.getJSONArray(i).getJSONArray(j).get(2)+
                                            ","+jsonArray.getJSONArray(i).getJSONArray(j).get(3)
                            );
                        }

                    }
                    listHash.put(listDataHeader.get(listDataHeader.size()-1),a);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final TextView tvStatusInfo = (TextView)this.findViewById(R.id.tvStatus);
        AlertDialog.Builder alerta = new AlertDialog.Builder(MainActivity.this);
        switch (item.getItemId()){
            case R.id.item1:
                enableDisableBT();
                return true;
            case R.id.item2:
                mBTDevice = null;
                //Estava antes
                final View customLayout = getLayoutInflater().inflate(R.layout.device_dialog, null);

                lvNewDevices = (ListView) customLayout.findViewById(R.id.lvNewDevices);
                lvNewDevices.setOnItemClickListener(MainActivity.this);

                alerta
                        .setTitle("Selecionar dispositivo")
                        .setIcon(R.mipmap.ic_launcher_round)
                        .setView(customLayout)
                        .setCancelable(false)
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(bluetoothAdapter == null){
                                    //Dispositivo não possuí Bluetooth
                                }else{

                                    if(bluetoothAdapter.isDiscovering()){
                                        bluetoothAdapter.cancelDiscovery();
                                    }
                                }
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Toast.makeText(getApplicationContext(),"Escolhido OK",Toast.LENGTH_SHORT).show();
                                if(bluetoothAdapter == null){
                                    //Dispositivo não possuí Bluetooth
                                }else{
                                    if(bluetoothAdapter.isDiscovering()){
                                        bluetoothAdapter.cancelDiscovery();
                                    }
                                    //startConnection();
                                    if(mBTDevice == null){
                                        Toast.makeText(getApplicationContext(),"Nenhum dispositivo selecionado.",Toast.LENGTH_SHORT).show();
                                    }else{
                                        ClientClass clientClass = new ClientClass(mBTDevice);
                                        clientClass.start();
                                    }

                                }
                            }
                        });
                AlertDialog alDev = alerta.create();
                alDev.show();
                return true;
            case R.id.item3:

                if(bluetoothAdapter == null){
                    //Dispositivo não possuí Bluetooth
                    Toast.makeText(getApplicationContext(),"Dispositivo não possuí Bluetooth.",Toast.LENGTH_SHORT).show();
                }else{
                    //startConnection();
                    if(mBTDevice == null){
                        Toast.makeText(getApplicationContext(),"Nenhum dispositivo selecionado.",Toast.LENGTH_SHORT).show();
                    }else{
                        refresh_list = true;
                        main_cycle = false;

                        String string = "e";
                        sendReceive.write(string.getBytes());
                    }
                }

                //Atualização manual
                /*String s = "[[[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[-1,-1,-1,-1],[15,15,2,108],[15,15,2,105]]]";
                refresh_list = true;
                carregarInfo(s);

                //Abre a lista em outra Activity
                String ini = "Início: 21/07/2020 14:00:02";
                String fi = "Fim   : 21/07/2020 15:58:32";
                str_msg = "30,30,4,108,0,0,0,0";

                Intent i = new Intent(getBaseContext(), ExportList.class);
                i.putExtra("listDataHeader",(Serializable) listDataHeader);
                i.putExtra("listHash",  (Serializable) listHash);
                i.putExtra("info",  str_msg);
                i.putExtra("inicio",  ini);
                i.putExtra("fim",  fi);
                startActivity(i);


                /*
                String s = "[[[1,2,3,4],[5,6,7,8]]]";

                JSONArray jsonArray = null;
                try {
                    jsonArray = (JSONArray) new JSONObject(new JSONTokener("{data:"+s+"}")).get("data");

                    //Log.i("TESTE", "N: "+jsonArray.length());

                    for(int i = 0; i < jsonArray.length();i++){
                        listDataHeader.add("DIA: "+listHash.size());
                        List<String> a = new ArrayList<>();

                        //Log.i("TESTE", "N2: "+jsonArray.getJSONArray(i).length());

                        if(jsonArray.getJSONArray(i).length()>0){
                            a.add(
                                    "Horário,Esquerda,Direita,Falhas,Tensão"
                            );
                        }

                        for(int j = 0; j < jsonArray.getJSONArray(i).length();j++){

                            a.add(
                                    j+
                                    ","+jsonArray.getJSONArray(i).getJSONArray(j).get(0)+
                                    ","+jsonArray.getJSONArray(i).getJSONArray(j).get(1)+
                                    ","+jsonArray.getJSONArray(i).getJSONArray(j).get(2)+
                                    ","+jsonArray.getJSONArray(i).getJSONArray(j).get(3)
                            );

                        }
                        listHash.put(listDataHeader.get(i),a);
                    }


                    //Abre a lista em outra Activity
                    Intent i = new Intent(getBaseContext(), ExportList.class);
                    i.putExtra("listDataHeader",(Serializable) listDataHeader);
                    i.putExtra("listHash",  (Serializable) listHash);
                    startActivity(i);
                    //listAdapter = new ExpandableListAdapter(getApplicationContext(),listDataHeader,listHash);
                    //lvInfo.setAdapter(listAdapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                */

                return true;
            case R.id.item4:

                final View customLayout3 = getLayoutInflater().inflate(R.layout.configuracoes_dialog, null);
                editFator = (EditText) customLayout3.findViewById(R.id.editFator);

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String ft = preferences.getString("FATOR", "");
                if(ft.equalsIgnoreCase(""))
                {
                    ft = "1.0";
                }
                editFator.setText(ft);

                alerta
                        .setTitle("Configurações")
                        .setIcon(R.mipmap.ic_launcher_round)
                        //.setView(R.layout.configuracoes_dialog)
                        .setView(customLayout3)
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String ft = "1.0";
                                if(editFator.getText().toString() != ""){
                                    ft = editFator.getText().toString();
                                }

                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("FATOR",ft);
                                editor.apply();
                            }
                        });
                AlertDialog alConfig = alerta.create();
                alConfig.show();

                return true;
            case R.id.item5:

                alerta
                        .setTitle("Sobre")
                        .setIcon(R.mipmap.ic_launcher_round)
                        .setView(R.layout.sobre_dialog)
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                AlertDialog alSobre = alerta.create();
                alSobre.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void enableDisableBT(){

        if(bluetoothAdapter == null){
            //Dispositivo não possuí Bluetooth
            Toast.makeText(this,"Dispositivo não possuí Bluetooth.", Toast.LENGTH_SHORT).show();
        }else{
            if(!bluetoothAdapter.isEnabled()){

                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);

            }else{
                //Desabilitando Bluetooth
                bluetoothAdapter.disable();
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what)
            {
                case STATE_LISTENING:
                    tvStatus.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    tvStatus.setText("Conectando...");
                    break;
                case STATE_CONNECTED:
                    tvStatus.setText("Conectado");
                    break;
                case STATE_CONNECTION_FAILED:
                    tvStatus.setText("Falha na conexão");
                    break;
                case STATE_MESSAGE_RECEIVED:

                    try{

                        byte[] readBuff = (byte[])msg.obj;
                        String tmp_msg = new String(readBuff,0,msg.arg1);
                        str_msg = tmp_msg;
                        //tvMsgBox.setText(str_msg);

                        if(main_cycle){

                            str_msg = str_msg.substring(1,str_msg.length());
                            String[] vl = str_msg.split(",");
                            if(vl.length == 10) {

                                //Não iniciado
                                if(Integer.parseInt(vl[4]) == 0){
                                    tvVal1.setText("X");
                                    tvVal2.setText("X");
                                    tvVal3.setText("X");
                                    tvVal4.setText("X");

                                    tvIni.setText("Dispositivo não sincronizado.");
                                    tvFim.setText("");
                                }else{
                                    tvVal1.setText(vl[0]);
                                    tvVal2.setText(vl[1]);
                                    tvVal3.setText(vl[2]);
                                    tvVal4.setText(vl[3]);

                                    tvIni.setText(
                                            "Início: "+
                                            convertNum(vl[6])+"/"+
                                            convertNum(vl[5])+"/"+
                                            vl[4]+" - "+
                                            convertNum(vl[7])+":"+
                                            convertNum(vl[8])+":"+
                                            convertNum(vl[9])
                                    );

                                    Calendar c = Calendar.getInstance();
                                    SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
                                    String datetime = dateformat.format(c.getTime());
                                    tvFim.setText("Fim   : "+datetime);

                                }
                            }

                            main_cycle = false;
                        }else{

                            String fim = "F";
                            if(!(str_msg.substring(0,1).toString().equals(fim.toString())) && str_msg.length() > 5){
                                carregarInfo(str_msg);
                                refresh_list = false;
                                String string = "e";
                                sendReceive.write(string.getBytes());
                            }else{
                                tvIni.setText("Finalizou");
                                refresh_list = true;

                                //Abre a lista em outra Activity
                                str_msg = str_msg.substring(1,str_msg.toString().length());

                                String[] childArr = str_msg.split(",");

                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                                Date firstDate = (Date) sdf.parse(convertNum(childArr[6])+"/"+ convertNum(childArr[5])+"/"+ childArr[4]);

                                listDataHeader2 = new ArrayList<>();
                                listHash2 = new HashMap<String, List<String>>();
                                for(int j = 0; j < listDataHeader.size();j++){

                                    if(j == 0) {
                                        listDataHeader2.add(sdf.format(firstDate).toString());
                                        listHash2.put(listDataHeader2.get(j).toString(),listHash.get(listDataHeader.get(j)));
                                    }else{
                                        firstDate = DateUtil.addDays(firstDate, 1);
                                        listDataHeader2.add(sdf.format(firstDate).toString());
                                        listHash2.put(listDataHeader2.get(j).toString(),listHash.get(listDataHeader.get(j)));
                                    }

                                }

                                String ini = "Início: "+
                                        convertNum(childArr[6])+"/"+
                                        convertNum(childArr[5])+"/"+
                                        childArr[4]+" - "+
                                        convertNum(childArr[7])+":"+
                                        convertNum(childArr[8])+":"+
                                        convertNum(childArr[9]);


                                Calendar c = Calendar.getInstance();
                                SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
                                String datetime = dateformat.format(c.getTime());
                                String fi = "Fim   : "+datetime;

                                Intent i = new Intent(getBaseContext(), ExportList.class);
                                i.putExtra("listDataHeader",(Serializable) listDataHeader2);
                                i.putExtra("listHash",  (Serializable) listHash2);
                                i.putExtra("info",  str_msg);
                                i.putExtra("inicio",  ini);
                                i.putExtra("fim",  fi);
                                startActivity(i);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        tvIni.setText(e.toString()+" - "+str_msg+" - "+str_msg.length());
                    }

                    break;
            }
            return true;
        }
    });

    private String convertNum(String s){
        if(Integer.parseInt(s) < 10){
            s = "0"+s;
        }
        return s;
    }

    private void findViewByIdeas(){
        btSend = (Button) findViewById(R.id.btSend);
        tvMsgBox = (TextView) findViewById(R.id.tvMsgBox);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        tvIni = (TextView) findViewById(R.id.tvIni);
        tvFim = (TextView) findViewById(R.id.tvFim);

        customLayout3 = getLayoutInflater().inflate(R.layout.exportar_dialog, null);
        lvInfo = (ExpandableListView) customLayout3.findViewById(R.id.lvInfo);
        lvInfo.setOnItemClickListener(MainActivity.this);

        tvVal1 = (TextView) findViewById(R.id.val1);
        tvVal2 = (TextView) findViewById(R.id.val2);
        tvVal3 = (TextView) findViewById(R.id.val3);
        tvVal4 = (TextView) findViewById(R.id.val4);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBTDevice = mBTDevices.get(position);
    }

    private class ServerClass extends  Thread{
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket = null;

            while(socket == null){
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket != null){
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();

                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread{
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1){
            device = device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            //Anterior
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run(){
            byte[] buffer = new byte[1024*16];
            int bytes;

            while(true){
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public static class DateUtil
    {
        public static Date addDays(Date date, int days)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, days); //minus number would decrement the days
            return (Date) cal.getTime();
        }
    }


}
