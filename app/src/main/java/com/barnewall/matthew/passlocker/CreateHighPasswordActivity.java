package com.barnewall.matthew.passlocker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/*
 * This activity is used to either created a new password or change their current password.
 * The user will select a question from the spinner and then answer the question.
 * The answered questions will be used to create a password that will allow them to log into
 * the app, either by entering that password, or reanswering the questions.
 */
public class CreateHighPasswordActivity extends ActionBarActivity {

    //Used for creating password when finished answering questions
    private String[]            answersToQuestions;
    private ArrayList<String>   list;

    //Used for the back button
    private Stack<String>       previousQuestions;
    private Stack<Integer>      previousQuestionsNumbers;

    private String              password;                   //Used to decrypt old password records
    private boolean             changePassword;             //Used to tell if inital setup of app or changing previous password


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

        setContentView(R.layout.activity_create_high_password);

        //Instantiate Variables
        previousQuestions           = new Stack<String>();
        previousQuestionsNumbers    = new Stack<Integer>();

        //Set up spinner with questions
        loadQuestions();

        //Set up editText onKeyListener, pressing enter key will trigger nextQuestion
        findViewById(R.id.answerEditText).setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() != KeyEvent.ACTION_DOWN){
                    return false;
                }
                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    nextQuestion(findViewById(R.id.answerEditText));
                    ((EditText) findViewById(R.id.answerEditText)).setText("");
                }
                return false;
            }
        });
    }



    //Not used
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_high_password, menu);
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
     * If the user has answered the question, the answer is saved to answersToQuestions array,
     * the questions is removed from the spinner and saved.
     * The answer editText is cleared and the spinner is updated.
     * checkFinish is called to see if the user has meet the minimum requirements for a password
     * to be created.
     * If the user has not answered the questions, a toast will be created to inform them
     *
     * @param view The view that initiated the method call
     */
    public void nextQuestion(View view){
        //Create variables
        Spinner     spinner         = (Spinner) findViewById(R.id.questionSpinner);
        EditText    answerText      = (EditText) findViewById(R.id.answerEditText);
        String      question        = spinner.getSelectedItem().toString();
        int         questionNumber  = list.indexOf(question);

        //Checks to make sure the user entered an answer
        //If the answer has been left black, a toast will notify them to enter an answer first
        if(answerText.length() == 0){
            Toast toast = Toast.makeText(this, "You must answer the question", Toast.LENGTH_SHORT);
            toast.show();
        }
        else{
            //Save the users answer
            answersToQuestions[questionNumber] = answerText.getText().toString();

            //Remove the question from appearing in the spinner, but save in a stack for the back button
            previousQuestions.push(list.get(questionNumber));
            list.set(questionNumber, null);
            previousQuestionsNumbers.push(questionNumber);

            //Update UI
            updateSpinner(spinner);
            answerText.setText("");

            checkFinish();
        }

    }

    /*
     *If the user has already answered a question, that question will be added back into the spinner
     * and the users answer will be removed.
     *
     * @param view The view that initiated the method call
     */
    public void onBackPressed(View view){
        if(!previousQuestions.empty()){
            //Create variables
            Spinner     spinner         = (Spinner) findViewById(R.id.questionSpinner);
            EditText    answerText      = (EditText) findViewById(R.id.answerEditText);
            int         questionNumber  = previousQuestionsNumbers.pop();

            //Add question back into the spinner, and remove their answer
            list.add(questionNumber, previousQuestions.pop());
            answersToQuestions[questionNumber] = null;

            //Update UI
            updateSpinner(spinner);
            answerText.setText("");

            checkFinish();
        }
    }

    /*
     * Refreshes the spinners elements with the questions in list.
     * Each element in list that is not null will be added to the spinner
     * A null element indicates the question was already answered
     *
     * @param spinner The spinner to be updated
     */
    private void updateSpinner(Spinner spinner){
        ArrayList<String> noNulls = new ArrayList<String>();
        for(String s : list){
            if(s != null){
                noNulls.add(s);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.my_spinner, noNulls);
        spinner.setAdapter(adapter);
    }

    /*
     *Loads the questions from securityQuestions.txt into the spinner
     */
    private void loadQuestions(){
        //Create Variables
        Spinner spinner = (Spinner) findViewById(R.id.questionSpinner);
                list    = new ArrayList<String>();

        //Set Spinner Prompt
        spinner.setPrompt("Select A Question");

        //Read in questions and add the list
        try{
            //Create variables for reading in text file
            InputStream         inputStream     = getAssets().open("securityQuestions.txt");
            InputStreamReader   reader          = new InputStreamReader(inputStream);
            BufferedReader      bufferedReader  = new BufferedReader(reader);
            String              line;

            //Read in every line
            int count = 0;  //Used to count the number of questions
            while((line = bufferedReader.readLine()) != null){
                list.add(line);
                count ++;
            }

            //Create array for answers to questions
            answersToQuestions = new String[count];
        }
        catch(Exception e){
            e.printStackTrace();
        }

        //Update UI
        updateSpinner(spinner);
    }

    /*
     * Called when the user presses the finish button.
     * First a password is created from the questions the user answered.
     * If this is an initial setup password, then a record will be added
     * to the USER_LOGIN datastore containing the IV, salt, and encrypted bytes of their password.
     * If the user is changing a password, covertOldRecords will deal with modify the datastore.
     * A dialogue box will be created informing the user of what their master password is.
     *
     * @param view The view that initiated the method call
     */
    public void finishQuestions(View view){

        String password = "";
        for(String answer : answersToQuestions){
            if(answer != null){
                password = password + selectCharacter(answer);
            }
        }

        //Adds password information into the dropbox datastore so that the user will be able
        //to login in with their password. The Salt, IV, and an encrypted version of the
        //word password will be stored. The Salt and the IV are necessary so that the
        //encrypter will be able to work the same every time as long as it is provided those
        //as parameters. The encrypted version of the word password will be used to verify
        //that the correct password has been provided

        if(changePassword){
            convertOldRecords(password);
        }
        else{
            clear(view);
            //Create variables
            DropboxManager  manager         = new DropboxManager(getApplicationContext());
            DbxTable        table           = manager.getTable("USER_LOGIN");
            Encrypter       encrypter       = new Encrypter(password);
            String          questionsString = stackToString(previousQuestionsNumbers);

            //Insert into datastore
            table.insert().set("IV", encrypter.getIV()).set("Salt", encrypter.getSalt())
                    .set("Hash", encrypter.encrypt(password)).set("Level", "1").set("Questions", questionsString);

            //Update datastore
            manager.sync();
            manager.close();
            //Create a dialog to show the user their password
        }

        //Create a dialog box to inform the user of their password
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your password is: " + password)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                raiseIntent();
            }});
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
     * Converts stack of integers to a comma separated string of integers
     * in order to be stored in the dropbox datastore
     *
     * @param   stack           The stack to be converted into a string
     * @return  returnString    The comma separated string of the stack
     */
    private String stackToString(Stack<Integer> stack){

        //Reverse order of stack
        Stack<Integer> questions = new Stack<Integer>();
        while(!stack.isEmpty()){
            questions.push(stack.pop());
        }

        //Add elements of stack to a string, comma seperated
        String returnString = Integer.toString(questions.pop());
        while(!questions.isEmpty()){
            returnString = returnString + "," + questions.pop();
        }
        return returnString;
    }

    /*
     * Launches LoginActivity as the top of the stack of activities
     */
    private void raiseIntent(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /*
     * Selects a "random" character from the passed in string
     * Creates a total of the value of all characters in the string and
     * return the character at the total modded by the length of the word
     *
     * @param   word    The word to select a "random" character from
     * @return          The "random" character from the word.
     */
    private String selectCharacter(String word){
        int total = 0;
        for(int i = 0; i < word.length(); i++){
            total = total + String.valueOf(word.charAt(i)).codePointAt(0);
        }
        return word.substring(total % word.length(), total % word.length() + 1);
    }

    /*
     * Check if the user has answered enough questions.
     * If they have, enable the finishButton
     * Else disable the finishButton
     */
    private void checkFinish() {
        //Checks if user has answered enough questions. If they have enable the finish button
        if (previousQuestions.size() >= 10) {
            ((Button) findViewById(R.id.finishButton)).setEnabled(true);
        } else {
            ((Button) findViewById(R.id.finishButton)).setEnabled(false);
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
            String          questionsString = stackToString(previousQuestionsNumbers);

            //Modify the USER_LOGIN record with the new data
            encrypterRecord.set("IV", newEncrypter.getIV()).set("Salt", newEncrypter.getSalt())
                    .set("Hash", newEncrypter.encrypt(newPassword)).set("Level", "1").set("Questions", questionsString);

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
