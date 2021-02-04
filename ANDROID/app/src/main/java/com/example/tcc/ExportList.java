package com.example.tcc;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExportList extends AppCompatActivity  implements AdapterView.OnItemClickListener, Serializable {

    private ExpandableListView lvInfo;
    private ExpandableListAdapter listAdapter;
    private List<String> listDataHeader;
    private HashMap<String,List<String>> listHash;
    TextView tvMsgBox, lbListItem1,lbListItem2,lbListItem3,lbListItem4,tvIni,tvFim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        lvInfo = (ExpandableListView) findViewById(R.id.lvInfo2);
        listDataHeader = new ArrayList<>();
        listHash = new HashMap<>();
        tvMsgBox = (TextView) findViewById(R.id.tvMsgBox);
        lbListItem1 = (TextView) findViewById(R.id.lbListItem1);
        lbListItem2 = (TextView) findViewById(R.id.lbListItem2);
        lbListItem3 = (TextView) findViewById(R.id.lbListItem3);
        lbListItem4 = (TextView) findViewById(R.id.lbListItem4);

        tvIni = (TextView) findViewById(R.id.tvIni);
        tvFim = (TextView) findViewById(R.id.tvFim);


        try{
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                listDataHeader = (List<String>)extras.getSerializable("listDataHeader");
                listHash = (HashMap<String,List<String>>)extras.getSerializable("listHash");
                String s = extras.getString("info");

                tvIni.setText(extras.getString("inicio"));
                tvFim.setText(extras.getString("fim"));

                listAdapter = new ExpandableListAdapter(getApplicationContext(),listDataHeader,listHash);
                lvInfo.setAdapter(listAdapter);

                String[] childArr = s.split(",");
                lbListItem1.setText(childArr[0]);
                lbListItem2.setText(childArr[1]);
                lbListItem3.setText(childArr[2]);
                lbListItem4.setText(childArr[3]);
            }
        }catch (Exception e){
            alertShow(e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.exportar_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private void alertShow(String msg){
        AlertDialog.Builder alerta = new AlertDialog.Builder(getApplicationContext());
        alerta
                .setTitle("Erro")
                .setIcon(R.mipmap.ic_launcher_round)
                .setMessage(msg)
                .setCancelable(true)
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alDev = alerta.create();
        alDev.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item1:

                //Exportar para planilha
                int linha = 0;
                Workbook wb=new HSSFWorkbook();
                Cell cell = null;
                Row row = null;
                //CellStyle cellStyle=wb.createCellStyle();
                //cellStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
                //cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                //cell.setCellStyle(cellStyle);

                //Now we are creating sheet
                Sheet sheet=null;
                sheet = wb.createSheet("LISTA");

                //Informações de início e fim
                row = sheet.createRow(0);
                cell = row.createCell(0);
                cell.setCellValue(tvIni.getText().toString());

                row = sheet.createRow(1);
                cell = row.createCell(0);
                cell.setCellValue(tvFim.getText().toString());

                //Informações de total
                //Cabeçalhos de totais
                row = sheet.createRow(3);
                cell = row.createCell(0);
                cell.setCellValue("TOTAL");

                row = sheet.createRow(4);
                cell = row.createCell(0);
                cell.setCellValue("NORMAL");

                cell = row.createCell(1);
                cell.setCellValue("REVERSO");

                cell = row.createCell(2);
                cell.setCellValue("FALHAS");

                cell = row.createCell(3);
                cell.setCellValue("TENSÃO");

                //Valores totais
                row = sheet.createRow(5);
                cell = row.createCell(0);
                cell.setCellValue(lbListItem1.getText().toString());

                cell = row.createCell(1);
                cell.setCellValue(lbListItem2.getText().toString());

                cell = row.createCell(2);
                cell.setCellValue(lbListItem3.getText().toString());

                cell = row.createCell(3);
                cell.setCellValue(lbListItem4.getText().toString());

                //Valores da lista
                linha = 7;
                for(int i = 0; i < listDataHeader.size();i++){
                    row = sheet.createRow(linha);
                    cell=row.createCell(0);
                    cell.setCellValue(listDataHeader.get(i).toString());
                    linha++;

                    row = sheet.createRow(linha);
                    cell=row.createCell(0);
                    cell.setCellValue("HORÁRIO");
                    cell=row.createCell(1);
                    cell.setCellValue("NORMAL");
                    cell=row.createCell(2);
                    cell.setCellValue("REVERSO");
                    cell=row.createCell(3);
                    cell.setCellValue("FALHAS");
                    cell=row.createCell(4);
                    cell.setCellValue("TENSÃO");
                    linha++;

                    //Linha 0 = cabeçalho
                    for(int j = 1; j < listHash.get(listDataHeader.get(i)).size(); j++){
                        row = sheet.createRow(linha);

                        String strVl = listHash.get(listDataHeader.get(i)).get(j).toString();
                        String[] vl = strVl.split(",");

                        cell=row.createCell(0);
                        cell.setCellValue(vl[0]+":00");
                        cell=row.createCell(1);
                        cell.setCellValue(vl[1]);
                        cell=row.createCell(2);
                        cell.setCellValue(vl[2]);
                        cell=row.createCell(3);
                        cell.setCellValue(vl[3]);
                        cell=row.createCell(4);
                        cell.setCellValue(vl[4]);
                        linha++;
                    }
                    linha++;
                }

                sheet.setColumnWidth(0,(10*200));
                sheet.setColumnWidth(1,(10*200));
                sheet.setColumnWidth(2,(10*200));
                sheet.setColumnWidth(3,(10*200));
                sheet.setColumnWidth(4,(10*200));

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"CPTM.xls");
                FileOutputStream outputStream =null;

                try {
                    //outputStream=new FileOutputStream(file);
                    outputStream=new FileOutputStream(file,true);
                    wb.write(outputStream);
                    Toast.makeText(getApplicationContext(),"Arquivo 'CPTM.xls' salvo na pasta de Downloads.",Toast.LENGTH_LONG).show();
                } catch (java.io.IOException e) {
                    e.printStackTrace();

                    Toast.makeText(getApplicationContext(),"NO OK",Toast.LENGTH_LONG).show();
                    try {
                        outputStream.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
