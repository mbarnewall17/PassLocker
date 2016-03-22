package com.barnewall.matthew.passlocker;

import android.graphics.Bitmap;

/**
 * Created by Matthew on 4/13/2015.
 */
public class IconListViewItem implements Comparable<IconListViewItem>{
    private Bitmap picture;
    private String text;

    public IconListViewItem(Bitmap picture, String text){
        this.picture = picture;
        this.text    = text;
    }

    public Bitmap getPicture(){
        return picture;
    }

    public String getText(){
        return text;
    }

    /*
     * Compare IconListViewItems
     * Uses text to compare
     *
     * @param   item    the item to compare to
     * @return          -1 less than, 0 equal, 1 greater
     */
    @Override
    public int compareTo(IconListViewItem item) {
        return text.compareTo(item.getText());
    }
}
