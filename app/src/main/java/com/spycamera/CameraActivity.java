package com.spycamera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

public class CameraActivity extends AppCompatActivity {
    public static final int DONE=1;


    private Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);






        // We need something to trigger periodically the capture of a
        // picture to be processed
        timer=new Timer(getApplicationContext(),threadHandler);
        timer.execute();
    }



    private void startRecording() {
        //  setRecording(true);

        ResultReceiver receiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                handlerCaptureImage(resultCode, resultData);
            }
        };

        CameraService.startToCaptureImage(this, receiver);
    }


    private void handlerCaptureImage(int resultCode, Bundle resultData) {

        if (resultCode == CameraService.RECORD_RESULT_OK) {
            Toast.makeText(this, "Start capturing...", Toast.LENGTH_SHORT).show();
            String videoPath = resultData.getString(CameraService.IMGE_PATH);
            Toast.makeText(this, "captured succeed, file saved in " + videoPath,
                    Toast.LENGTH_LONG).show();
            timer=new Timer(getApplicationContext(),threadHandler);
            timer.execute();
        } else {
            // start recording failed.
            Toast.makeText(this, "Start capturing failed...", Toast.LENGTH_SHORT).show();
            // setRecording(false);

        }

    }

    private Handler threadHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch(msg.what){
                case DONE:
                    // Trigger camera callback to take pic

                   startRecording();
                    break;

            }
        }
    };


public class Timer extends AsyncTask<Void, Void, Void> {
    Context mContext;
    private Handler threadHandler;
    public Timer(Context context,Handler threadHandler) {
        super();
        this.threadHandler=threadHandler;
        mContext = context;
    }
    @Override
    protected Void doInBackground(Void...params) {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Message.obtain(threadHandler,1, "").sendToTarget();
        return null;
    }
}

    @Override
    protected void onStop() {
        super.onStop();
        stopRecording();
    }

    private void stopRecording() {
        //  setRecording(false);

        ResultReceiver receiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                handleStopRecordingResult(resultCode, resultData);
            }
        };

        CameraService.startToStopCapture(this, receiver);
    }
    private void handleStopRecordingResult(int resultCode, Bundle resultData) {
        if (resultCode == CameraService.RECORD_RESULT_OK) {
            Toast.makeText(this, "stop capturing  image...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Record failed...", Toast.LENGTH_SHORT).show();
        }
    }
}

