package com.example.root.akuvo;


        import android.Manifest;
        import android.content.ClipData;
        import android.content.ClipboardManager;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.Environment;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.AlertDialog;
        import android.support.v7.app.AppCompatActivity;
        import android.text.TextUtils;
        import android.text.method.ScrollingMovementMethod;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.View;
        import android.widget.EditText;
        import android.widget.TextView;
        import android.widget.Toast;


        import java.io.File;
        import java.io.FileOutputStream;
        import java.util.Random;

        import butterknife.BindView;
        import butterknife.ButterKnife;

public class RealtimeTextToSpeechActivity extends AppCompatActivity {


    private static final int RECORD_REQUEST_CODE = 101;
    private String SAVE_FILE_NAME;
    private static final String TEXT_SAVE_FOLDER = "Akuvo Text";
    private static final String FILE_EXT=".txt";
    String Language,langCode;
    @BindView(R.id.status)
    TextView status;
    @BindView(R.id.textMessage)
    TextView textMessage;

    TextView covertedTextField ;
    String TEXTVIEW_HISTORY;
    private SpeechAPI speechAPI;
    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            if (speechAPI != null) {
                speechAPI.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (speechAPI != null) {
                speechAPI.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            if (speechAPI != null) {
                speechAPI.finishRecognizing();
            }
        }

    };
    private final SpeechAPI.Listener mSpeechServiceListener =
            new SpeechAPI.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (textMessage != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    // Writing Processed text to TextView.
                                    textMessage.setText(null);
                                    covertedTextField.append(" "+text);
                                    TEXTVIEW_HISTORY=text;
                                } else {
                                    textMessage.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_text_to_speech);;
        ButterKnife.bind(this);

        // Getting value from PutExtra
        Bundle extras = getIntent().getExtras();
        Language=extras.getString("langOption");
        langCode=getLangCode(Language);

        Log.i(Language,langCode);


        speechAPI = new SpeechAPI(RealtimeTextToSpeechActivity.this,langCode);
        covertedTextField = (TextView) findViewById(R.id.covertedTextField);
        covertedTextField.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onStop() {
        stopVoiceRecorder();

        // Stop Cloud Speech API
        speechAPI.removeListener(mSpeechServiceListener);
        speechAPI.destroy();
        speechAPI = null;

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isGrantedPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else {
            makeRequest(Manifest.permission.RECORD_AUDIO);
        }
        speechAPI.addListener(mSpeechServiceListener);
    }

    private int isGrantedPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }

    private void makeRequest(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_REQUEST_CODE);
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RECORD_REQUEST_CODE) {
            if (grantResults.length == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED
                    && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                finish();
            } else {
                startVoiceRecorder();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.realtime_text_to_speech_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.undo_text_translated){
            if(TEXTVIEW_HISTORY!=null){
                covertedTextField.setText(covertedTextField.getText().toString().replace(TEXTVIEW_HISTORY,""));
            }
        }else if(item.getItemId()==R.id.clear_text_translated){
            covertedTextField.setText("");
        }else if(item.getItemId()==R.id.save_text_translated){
            // get translated_text_save_dialog.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(RealtimeTextToSpeechActivity.this);
            View promptView = layoutInflater.inflate(R.layout.translated_text_save_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RealtimeTextToSpeechActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText editText = (EditText) promptView.findViewById(R.id.text_file_name);
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    // Saving File with Converted Text
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            SAVE_FILE_NAME = editText.getText().toString();
                            String filepath = Environment.getExternalStorageDirectory().getPath();
                            File file = new File(filepath,TEXT_SAVE_FOLDER);

                            if(!file.exists()){
                                file.mkdirs();
                            }
                            File tempFile = new File(file.getAbsolutePath(),SAVE_FILE_NAME+FILE_EXT);

                            // Handle Files with same name
                            if(tempFile.exists()){
                                int i=0;
                                while(tempFile.exists()) {
                                    i++;
                                    tempFile = new File(file.getAbsolutePath(),SAVE_FILE_NAME+i+FILE_EXT);
                                }
                                SAVE_FILE_NAME+=i;
                            }
                            saveFile(file.getAbsolutePath() + "/" + SAVE_FILE_NAME+FILE_EXT,covertedTextField.getText().toString());

                        }
                    })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            // create an alert dialog
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();

        // Copy to clip board
        }else if(item.getItemId()==R.id.copy_text_translated){
            String clipBoardData=covertedTextField.getText().toString();
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text",clipBoardData);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(RealtimeTextToSpeechActivity.this,"Copied To Clip Board",Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    // Get Language Code For Google Speech API
    String getLangCode(String LangName){
        Log.i("lang",LangName);
        if(LangName.equals("മലയാളം"))
            return "ml-IN";
        else if(LangName.equals("English"))
            return "en-IN";
        else if(LangName.equals("தமிழ்"))
            return "ta-IN";
    return "en-IN";
    }

    // Save File Function
    public  void  saveFile(String file,String text)
    {
        try{
            FileOutputStream fos= new FileOutputStream(file);
            fos.write(text.getBytes());
            fos.close();
            File tmpFile = new File(file);
            Toast.makeText(RealtimeTextToSpeechActivity.this, "Saved to "+tmpFile.getName(), Toast.LENGTH_SHORT).show();


        }catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(RealtimeTextToSpeechActivity.this, "Error saving file!",Toast.LENGTH_SHORT).show();
        }
    }

}
