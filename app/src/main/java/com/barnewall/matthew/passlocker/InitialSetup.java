package com.barnewall.matthew.passlocker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class InitialSetup extends ActionBarActivity {
    static final int REQUEST_LINK_TO_DBX = 0;  // This value is up to you

    //Used to link dropbox, comes from dropbox datastore api tutorial
    private Button              mLinkButton;
    private DbxAccountManager   mAccountManager;
    private DbxDatastoreManager mDatastoreManager;

    //Used to decrypt data when changing password
    private boolean             changePassword;
    private String              password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_initial_setup);

        //Set up for when changing password
        Bundle extra = getIntent().getExtras();

        //Change password
        if (extra != null) {
            changePassword = extra.getBoolean("changePassword");
            if(changePassword) {
                ((Button) findViewById(R.id.link_button)).setVisibility(View.GONE);
                ((TextView) findViewById(R.id.welcomeTextView)).setText("Select a method to change your password.");
                password = extra.getString("password");
            }
        }

        //Initial setup of the application
            checkSetUp();
            setUpDropbox();

        //Hides the action bar
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }

    /*
     * This following code is used to link the app with the users Dropbox account
     * Code Comes From https://www.dropbox.com/developers/datastore/tutorial/android
     */
    private void setUpDropbox(){

        // Set up the account manager
        mAccountManager = DbxAccountManager.getInstance(getApplicationContext(), "oh8dsmwj9jp2es1", "b33vntcwrv9v1h1");
        // Button to link to Dropbox
        mLinkButton = (Button) findViewById(R.id.link_button);
        mLinkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAccountManager.startLink((Activity)InitialSetup.this, REQUEST_LINK_TO_DBX);
            }
        });
        // Set up the datastore manager
        if (mAccountManager.hasLinkedAccount()) {
            try {
                // Use Dropbox datastores
                mDatastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
                // Hide link button
                mLinkButton.setVisibility(View.GONE);
            } catch (DbxException.Unauthorized e) {
                System.out.println("Account was unlinked remotely");
            }
        }
        if (mDatastoreManager == null) {
            // Account isn't linked yet, use local datastores
            mDatastoreManager = DbxDatastoreManager.localManager(mAccountManager);
            // Show link button
            mLinkButton.setVisibility(View.VISIBLE);
        }
    }

    /*
     * Checks to see if the user has set up the app before
     * If they have, reroute them to the login activity
     */
    private void checkSetUp(){

        DropboxManager manager = new DropboxManager(getApplicationContext());
        if(manager != null){
            Log.d("AAAAAAAAa", "null");
        }
        DbxRecord record = null;
        try{
            List<DbxRecord> results = manager.getTable("USER_LOGIN").query().asList();

            //If results > 0, the user has set up the app before
            if(results.size() != 0) {
                //Go to password input screen
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

        } catch(Exception e){
            e.printStackTrace();
        } finally{
            manager.close();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_initial_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //This code is used to link the app with the users Dropbox
    //The following code comes from https://www.dropbox.com/developers/datastore/tutorial/android
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                DbxAccount account = mAccountManager.getLinkedAccount();
                try {
                    // Migrate any local datastores to the linked account
                    mDatastoreManager.migrateToAccount(account);
                    // Now use Dropbox datastores
                    mDatastoreManager = DbxDatastoreManager.forAccount(account);

                    DbxDatastore datastore = mDatastoreManager.openDefaultDatastore();

                    //Wait for updates from server
                    while(datastore.getSyncStatus().isDownloading){
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    datastore.close();

                    //Relaunch to see if they have set up before
                    checkSetUp();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
                // recreate();
            } else {
                // Link failed or was cancelled by the user
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    /*
     * Launches the activity to set a password
     * extra SECURITY_LEVEL defines which setup to use in next activity
     *  0 for low, 1 for high
     *
     * @param   view    The view that initiates the method call
     */
    public void sendMessage(View view) {
        Intent intent;
        int level = 0;

        //Selet which activity to go to based on button press
        if (view.getId() == R.id.lowSecurityImageView) {
            intent = new Intent(this, CreateLowPasswordActivity.class);
        } else {
            intent = new Intent(this, CreateHighPasswordActivity.class);
            level = 1;
        }

        //Add the security level extra
        intent.putExtra("SECURITY_LEVEL", level);

        //Add the password as extra if the user is changing their password
        if (changePassword) {
            intent.putExtra("changePassword", changePassword);
            intent.putExtra("password", password);
        }

        startActivity(intent);
    }

    //in case app doesn't get destroyed so oncreate isn't called
    @Override
    protected void onResume() {
        super.onResume();
        checkSetUp();
        setUpDropbox();
    }
}
