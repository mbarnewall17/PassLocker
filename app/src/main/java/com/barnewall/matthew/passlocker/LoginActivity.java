package com.barnewall.matthew.passlocker;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.util.Arrays;
import java.util.List;


public class LoginActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hides the action bar
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_login);

        setUI();

    }

    /*
     * Sets up the UI depending on the level of security chosen when setting up a password
     */
    private void setUI(){
        DropboxManager  manager = new DropboxManager(getApplicationContext());
        DbxRecord       record  = null;
        ImageView       image   = (ImageView) findViewById(R.id.securityLevelImageView);

        //Get security level from datastore. 0 for low, 1 for high
        try{
            DbxTable.QueryResult results = manager.getTable("USER_LOGIN").query();
            record = results.iterator().next();

            //Set image based on security level
            if(record.getString("Level").equals("0")){
                image.setImageResource(R.drawable.lowlock);
            }
            else{
                image.setImageResource(R.drawable.highsecurity);

                //Enable ability to sign in with questions
                findViewById(R.id.questionTextView).setVisibility(View.VISIBLE);
            }

        } catch(Exception e){
            e.printStackTrace();
            image.setImageResource(R.drawable.lowlock);
        } finally{
            manager.close();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //To delete
    public void clear(View view){
        DropboxManager  manager = new DropboxManager(getApplicationContext());
        try{
            List<DbxRecord> results = manager.getTable("USER_LOGIN").query().asList();
            for(DbxRecord r : results){
                r.deleteRecord();
            }
            results = manager.getTable("ACCOUNT_INFO").query().asList();
            for(DbxRecord r : results){
                r.deleteRecord();
            }

            manager.sync();
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            manager.close();
        }

    }

    /*
     * Checks if the input password matches the data in the datastore
     * If it does, let the user into the app
     * Otherwise notify the user their password is incorrect
     *
     * @param   view    The view that initiated the method call
     */
    public void submit(View view){
        //Set up variables
        DropboxManager  manager         = new DropboxManager(getApplicationContext());
        String          password        = ((EditText) findViewById(R.id.passwordEditText)).getText().toString();
        Encrypter       encrypter;

        try{
            DbxRecord   record      = manager.getTable("USER_LOGIN").query().iterator().next();
                        encrypter   = new Encrypter(password, record.getBytes("Salt"), record.getBytes("IV"));

            //If byte array of encrypted password and byte array of encrypted password in datastore match
            //then the passwords are the same and launch the main acitivity
            if(Arrays.equals(record.getBytes("Hash"), encrypter.encrypt(password))){
                Intent intent = new Intent(this, MainMenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                //Pass password to new activity to be able to encrypt without user having to re-enter
                //their password.
                intent.putExtra("password", password);
                startActivity(intent);
            }
            //Alert the user their password is incorrect
            else{
                Toast toast = Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT);
                toast.show();
            }

        } catch(Exception e){
            e.printStackTrace();
        } finally {
            manager.close();
        }
    }

    /*
     * Launches the loginByQuestion activity as means of logging into the app
     *
     * @param   view    The view that initiates the method call
     */
    public void loginByQuestion(View view){
        Intent intent = new Intent(this, QuestionLoginActivity.class);
        startActivity(intent);
    }

}
