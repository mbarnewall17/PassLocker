package com.barnewall.matthew.passlocker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class CreateNewActivity extends ActionBarActivity {
    private String password;                        //Password to encrypt data with
    private static final int SELECT_PHOTO = 100;    //Photo size
    private Bitmap imageBitmap;                     //Bypass need to covert from Drawable
    private DbxRecord record;                       //Record to be edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //convert resource to bitmap to prevent need to convert Drawable
        imageBitmap= BitmapFactory.decodeResource(getResources(), R.drawable.no_image);

        //Get extras
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            //Get password to encrypt data with
            password = extra.getString("password");

            //Get record to edit
            if(extra.getString("recordName") != null){
                try{

                   DropboxManager manager = new DropboxManager(getApplicationContext());
                   record = manager.getTable("ACCOUNT_INFO").query(new DbxFields().set("name",
                           extra.getString("recordName"))).asList().get(0);
                   manager.close();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        }

        //Set the up button as function
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_create_new);

        //Set values to be edited
        if(record != null){
            fillValuesFromRecord(record);
        }
    }

    /*
     * Fill the editTexts with the values from the record so they can be edited
     *
     * @param record    The record to be edited, from which the values will come
     */
    private void fillValuesFromRecord(DbxRecord record){
        try{
            DropboxManager  manager = new DropboxManager(getApplicationContext());
            DbxRecord       encrypterRecord = manager.getTable("USER_LOGIN").query().asList().get(0);
            Encrypter       encrypter       = new Encrypter(password, encrypterRecord.getBytes("Salt"),
                    encrypterRecord.getBytes("IV"));


            //Load Values into views
            ((EditText) findViewById(R.id.nameEditText)).setText(record.getString("name"));
            ((EditText) findViewById(R.id.emailEditText)).setText(encrypter.decrypt(record.getBytes("email")));
            ((EditText) findViewById(R.id.usernameEditText)).setText(encrypter.decrypt(record.getBytes("username")));
            ((EditText) findViewById(R.id.URLEditText)).setText(encrypter.decrypt(record.getBytes("url")));
            ((EditText) findViewById(R.id.passwordEditText)).setText(encrypter.decrypt(record.getBytes("password")));
            imageBitmap = byteToBitmap(record.getBytes("icon"));
            ((ImageButton) findViewById(R.id.iconImageButton)).setImageBitmap(imageBitmap);

            setTitle("Edit Record");
            manager.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_new, menu);
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

    /*
     * Saves the users info in the ACCOUNT_INFO table in a new record if the user is not editing a record.
     * If the user is editing a record, the record will simply be modified.
     *
     * @param   view    The view that initiated the method call
     */
    public void submit(View view){
        //Create variables
        String          name                = ((EditText) findViewById(R.id.nameEditText)).getText().toString();
        String          email               = ((EditText) findViewById(R.id.emailEditText)).getText().toString();
        String          username            = ((EditText) findViewById(R.id.usernameEditText)).getText().toString();
        String          recordPassword      = ((EditText) findViewById(R.id.passwordEditText)).getText().toString();

        //Name must have a value, and either an email or username must be entered, and password
        //must be entered to be able to create a record
        if(name.isEmpty()) {
            Toast toast = Toast.makeText(this, "You must enter a name for this record", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if(email.isEmpty() && username.isEmpty()) {
            Toast toast = Toast.makeText(this, "You must enter an e-mail or username", Toast.LENGTH_SHORT);
            toast.show();
        }
        else if(recordPassword.isEmpty()) {
            Toast toast = Toast.makeText(this, "You must enter a password", Toast.LENGTH_SHORT);
            toast.show();
        }
        else{
            //Create the other variables, done here for performance
            String          url                 = ((EditText) findViewById(R.id.URLEditText)).getText().toString();
            byte[]          icon                = bitmapToByte(imageBitmap);
            DropboxManager  manager             = new DropboxManager(getApplicationContext());
            DbxTable        accountInfoTable    = manager.getTable("ACCOUNT_INFO");

            Encrypter encrypter = null;
            try{
                //Get the USER_LOGIN record to create the encrypter object using the correct IV and salt
                DbxTable.QueryResult results = manager.getTable("USER_LOGIN").query();
                DbxRecord record = results.iterator().next();
                byte[] salt = record.getBytes("Salt");
                byte[] iv = record.getBytes("IV");
                encrypter = new Encrypter(password, salt, iv);
            } catch(Exception e){
                e.printStackTrace();
            }

            //If record is null, this is a new record not an edit so a new record must be created
            if(record == null){
                accountInfoTable.insert().set("name", name).set("email", encrypter.encrypt(email))
                        .set("username", encrypter.encrypt(username)).set("url", encrypter.encrypt(url))
                        .set("password", encrypter.encrypt(recordPassword)).set("icon", icon);
            }
            //Otherwise, this is an edit to a previous record and should just replace the old data
            else{
                record.set("name", name).set("email", encrypter.encrypt(email))
                      .set("username", encrypter.encrypt(username)).set("url", encrypter.encrypt(url))
                      .set("password", encrypter.encrypt(recordPassword)).set("icon", icon);
            }

            //Update and close Datastore
            manager.sync();
            manager.close();

            //Finish this activity
            this.finish();
        }
    }

    //Launch the Image picker activity
    public void selectImage(View view){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case SELECT_PHOTO:
                    if (resultCode == RESULT_OK) {

                        //Save bitmap from selected photo and set the button to that image
                        Uri selectedImage = data.getData();
                        Bitmap yourSelectedImage = decodeUri(selectedImage);
                        imageBitmap = yourSelectedImage;
                        ((ImageButton)findViewById(R.id.iconImageButton)).setImageBitmap(yourSelectedImage);
                    }
            }
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    /*
     * Method used to convert data from image selection activity into a Bitmap
     * Code comes from http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app
     *
     * @param   selectedImage   The uri of the image to be converted
     * @return                  The converted bitmap
     */
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 100;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }

    /*
     * Converts a bitmap to a byte array
     *
     * @param   image       The bitmap to be converted
     * return   byteArray   The converted byteArray
     */
    public byte[] bitmapToByte(Bitmap image){
        try{
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return byteArray;
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
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

}
