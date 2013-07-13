package com.opassclient;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;

public class OpassActions extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.opass_actions);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.opass_actions, menu);
        return true;
    }

    public void registrationClicked(View registrationButton) {
        Intent myIntent = new Intent(this, LoginActivity.class);
//        myIntent.putExtra("key", "registatopm"); //Optional parameters
        this.startActivity(myIntent);
    }

    
}
