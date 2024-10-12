package com.example.sensorx;

import android.widget.Button;

public class ListItem {
    private String text;
    public boolean isButtonEnabled = false;

    public ListItem(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }


}