package com.example.tcc;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.HashMap;
import java.util.List;

public class ExpandableListAdapter extends BaseExpandableListAdapter  {
    private Context context;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listHashMap;
    private LayoutInflater inflater;

    public ExpandableListAdapter(Context context, List<String> listDataHeader, HashMap<String, List<String>> listHashMap){
        this.context = context;
        this.listDataHeader = listDataHeader;
        this.listHashMap = listHashMap;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getGroupCount() {
        return listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (listHashMap == null) {
            Log.i("listHashMap", "listHashMap = null " + Integer.toString(listHashMap.size()));
        }
        return listHashMap.get(listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listDataHeader.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listHashMap.get(listDataHeader.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        try{
            ViewHolderGroup holder;

            String headerTitle = (String)getGroup(groupPosition);
            if(convertView == null){
                //LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_group,null);
                holder = new ViewHolderGroup();
                convertView.setTag(holder);

                holder.tvGroup = (TextView) convertView.findViewById(R.id.lbListHeader);
            }else{
                holder = (ViewHolderGroup)convertView.getTag();
            }

            holder.tvGroup.setTypeface(null, Typeface.BOLD);
            holder.tvGroup.setText(headerTitle);

            //TextView lbListHeader = (TextView) convertView.findViewById(R.id.lbListHeader);
            //lbListHeader.setTypeface(null, Typeface.BOLD);
            //lbListHeader.setText(headerTitle);
        }catch(Exception e){
            alertShow(e.toString());
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        try{
            ViewHolderItem holder;
            final String childText = (String)getChild(groupPosition,childPosition);
            final String[] childArr = childText.split(",");

            if(convertView == null){
                //LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item,null);
                holder = new ViewHolderItem();
                convertView.setTag(holder);

                holder.tvItem1 = (TextView) convertView.findViewById(R.id.lbListItem1);
                holder.tvItem2 = (TextView) convertView.findViewById(R.id.lbListItem2);
                holder.tvItem3 = (TextView) convertView.findViewById(R.id.lbListItem3);
                holder.tvItem4 = (TextView) convertView.findViewById(R.id.lbListItem4);
                holder.tvItem5 = (TextView) convertView.findViewById(R.id.lbListItem5);
            }else{
                holder = (ViewHolderItem)convertView.getTag();
            }

            holder.tvItem1.setText(childArr[0]+((childPosition == 0)?"":":00"));
            holder.tvItem2.setText(childArr[1]);
            holder.tvItem3.setText(childArr[2]);
            holder.tvItem4.setText(childArr[3]);
            holder.tvItem5.setText(childArr[4]);

            if(childPosition == 0) {
                holder.tvItem1.setBackgroundColor(Color.BLACK);
                holder.tvItem2.setBackgroundColor(Color.BLACK);
                holder.tvItem3.setBackgroundColor(Color.BLACK);
                holder.tvItem4.setBackgroundColor(Color.BLACK);
                holder.tvItem5.setBackgroundColor(Color.BLACK);
                holder.tvItem1.setTextColor(Color.WHITE);
                holder.tvItem2.setTextColor(Color.WHITE);
                holder.tvItem3.setTextColor(Color.WHITE);
                holder.tvItem4.setTextColor(Color.WHITE);
                holder.tvItem5.setTextColor(Color.WHITE);
            }else{
                holder.tvItem1.setBackgroundColor(Color.WHITE);
                holder.tvItem2.setBackgroundColor(Color.WHITE);
                holder.tvItem3.setBackgroundColor(Color.WHITE);
                holder.tvItem4.setBackgroundColor(Color.WHITE);
                holder.tvItem5.setBackgroundColor(Color.WHITE);
                holder.tvItem1.setTextColor(Color.BLACK);
                holder.tvItem2.setTextColor(Color.BLACK);
                holder.tvItem3.setTextColor(Color.BLACK);
                holder.tvItem4.setTextColor(Color.BLACK);
                holder.tvItem5.setTextColor(Color.BLACK);
            }
        }catch(Exception e){
            alertShow(e.toString());
        }



        //TextView lbListHeader = (TextView) convertView.findViewById(R.id.lbListHeader);
        //lbListHeader.setTypeface(null, Typeface.BOLD);
        //lbListHeader.setText(headerTitle);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    class ViewHolderGroup{
        TextView tvGroup;
    }

    class ViewHolderItem{
        TextView tvItem1;
        TextView tvItem2;
        TextView tvItem3;
        TextView tvItem4;
        TextView tvItem5;
    }

    private void alertShow(String msg){
        AlertDialog.Builder alerta = new AlertDialog.Builder(context);
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
}
