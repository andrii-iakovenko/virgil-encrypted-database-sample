package com.virgilsecurity.encrypteddatabase;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.virgilsecurity.sdk.crypto.CryptoHelper;
import com.virgilsecurity.sdk.crypto.KeyPair;
import com.virgilsecurity.sdk.crypto.KeyPairGenerator;
import com.virgilsecurity.sdk.crypto.PrivateKey;
import com.virgilsecurity.sdk.crypto.PublicKey;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String PREF_PUBLIC_KEY = "public_key";
    private static final String PREF_PRIVATE_KEY = "private_key";
    private static final String RECIPIENT_ID = "my_recipient_id";

    private DbHelper mDbHelper;

    private EditText mMessage;
    private EditText mRecordId;

    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessage = (EditText) findViewById(R.id.message);
        mRecordId = (EditText) findViewById(R.id.recordId);

        mDbHelper = new DbHelper(getBaseContext());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains(PREF_PUBLIC_KEY)) {
            // Load keys from preferences
            mPublicKey = new PublicKey(prefs.getString(PREF_PUBLIC_KEY, ""));
            mPrivateKey = new PrivateKey(prefs.getString(PREF_PRIVATE_KEY, ""));
        } else {
            // Generate keys
            KeyPair keyPair = KeyPairGenerator.generate();
            mPublicKey = keyPair.getPublic();
            mPrivateKey = keyPair.getPrivate();

            prefs.edit()
                    .putString(PREF_PUBLIC_KEY, mPublicKey.getAsString())
                    .putString(PREF_PRIVATE_KEY, mPrivateKey.getAsString())
                    .commit();
        }
    }

    public void save(View view) {
        String message = mMessage.getText().toString();
        try {
            String encodedMessage = CryptoHelper.encrypt(message, RECIPIENT_ID, mPublicKey);

            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(MessageEntry.COLUMN_NAME_MESSAGE, encodedMessage);

            long newRowId;
            newRowId = database.insert(MessageEntry.TABLE_NAME, null, values);

            mRecordId.setText(String.valueOf(newRowId));
            clearText();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void read(View view) {
        String rowId = mRecordId.getText().toString();

        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String[] projection = {
                MessageEntry.COLUMN_NAME_MESSAGE
        };

        Cursor cursor = database.query(
                MessageEntry.TABLE_NAME,
                projection,
                MessageEntry._ID + " = ?",
                new String[] {rowId},
                null, null, null
        );

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();


            String encodedMessage = cursor.getString(cursor.getColumnIndex(MessageEntry.COLUMN_NAME_MESSAGE));
            String message = null;
            try {
                message = CryptoHelper.decrypt(encodedMessage, RECIPIENT_ID, mPrivateKey);
                mMessage.setText(message);
            } catch (Exception e) {
                clearText();
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            clearText();
        }
    }

    private void clearText() {
        mMessage.setText("");
    }
}
