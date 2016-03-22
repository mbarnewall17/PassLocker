package com.barnewall.matthew.passlocker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainMenuActivity extends ActionBarActivity {
    private String password;
    private ArrayList<TextView> names;
    private boolean delete;
    static final int REQUEST_LINK_TO_DBX = 0;  // This value is up to you
    private DbxAccountManager mAccountManager;
    //private DbxDatastoreManager mDatastoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        delete = false;

        //Retrieve passed password to be able to encrypt data
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            password = extra.getString("password");
        }

        //Set up layout
        setContentView(R.layout.activity_main_menu);

        //Fill ListView with items in datastore
        fillListView();

        //Add listeners to the ListView
        setUpListeners();

        mAccountManager = DbxAccountManager.getInstance(getApplicationContext(), "oh8dsmwj9jp2es1", "b33vntcwrv9v1h1");
        //setUpDropbox();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //this.menu = menu;
        if(delete){
            getMenuInflater().inflate(R.menu.delete_menu, menu);
        }
        else if(mAccountManager.hasLinkedAccount()){
            getMenuInflater().inflate(R.menu.main_menu_activity_bar_no_dropbox, menu);
        }
        else{
            getMenuInflater().inflate(R.menu.main_menu_activity_bar, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_delete:
                delete();
                return true;
            case R.id.cancel:
                finishDelete();
                return true;
            case R.id.delete:
                deleteRecords(names);
                finishDelete();
                return true;
            case R.id.link_dropbox:
                linkDropbox();
                return true;
            case R.id.unlink_dropbox:
                unlinkDropbox();
                return true;
            case R.id.change_password:
                changePassword();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Launches initialSetup acitvity to change the users password
     */
    private void changePassword(){
        Intent intent = new Intent(this, InitialSetup.class);
        intent.putExtra("changePassword", true);
        intent.putExtra("password", password);
        startActivity(intent);
    }

    /*
     * Unlinks the users dropbox and starts the initial setup activity to resetup app
     */
    private void unlinkDropbox(){
        mAccountManager.getLinkedAccount().unlink();
        DropboxManager manager = new DropboxManager(getApplicationContext());
        manager.sync();
        manager.close();
        Intent intent = new Intent(this, InitialSetup.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /*
     * Code comes from dropbox datastore api tutorial
     * Migrates local datastores and uses linked datastore
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                DbxAccount account = mAccountManager.getLinkedAccount();
                try {
                    DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.localManager(mAccountManager);
                    mDatastoreManager.migrateToAccount(account);
                } catch (DbxException e) {
                    e.printStackTrace();
                }

                //Relogin
                Intent intent = new Intent(this,LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            } else {
                // Link failed or was cancelled by the user
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*
     * Warns the user of the risks of linking their dropbox and if they continue links their account
     */
    private void linkDropbox(){
        //Creates a dialog box to warn the user of the risks
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Before continuing: If PassLocker is already set up on another device," +
                " make sure your password is the same on both devices " +
                "or the the app may malfunction. ")
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        //Launch link dropbox activity
                        mAccountManager.startLink((Activity) MainMenuActivity.this, REQUEST_LINK_TO_DBX);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
     * Sets up the delete UI for a delete and changes the value of delete
     */
    private void delete(){
        this.delete = true;
        invalidateOptionsMenu();
        names = new ArrayList<TextView>();

    }

    /*
     * Sets the UI back to normal after deleting records or canceling a delete
     */
    private void finishDelete(){

        //Change delete's value and change the action bar
        this.delete = false;
        invalidateOptionsMenu();

        //This is done incase the user cancels the delete
        for(TextView t : names){
            t.setBackgroundColor(Color.parseColor("#ffefefef"));
        }
        names = null;

        //Reset the UI
        setTitle("Pass Locker");
        fillListView();
    }

    /*
     * Deletes all the records that are associated with the TextViews in the name arraylist
     *
     * @param   names   An arraylist of textViews that are to be deleted form the datastore
     */
    private void deleteRecords(ArrayList<TextView> names) {
        DropboxManager manager = new DropboxManager(getApplicationContext());

        try {
            DbxTable table = manager.getTable("ACCOUNT_INFO");
            List<DbxRecord> results;

            //Delete each record associated with a textView in names
            for(TextView tV : names){
                results = table.query(new DbxFields().set("name", tV.getText().toString())).asList();
                results.get(0).deleteRecord();
            }

            manager.sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.close();
        }
    }

    /*
     * Launch the CreateNewActivity to create a new record
     *
     * @param   view    The view that initiated the method call
     */
    public void createNew(View view){
        Intent intent = new Intent(this, CreateNewActivity.class);

        //Pass password to new activity to be able to encrypt without user having to re-enter
        //their password.
        intent.putExtra("password", password);
        startActivity(intent);
    }

    /*
     *Fills in the listView with the names of the different records in the ACCOUNT_INFO table
     */
    private void fillListView(){
        DropboxManager  manager     = new DropboxManager(getApplicationContext());

        try{
            List<DbxRecord>                 results = manager.getTable("ACCOUNT_INFO").query().asList();
            ArrayList<IconListViewItem>     names   = new ArrayList<IconListViewItem>();

            //Create an IconListViewItem for each record in the ACCOUNT_INFO table and add it to an arraylist
            if(results.size() > 0){
                DbxRecord record;
                for(int i = 0; i < results.size(); i++){
                    record  = results.get(i);
                    names.add(new IconListViewItem(byteToBitmap(record.getBytes("icon")), record.getString("name")));
                }
            }

            //Sort the records by name
            Collections.sort(names);

            //Add the adapter to the listview
            IconListViewAdapter adapter = new IconListViewAdapter(this, R.layout.my_listview, names);
            ((ListView) findViewById(R.id.identifierListView)).setAdapter(adapter);

        } catch(Exception e){
            e.printStackTrace();
        } finally {
            manager.close();
        }
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
     * Reloads the elements of the list view
     */
    @Override
    public void onRestart(){
        super.onRestart();
        fillListView();
    }

    /*
     * Sets up the listeners on the listView
     * Long press will edit the record
     * If the user is not deleting, a short press will display the record
     * If the user is deleting, a short press will change the background color of the
     * item and put the item in an arraylist of records to be deleted
     */
    private void setUpListeners(){
        ((ListView) findViewById(R.id.identifierListView)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = ((TextView) view.findViewById(R.id.textViewInListView));

                //If delete is true, the user is deleting records
                // Add record to list to be deleted
                if (delete) {

                    //If item is not in the arraylist of records to be deleted, change its color and add it
                    if (names.indexOf(textView) == -1) {
                        textView.setBackgroundColor(Color.parseColor("#ff82b6ff"));
                        names.add(textView);
                    }
                    //If the item is in the arraylist, remove it from the arraylist and change its color back
                    else {
                        names.remove(textView);
                        textView.setBackgroundColor(Color.parseColor("#ffefefef"));
                    }

                    //Change the title
                    final int checkedCount = names.size();
                    setTitle(checkedCount + " Selected");

                }
                //Show selected record
                else {
                    Intent intent = new Intent(view.getContext(), DisplayInfoActivity.class);
                    intent.putExtra("recordName", textView.getText().toString());
                    intent.putExtra("password", password);
                    startActivity(intent);
                }

            }
        });

        //If the user long presses on an item, load that record to be edited
        ((ListView) findViewById(R.id.identifierListView)).setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = ((TextView) view.findViewById(R.id.textViewInListView));
                String recordString = textView.getText().toString();
                Intent intent = new Intent(view.getContext(), CreateNewActivity.class);
                intent.putExtra("recordName", recordString);
                intent.putExtra("password", password);
                startActivity(intent);
                return true;
            }
        });
    }

//    /*
//     * This following code is used to link the app with the users Dropbox account
//     * Code Comes From https://www.dropbox.com/developers/datastore/tutorial/android
//     */
//    private void setUpDropbox(){
//        // Set up the account manager
//        mAccountManager = DbxAccountManager.getInstance(getApplicationContext(), "oh8dsmwj9jp2es1", "b33vntcwrv9v1h1");
//
//
//    }
}
