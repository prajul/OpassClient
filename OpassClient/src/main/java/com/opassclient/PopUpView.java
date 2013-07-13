package com.opassclient;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Prajul on 2/7/13.
 */
public class PopUpView extends LinearLayout{

    private EditText password;
    private Button okButton;

    public PopUpView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        password = (EditText)findViewById(R.id.long_password);
        okButton = (Button) findViewById(R.id.ok);


    }
}
