package com.martin.rayate.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.martin.rayate.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class listaAdaptador extends RecyclerView.Adapter<listaAdaptador.ViewHolder> {
    public List<String> fileNameList;
    public List<String> fileDoneList;

    public listaAdaptador(List<String> fileNameList, List<String> fileDoneList) {
        this.fileNameList = fileNameList;
        this.fileDoneList = fileDoneList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lista_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String fileName = fileNameList.get(position);
        holder.fileName.setText(fileName);
        String fileDone = fileDoneList.get(position);
        if (fileDone.equals("Uploading")){
            holder.fileDone.setImageResource(R.drawable.progress_horizontal);
        } else {
            holder.fileDone.setImageResource(R.drawable.ic_check);
        }
    }

    @Override
    public int getItemCount() {
        return fileNameList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView fileName;
        public ImageView fileDone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.txtFilename);
            fileDone = itemView.findViewById(R.id.imgLoading);
        }
    }
}