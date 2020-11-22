package com.example.audio_player_and_recorder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFilePickedCallback {

    private final int PERMISSIONS = 1;
    private final String RECORDING_FILE_EXTENSION = ".mp3";

    private File mRecordingsDir;
    FileAdapter mFileAdapter;
    List<File> mRecordings;

    MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordAudio();
            }
        });

        mRecordingsDir = Environment.getExternalStorageDirectory();

        // media player setup
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setVolume(1, 1);

        // all available recordings and playing them
        mRecordings = new ArrayList<File>();
        File[] files = mRecordingsDir.listFiles();
        for (File f : files){
            if (f.isFile() && f.getName().endsWith(RECORDING_FILE_EXTENSION)) {
                mRecordings.add(f);
            }
        }

        RecyclerView recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView);
        recordingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFileAdapter = new FileAdapter(mRecordings, this);
        recordingsRecyclerView.setAdapter(mFileAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMediaPlayer.stop();
        mMediaPlayer.release();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
            }, PERMISSIONS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private void recordAudio() {
        try {
            Calendar calendar = Calendar.getInstance();

            String fileName = String.format(Locale.US, "recording_%d%02d%02d%02d%02d%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));
            String filePath = mRecordingsDir.getAbsolutePath() + "/" + fileName + RECORDING_FILE_EXTENSION;
            final File file = new File(filePath);

            // media recorder setup
            final MediaRecorder mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioSamplingRate(8000);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(file.getAbsolutePath());

            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("0");

            builder1.setPositiveButton(
                    "End recording",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            final Timer timer = new Timer();
            builder1.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    timer.cancel();
                    timer.purge();

                    if (!mRecordings.contains(file)) {
                        mRecordings.add(file);
                        mFileAdapter.notifyDataSetChanged();
                    }
                }
            });

            final AlertDialog alert = builder1.create();

            alert.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {

                    final long startTime = System.currentTimeMillis();

                    TimerTask task = new TimerTask() {

                        @Override
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    long dt = System.currentTimeMillis() - startTime;
                                    int minutes = (int) dt / 1000 / 60;
                                    int seconds = (int) (dt / 1000) % 60;
                                    alert.setMessage(String.format("%d:%02d", minutes, seconds));
                                }
                            });
                        }
                    };

                    timer.scheduleAtFixedRate(task, 0, 1000);
                }
            });

            mediaRecorder.prepare();
            mediaRecorder.start();
            alert.show();
        } catch (IOException e) {
            Toast.makeText(this, "Error recording file", Toast.LENGTH_LONG);
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == PERMISSIONS)) {
            for(int grantResult : grantResults) {
                if (grantResult != 1) this.finishAffinity();
            }
        }
    }

    @Override
    public void OnFilePicked(File file) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();

            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Playing file " + file.getName(), Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    snackbar.dismiss();
                }
            });
        } catch (IOException e) {
            Snackbar.make(findViewById(android.R.id.content), "Could not play file " + file.getName(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }
}