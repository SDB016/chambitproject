package com.example.chambitproject;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HosunViewer extends LinearLayout {
    TextView textView;

    public HosunViewer(Context context) {
        super(context);
        init(context);
    }

    public HosunViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hosunitem,this,true);
        textView = (TextView) findViewById(R.id.textView);
    }

    public void setItem(String hosunItem) {
        textView.setText(hosunItem);
    }
}