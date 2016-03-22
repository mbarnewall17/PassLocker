package com.barnewall.matthew.passlocker;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Matthew on 4/13/2015.
 */
public class IconListViewAdapter extends ArrayAdapter<IconListViewItem>{
    private Context context;
    private LayoutInflater inflater;
    ArrayList<IconListViewItem> items;

    public IconListViewAdapter(Context context, int resourceId, ArrayList<IconListViewItem> items){
        super(context, resourceId, items);

        this.context = context;
        this.items = items;
        inflater = LayoutInflater.from(context);
    }


    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if(view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.my_listview, null);
            holder.image = (ImageView)view.findViewById(R.id.imageViewInImageView);
            holder.text = (TextView)view.findViewById(R.id.textViewInListView);
            view.setTag(holder);
        } else {
            holder = (ViewHolder)view.getTag();
        }

        IconListViewItem item = items.get(position);

        holder.text.setText(item.getText());
        holder.image.setImageBitmap(item.getPicture());

        return view;
    }

    private class ViewHolder {
        public ImageView image;
        public TextView text;
    }
}
