package com.barnewall.matthew.passlocker;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;


public class QuestionLoginActivity extends ActionBarActivity {
    private String[]            answersToQuestions; //The users answers to the questions
    private ArrayList<String>   list;               //The questions the user has to answer
    private int                 index;              //Used to indicate which question the user is on


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hides the action bar
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_question_login);

        //Load questions that the user answered when setting up their password
        loadQuestions();

        //Load the first question
        loadQuestion();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_question_login, menu);
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
     * Retrieves the questions numbers that the user answered when they set up their password
     * and loads them for the user to answer
     */
    private void loadQuestions(){
        //Create Variables
        list    = new ArrayList<String>();

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


            DropboxManager      manager         = new DropboxManager(getApplicationContext());
            Integer[]           questionNumbers = parseString(manager.getTable("USER_LOGIN").query().asList().get(0).getString("Questions"));
            ArrayList<String>   actualQuestions = new ArrayList<String>();
            manager.close();

            //Add the questions to be answered into an arraylist
            for(int i : questionNumbers){
                actualQuestions.add(list.get(i));
            }


            list = actualQuestions;
            //Create array for answers to questions
            answersToQuestions = new String[list.size()];
            index = 0;

        }
        catch(Exception e){
           e.printStackTrace();
        }
    }

    /*
     * Set the questionTextView's text to the question the user needs to answer
     * Set the answerEditText's text to the answer the user gave
     */
    private void loadQuestion(){
        TextView textView       = (TextView) findViewById(R.id.questionTextView);
        EditText answerEditText = (EditText) findViewById(R.id.answerEditText);

        //Set the question
        textView.setText(list.get(index));

        //Set the answer if the answer isn't null
        if(answersToQuestions[index] != null){
            answerEditText.setText(answersToQuestions[index]);
        }
        else{
            answerEditText.setText("");
        }
    }

    /*
     * Converts a comma seperated string into an array of ints
     *
     * @param   toParse     The comma separated string
     * @return  array       The array of ints
     */
    private Integer[] parseString(String toParse){
        //Create a string array of the ints
        String[] stringVersion = toParse.split(",");

        Integer[] array = new Integer[stringVersion.length];

        //Convert the string array into an array of ints
        for(int i = 0; i < stringVersion.length; i++){
            array[i] = Integer.parseInt(stringVersion[i]);
        }
        return array;
    }

    /*
     * Saves the users answer and loads the next question.
     * If there is not another question, checks to see if the user answered all the questions correct
     * Alert the user if the they didn't answer the question or if their answers don't match
     */
    public void nextQuestion(View view){
        EditText    answerText      = (EditText) findViewById(R.id.answerEditText);

        //Checks to make sure the user entered an answer
        //If the answer has been left black, a toast will notify them to enter an answer first
        if(answerText.length() == 0){
            Toast toast = Toast.makeText(this, "You must answer the question", Toast.LENGTH_SHORT);
            toast.show();
        }
        else{
            //Save the users answer
            answersToQuestions[index] = answerText.getText().toString();
            answerText.setText("");

            //Load the next question
            if(index < answersToQuestions.length - 1){
                index = index + 1;
                loadQuestion();
            }

            //Check to see if the password created from answering the questions match the one in the datastore
            else {
                verifyPassword();
            }
        }
    }

    /*
     * Checks if the input password matches the data in the datastore
     * If it does, let the user into the app
     * Otherwise notify the user their password is incorrect
     */
    private void verifyPassword(){

        //Construct password from answered questions
        String password = "";
        for(String answer : answersToQuestions){
            if(answer != null){
                password = password + selectCharacter(answer);
            }
        }

        //Set up variables
        DropboxManager  manager         = new DropboxManager(getApplicationContext());
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
     * Decrease index if possible, and load that question
     *
     * @param   view    The view that initiated the method call
     */
    public void onBackPressed(View view){
        if(index != 0){
            index = index - 1;
            loadQuestion();
        }
    }
}
