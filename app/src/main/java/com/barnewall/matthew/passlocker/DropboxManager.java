package com.barnewall.matthew.passlocker;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxTable;

/**
 * A simplified way to access the dropbox datastores
 *
 * Created by Matthew on 4/4/2015.
 */
public class DropboxManager {
    private DbxAccountManager mAccountManager;
    private DbxDatastoreManager mDatastoreManager;
    private DbxDatastore datastore;

    /*
     * Sets up the datastore for the given context
     *
     * @param   applicationContext  The content of the application
     */
    public DropboxManager(Context applicationContext){

        // Set up the account manager
        mAccountManager = DbxAccountManager.getInstance(applicationContext, "oh8dsmwj9jp2es1", "b33vntcwrv9v1h1");

        // Set up the datastore manager
        if (mAccountManager.hasLinkedAccount()) {
            try {
                // Use Dropbox datastores
                mDatastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
            } catch (DbxException.Unauthorized e) {
                System.out.println("Account was unlinked remotely");
            }
        }
        if (mDatastoreManager == null) {
            // Account isn't linked yet, use local datastores
            mDatastoreManager = DbxDatastoreManager.localManager(mAccountManager);
        }
        try{
            datastore = mDatastoreManager.openDefaultDatastore();
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    /*
     * Returns the table from the datastore, or creates one if it does not exist
     *
     * @param   tableName   The name of the table to create or return
     * @return              The table in the datastore with the name tableName
     */
    public DbxTable getTable(String tableName){
        sync();
        return datastore.getTable(tableName);
    }

    /*
     * Calls the sync method on the datastore to make changes on disk and over the network.
     */
    public void sync(){
        try{
            datastore.sync();
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    /*
     * Calls the close method on the datastore if it is open.
     */
    public void close(){
        if(datastore.isOpen()) {
            datastore.close();
        }
    }

}
