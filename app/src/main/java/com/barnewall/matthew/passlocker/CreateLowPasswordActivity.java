package com.barnewall.matthew.passlocker;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.util.List;


public class CreateLowPasswordActivity extends ActionBarActivity {
    private String  initialPassword;

    //Used to change the users password
    private String  password;
    private boolean changePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hides the action bar
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        //Sets up variables if changing password
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            password        = extra.getString("password");
            changePassword  = extra.getBoolean("changePassword");
        }

        setContentView(R.layout.activity_create_low_password);
    }

    //Not used
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_low_password, menu);
        return true;
    }

    //Not used
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

    /*
     * Checks to make sure the password's length > 10
     * Will make the user enter the password twice for accuracy
     *
     * @param view  The view that initiates the method call
     */
    public void submitPassword(View view){
        //Create variables
        EditText passwordEditText = (EditText) findViewById(R.id.lowPasswordEditText);
        String   password         = passwordEditText.getText().toString();

        //First time the method is called
        //Check the passwords length, if it is not long enough it creates a toast and alerts the user
        //If it is long enough, it informs the user they must reenter their password to check for accuracy
        if(initialPassword == null) {
            if (password.length() < 10) {
                Toast toast = Toast.makeText(this, "Password must be longer than 10 characters", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                initialPassword = password;
                ((TextView) findViewById(R.id.enterPasswordTextView)).setText("Please Re-enter Your Password");
                passwordEditText.setText("");
            }
        }

        //Second time the method is called
        else{
            //Checks to see that the password is the same as the first time
            if(password.equals(initialPassword)){

                //If the user is changing a password, covertOldRecords will deal with modify the datastore.
                if(changePassword){
                    convertOldRecords(password);
                }
                else{
                    //Adds password information into the dropbox datastore so that the user will be able
                    //to login in with their password. The Salt, IV, and an encrypted version of the
                    //word password will be stored. The Salt and the IV are necessary so that the
                    //encrypter will be able to work the same every time as long as it is provided those
                    //as parameters. The encrypted version of the word password will be used to verify
                    //that the correct password has been provided
                    clear(view);
                    //Create variables
                    DropboxManager  manager     = new DropboxManager(getApplicationContext());
                    DbxTable        table       = manager.getTable("USER_LOGIN");
                    Encrypter       encrypter   = new Encrypter(password);

                    //Insert into datastore
                    table.insert().set("IV", encrypter.getIV()).set("Salt", encrypter.getSalt())
                            .set("Hash", encrypter.encrypt(password)).set("Level", "0");

                    //Update datastore
                    manager.sync();
                    manager.close();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

            }
            else{
                Toast toast = Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    /*
     * Clears the USER_LOGIN table of any old records
     *
     * @param   view    The view that instantiated the method call
     */
    public void clear(View view){
        DropboxManager  manager = new DropboxManager(getApplicationContext());
        try{
            //Get all records in the USER_LOGIN table
            List<DbxRecord> results = manager.getTable("USER_LOGIN").query().asList();

            //Delete every record
            for(DbxRecord r : results){
                r.deleteRecord();
            }

            //Update datastore
            manager.sync();
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            manager.close();
        }
    }

    /*
     * Modifies the USER_LOGIN tables record to match the new password.
     * Decrypts all the older ACCOUNT_INFO records using the old password
     * and reencrypts them using the new password.
     *
     * @param   newPassword     The new password to encrypt the data with
     */
    private void convertOldRecords(String newPassword){
        DropboxManager  manager         = new DropboxManager(getApplicationContext());
        try {
            //Get old USER_LOGIN record an encrypter object can be created with its data and then updated
            //with the new data.
            DbxRecord       encrypterRecord = manager.getTable("USER_LOGIN").query().asList().get(0);
            Encrypter       oldEncrypter    = new Encrypter(password, encrypterRecord.getBytes("Salt"),
                                                encrypterRecord.getBytes("IV"));
            //Create a new encrypter based on the new password
            Encrypter       newEncrypter    = new Encrypter(newPassword);

            //Get all the records in the ACCOUNT_INFO table so they can be converted
            List<DbxRecord> records         = manager.getTable("ACCOUNT_INFO").query().asList();

            //Modify the USER_LOGIN record with the new data
            encrypterRecord.set("IV", newEncrypter.getIV()).set("Salt", newEncrypter.getSalt())
                    .set("Hash", newEncrypter.encrypt(newPassword)).set("Level", "0");

            //Modify each record so that it is decrypted with the old encrypter and reencrypted with the new encrypter
            for(DbxRecord r : records){
                r.set("name", r.getString("name"))
                        .set("email", newEncrypter.encrypt(oldEncrypter.decrypt(r.getBytes("email"))))
                        .set("username", newEncrypter.encrypt(oldEncrypter.decrypt(r.getBytes("username"))))
                        .set("url", newEncrypter.encrypt(oldEncrypter.decrypt(r.getBytes("url"))))
                        .set("password", newEncrypter.encrypt(oldEncrypter.decrypt(r.getBytes("password"))))
                        .set("icon", r.getBytes("icon"));
            }

            //Update the datastore
            manager.sync();
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            manager.close();
        }
    }
}
