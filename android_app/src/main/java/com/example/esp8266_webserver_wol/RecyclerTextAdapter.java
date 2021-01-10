package com.example.esp8266_webserver_wol;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerTextAdapter extends RecyclerView.Adapter<RecyclerTextAdapter.ViewHolder> {
    private ArrayList<RecyclerItem> mData = null ;

    // ArrayList 데이터 얻기
    RecyclerTextAdapter(ArrayList<RecyclerItem> list) {
        mData = list ;
    }

    // onCreateViewHolder() - 어댑터 정의
    @Override
    public RecyclerTextAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext() ;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;

        View view = inflater.inflate(R.layout.recycler_item, parent, false) ;
        RecyclerTextAdapter.ViewHolder vh = new RecyclerTextAdapter.ViewHolder(view) ;

        return vh ;
    }

    // onBindViewHolder() - 데이터 입력
    @Override
    public void onBindViewHolder(RecyclerTextAdapter.ViewHolder holder, int position) {
        RecyclerItem item = mData.get(position) ;
        holder.name.setText(item.getName()) ;
        holder.mac.setText(item.getMac()) ;
    }

    // 전체 크기 가져오기
    @Override
    public int getItemCount() {
        return mData.size() ;
    }

    // Viewholder
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name ;
        TextView mac ;

        ViewHolder(View itemView) {
            super(itemView) ;
            name = itemView.findViewById(R.id.textName) ;
            mac = itemView.findViewById(R.id.textMac) ;
            
            // 클릭 이벤트
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION) {
                        ((MainActivity)MainActivity.mContext).WoLSend(pos);
                    }
                }
            });
            
            // 롱클릭 이벤트
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION) {
                        ((MainActivity)MainActivity.mContext).ProcessItemControl(pos);
                    }
                    return true;
                }
            });
        }
    }
}