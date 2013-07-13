package com.opassclient;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello",
            "bar@example.com:world"
    };

    /**
     * The default email to populate the email field with.
     */
    public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mEmail;
    private String mPassword;
    private String longTermPassword = "test";

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private String tspResponse;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Recieved intent","broadcast receiver");
            Toast.makeText(context, "Intent Detected."+intent.getExtras().getString("SMS"), Toast.LENGTH_LONG).show();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String value = extras.getString("SMS");
                String from = extras.getString("FROM");
                Log.i("SMS passed by intent = >>>", value);
//                handleSMS(value,from);
            }

        }
    };

    public void showPopUp() {

        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("Password");
        helpBuilder.setMessage("Enter Long Term Password");
        helpBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing but close the dialog

                    }
                });

        helpBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });



        final EditText input = new EditText(this);
        input.setId(1000);
        input.setSingleLine();
        input.setText("");
        helpBuilder.setView(input);

        // Remember, create doesn't show the dialog
        final AlertDialog helpDialog = helpBuilder.create();

        helpDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button b = helpDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Do something
                        EditText input = (EditText) helpDialog.findViewById(1000);
                        if (!longTermPassword.equals(input.getText().toString())){
                            input.setError("Password dosen't match");
                            input.setFocusable(true);

                        }else {
                        //Dismiss once everything is OK.
                            sendRegistrationConfirmation(tspResponse);

                            helpDialog.dismiss();
                        }
                    }
                });
            }
        });

        helpDialog.show();

    }


    public  void  backButtonClicked(View backButton){
        Log.i("back","back button pressed");
        finish();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.opassserver.SMS_MESSAGE_RECEIVED");
        this.registerReceiver(this.receiver, filter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    private void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

//        SmsManager sms = SmsManager.getDefault();
//        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);


        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message);

        ArrayList<PendingIntent> sendIntentList = new ArrayList<PendingIntent>();
        sendIntentList.add(sentPI);
        ArrayList<PendingIntent> deliveredIntentList = new ArrayList<PendingIntent>();
        deliveredIntentList.add(deliveredPI);

//        for (String sms : parts){
//
//            smsManager.sendTextMessage(phoneNumber, null, sms, null, null);
//
//        }
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sendIntentList, deliveredIntentList);

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {


//        String phoneNumber = "5556";
//        String message = "Hello World! Now we are going to demonstrate " +
//                "how to send a message with more than 160 characters from your Android application"
//                +"In publishing and graphic design, lorem ipsum[1] is a placeholder text (filler text) commonly used to demonstrate the graphic elements of a document or visual presentation, such as font, typography, and layout, by removing the distraction of meaningful content. The lorem ipsum text is typically a section of a Latin text by Cicero with words altered, added, and removed that make it nonsensical and not proper Latin";
//
//        SmsManager smsManager = SmsManager.getDefault();
//        ArrayList<String> parts = smsManager.divideMessage(message);
//        for (String messageParts : parts){
//            Log.i("Message Div",messageParts);
//        }
//        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);



        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!mEmail.contains("@")) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);

            requestRegistrationFromTSP();


//            mAuthTask = new UserLoginTask();
//            mAuthTask.execute((Void) null);
        }
    }
public void sendRegistrationConfirmation(String message)  {
    try {

        String iv = "0000000000000000";


        JSONObject jObject =  new JSONObject(message);
        String sharedKey = jObject.getString("sharedkey");
        String seed = jObject.getString("seed");
        String telephone = jObject.getString("telephone");
        String servername = jObject.getString("servername");
        String messageDigest = longTermPassword+servername+seed;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] c = digest.digest(messageDigest.getBytes("UTF-8"));

        String sha = Base64.encodeToString(c,Base64.DEFAULT);

        String messageDisgestWithSeed = messageDigest+"|"+seed;
        String encryptedData = StringCryptor.encrypt(messageDisgestWithSeed,sharedKey,iv);


        JSONObject requestObject = new JSONObject();
        requestObject.put("username",mEmail);
        requestObject.put("servername",mPassword);
        requestObject.put("data",encryptedData);
        requestObject.put("sha",sha);
        requestObject.put("iv",iv);

        String phoneNumber = "5554";
        String messageText = toHex(requestObject.toString());//Base64.encodeToString( requestObject.toString().getBytes(),Base64.DEFAULT);
//        sendSMS( phoneNumber,messageText);

ArrayList<String> messageList = new ArrayList<String>();
messageList.add("Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy tex");
messageList.add("Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years");
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendMultipartTextMessage(phoneNumber,"5556",messageList,null,null);



        StringEntity entity = null;
        try {
            entity = new StringEntity(requestObject.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        AsyncHttpClient client = new AsyncHttpClient();

        client.post(getBaseContext(),"http://10.0.2.2/registration_complete.php",entity,"application/json",new AsyncHttpResponseHandler(){

            @Override
            public void onSuccess(String s) {
                super.onSuccess(s);
                try {
                    JSONObject responseObject = new JSONObject(s);
                    if(responseObject.getString("status").equals("1")){

                        Toast.makeText(getBaseContext(),"Registration Completed",2).show();
                    }
                    else {

                        Toast.makeText(getBaseContext(),"Already Registered",2).show();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });


    } catch (JSONException e) {
        e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
    } catch (Exception e) {
        e.printStackTrace();
    }


}

    public String toHex(String arg) {
        return String.format("%040x", new BigInteger(arg.getBytes(/*YOUR_CHARSET?*/)));
    }

    public  void  requestRegistrationFromTSP() {




        try {


            JSONObject jObject =  new JSONObject();
            jObject.put("username",mEmail);
            jObject.put("servername",mPassword);

                StringEntity entity = null;

                try {
                    entity = new StringEntity(jObject.toString());
                    Log.i("request json string to TSP>>",jObject.toString());


                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }






         AsyncHttpClient client = new AsyncHttpClient();

         client.post(getBaseContext(),"http://10.0.2.2/tsp.php",entity,"application/json",new AsyncHttpResponseHandler(){



             @Override
             public void onFailure(Throwable throwable, String s) {
                 Log.i("Failure to POST", "POST unsuccessfull");
                 super.onFailure(throwable, s);
             }

             @Override
                    public void onFinish() {
                        showProgress(false);
                        super.onFinish();

                    }

                    public void onSuccess(String response) {

                        try {
                            showProgress(false);
                            Log.i("Response>>", response);
                            JSONObject responseJson = new JSONObject(response);
                            if (responseJson.getString("status").equals("1")){
                            tspResponse = response;
                            showPopUp();
                            }
                            else {
                                Toast.makeText(getBaseContext(),"Unable to identify domain",2).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        super.onSuccess(response);

                    }

                });




        } catch (JSONException e) {
            System.out.print("Asyn process throws error");
            e.printStackTrace();
        }

    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
//                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
