package com.example.nfcreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity implements NfcAdapter.ReaderCallback {
    private NfcAdapter mNfcAdapter;
    private volatile List<String> mUrls;
    ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mUrls = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                mUrls);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // NFCアダプタをType Aのリーダモードに変更する
        // これによって、システムが行うLLCPを抑制する
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (mNfcAdapter != null) {
            mNfcAdapter.setNdefPushMessage(null, this);
            mNfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // リーダモードを解除する
        mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.clear_settings:
            // 一覧表示をリセットする
            mUrls.clear();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * NFC
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        NdefMessage message;

        if (ndef != null) {
            message = ndef.getCachedNdefMessage();
        } else {
            return;
        }

        mUrls.clear();

        if (message != null) {
            digUrisInMessage(message);
        }

        // URL一覧の変更をListViewに通知する
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    // NDEF MessageからURLを含むレコードを再帰的に探索し、mUriに登録する
    private void digUrisInMessage(NdefMessage message) {
        NdefRecord[] records = null;
        if (message != null) {
            records = message.getRecords();
        }

        if (records == null || records.length == 0) {
            return;
        }

        for (NdefRecord record : records) {
            digUrisInRecord(record);
        }
    }

    // NDEF RecordからURLを含むレコードを再帰的に探索し、mUriに登録する
    private void digUrisInRecord(NdefRecord record) {
        short tnf = record.getTnf();
        byte[] type = record.getType();

        if (tnf == NdefRecord.TNF_ABSOLUTE_URI ||
                tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(NdefRecord.RTD_URI, type) ||
                tnf == NdefRecord.TNF_EXTERNAL_TYPE) {
            Uri uri = record.toUri();
            if (uri != null) {
                mUrls.add(uri.toString());
            }
        } else if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(NdefRecord.RTD_SMART_POSTER, type)) {
            byte[] payload = record.getPayload();
            NdefMessage message;
            try {
                message = new NdefMessage(payload);
            } catch (FormatException e) {
                e.printStackTrace();
                return;
            }

            digUrisInMessage(message);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        // URIに対応するアプリを起動する
        Uri uri = Uri.parse(mUrls.get(position));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}