package com.barnewall.matthew.passlocker;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.util.List;


public class DisplayInfoActivity extends ActionBarActivity{
    //Used for decrypting information from record
    private String      recordName;
    private String      password;

    //Stored here so can be deleted without querying again
    private DbxRecord   record;
    //Stored here so can be hidden/shown on display without having to decrypt each time
    private String      recordPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_info);

        //Get the record
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            recordName = extra.getString("recordName");
            password = extra.getString("password");
        }

        //Enable the up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Fill texts views with data
        fillTextViews();
    }

    /*
     * Fills the text views with data from the record
     */
    private void fillTextViews(){
        DropboxManager  manager = new DropboxManager(getApplicationContext());
        try{
            List<DbxRecord> results     = manager.getTable("ACCOUNT_INFO").query(new DbxFields().set("name", recordName)).asList();
            if(results.size() > 0){

                record          = results.get(0);

                //Set up encrypter
                DbxRecord       encrypterRecord = manager.getTable("USER_LOGIN").query().asList().get(0);
                Encrypter       encrypter       = new Encrypter(password, encrypterRecord.getBytes("Salt"),
                        encrypterRecord.getBytes("IV"));


                String          url             = encrypter.decrypt(record.getBytes("url"));
                recordPassword  = encrypter.decrypt(record.getBytes("password"));

                setTitle(recordName);

                //Get Views
                TextView    emailTextView       = ((TextView) findViewById(R.id.emailDatastoreValueTextView));
                TextView    usernameTextView    = ((TextView) findViewById(R.id.usernameDatastoreValueTextView));
                TextView    urlTextView         = ((TextView) findViewById(R.id.URLDatastoreValueTextView));
                TextView    passwordTextView    = ((TextView) findViewById(R.id.passwordDatastoreValueTextView));
                ImageView   iconImageView       = ((ImageView) findViewById(R.id.iconImageView));

                //Fill views with data
                emailTextView.setText(encrypter.decrypt(record.getBytes("email")));
                emailTextView.setMovementMethod(new ScrollingMovementMethod());
                usernameTextView.setText(encrypter.decrypt(record.getBytes("username")));
                usernameTextView.setMovementMethod(new ScrollingMovementMethod());

                //Make visible if there is a url
                if(url.equals("")){
                    findViewById(R.id.urlButton).setVisibility(View.GONE);
                }
                else{
                    findViewById(R.id.urlButton).setVisibility(View.VISIBLE);
                }
                urlTextView.setText(url);
                urlTextView.setMovementMethod(new ScrollingMovementMethod());

                passwordTextView.setText(starPassword(recordPassword));
                iconImageView.setImageBitmap(byteToBitmap(record.getBytes("icon")));
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            manager.close();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.delete_record) {
            deleteRecord();
            return true;
        }
        else if(id == R.id.edit_record){
            editRecord();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Deletes the record from the datastore
     */
    public void deleteRecord(){
        DropboxManager manager = new DropboxManager(getApplicationContext());
        record.deleteRecord();
        manager.sync();
        manager.close();
        finish();
    }

    /*
     * Launches the createNewActivity to edit the record
     */
    public void editRecord(){
        Intent intent = new Intent(this, CreateNewActivity.class);
        intent.putExtra("recordName", this.recordName);
        intent.putExtra("password", this.password);
        startActivity(intent);
    }

    /*
     * Converts a byte array to a bitmap
     *
     * @param   bytes   The array to be converted
     * @return          The converted bitmap
     */
    public Bitmap byteToBitmap(byte[] bytes){
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /*
     * Lauches a webbrowser activity of the url in the url textview
     *
     * @param view  The view that initiated the method call
     */
    public void openURL(View view){
        String  url     = ((TextView) findViewById(R.id.URLDatastoreValueTextView)).getText().toString();
        Intent  intent  = new Intent(Intent.ACTION_VIEW);
        if (!url.startsWith("https://") && !url.startsWith("http://")){
            url = "http://" + url;
        }
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    /*
     * Creates a string of * as long as the passed in string
     *
     * @param   pass    The word to base the length of the * string on
     * @return  stars   The string of *
     */
    private String starPassword(String pass){
        String stars = "";
        for(int i = 0; i < recordPassword.length(); i++){
            stars = stars + "*";
        }
        return stars;
    }

    /*
     * Toggles the visibility of the password to the user
     *
     * @param view  The view that initiated the method call
     */
    public void showPassword(View view){
        Button showButton = ((Button) findViewById(R.id.showButton));

        //Select what to do based on text of the button
        if(showButton.getText().toString().equals("Show Password")){
            ((TextView) findViewById(R.id.passwordDatastoreValueTextView)).setText(recordPassword);
            showButton.setText("Hide Password");
        }
        else{
            ((TextView) findViewById(R.id.passwordDatastoreValueTextView)).setText(starPassword(recordPassword));
            showButton.setText("Show Password");
        }
    }

    /*
     * Copies the password from the record to the users clipboard
     *
     * @param view  The view that initiated the method call
     */
    public void copyPassword(View view){

        //Copy password to clipboard
        ClipboardManager    clipboardManager    = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData            data                = ClipData.newPlainText("password", recordPassword);
        clipboardManager.setPrimaryClip(data);

        //Notify user password was copied
        Toast toast = Toast.makeText(this, "Password copied.", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onRestart(){
        super.onRestart();
        fillTextViews();
    }
}
