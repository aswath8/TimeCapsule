package in.wizelab.timecapsule;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraPreview implements
        SurfaceHolder.Callback,Camera.PictureCallback,Camera.PreviewCallback {
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Context context;
    private String TAG=CameraPreview.class.getSimpleName();
    Location location;

    private static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCameraApp");
    private MediaRecorder recorder;

    private static String fileName;
    protected static File mediaFile;



    // Constructor that obtains context and camera
    @SuppressWarnings("deprecation")
    public CameraPreview(Context context,SurfaceView cameraPreview) {
        this.context=context;
        mSurfaceHolder = cameraPreview.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (mCamera == null) {
            mCamera=getCameraInstance();
        }
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                mCamera.setPreviewCallback(this);

                /*
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        byteArray=data;
                    }
                });
                */
            }
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
                               int width, int height) {
        refreshCamera();
    }
    public void refreshCamera() {
        if (mSurfaceHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    protected void takePic(Location l){
        location=l;
        mCamera.takePicture(null,null,this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(rotate(data));
            fos.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
        }
        Intent recipientsIntent = new Intent(context,SendActivity.class);
            recipientsIntent.setData(Uri.fromFile(mediaFile));
            recipientsIntent.putExtra(ParseConstants.KEY_FILE_TYPE, ParseConstants.TYPE_IMAGE);
            recipientsIntent.putExtra(ParseConstants.KEY_LOCATION_POINT, location);
            context.startActivity(recipientsIntent);

        mCamera.startPreview();
    }

    public void prepareMediaRecorder() {
        recorder = new MediaRecorder();
        mCamera.unlock();
        recorder.setOrientationHint(90);
        recorder.setCamera(mCamera);
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        //recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        //recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

        recorder.setOutputFile(filePath(".mp4"));
        recorder.setMaxDuration(5*1000);

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
        } catch (IOException e) {
            releaseMediaRecorder();
        }

    }

    public void startCapturingVideo(Location l){
        location=l;
        prepareMediaRecorder();
        recorder.start();
    }

    public void stopCapturingVideo() {
        recorder.stop();
        releaseMediaRecorder();

            Intent recipientsIntent = new Intent(context, SendActivity.class);
            recipientsIntent.setData(Uri.parse("file://" + fileName));
            recipientsIntent.putExtra(ParseConstants.KEY_FILE_TYPE, ParseConstants.TYPE_VIDEO);
            recipientsIntent.putExtra(ParseConstants.KEY_LOCATION_POINT, location);
            context.startActivity(recipientsIntent);

    }

    private void releaseMediaRecorder() {
        if (recorder != null) {
            recorder.reset(); // clear recorder configuration
            recorder.release(); // release the recorder object
            recorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    Camera.Parameters params;
    Camera.Size mSize;
    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
            params = camera.getParameters();
            if(params.getFlashMode()!=null){
                //params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            }
            if(params.isZoomSupported()){
                params.setZoom(0);
            }

            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            mSize = null;
            for (Camera.Size size : sizes) {
                Log.i(TAG, "Available resolution: "+size.width+" "+size.height);
                if(size.height>=3120) {
                    mSize = size;
                }else{
                    break;
                }
            }
            params.set("orientation", "portrait");
            if(android.os.Build.VERSION.SDK_INT > 7)
                camera.setDisplayOrientation(90);
            params.setRotation(90);
            Log.i(TAG, "Chosen resolution: "+mSize.width+" "+mSize.height);
            params.setPictureSize(mSize.width, mSize.height);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(params);

        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        return camera;
    }

    private static File getOutputMediaFile() {
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        mediaFile = new File(filePath(".jpg"));
        return mediaFile;
    }

    private static String filePath(String fileExtension){
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        fileName=mediaStorageDir.getPath() + File.separator
                + timeStamp + fileExtension;
        return fileName;
    }

    public static byte[] rotate(byte[] data) {
        Matrix matrix = new Matrix();
        //Device.getOrientation() is used in order to support the emulator and an actual device
        matrix.postRotate(90);
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap.getWidth() < bitmap.getHeight()) {
            //no rotation needed
            return data;
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true
        );
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        byte[] bm = blob.toByteArray();
        return bm;
    }

    byte[] byteArray;

    public Bitmap getBitmap() {
        Bitmap mBitmap=null;
        try {
            if (params == null)
                return null;

            if (mSize == null)
                return null;

            int format = params.getPreviewFormat();
            YuvImage yuvImage = new YuvImage(byteArray, format, mSize.width, mSize.height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            Rect rect = new Rect(0, 0, mSize.width, mSize.height);

            yuvImage.compressToJpeg(rect, 100, byteArrayOutputStream);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inInputShareable = true;
            mBitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size(), options);

            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
       saveBitmap(mBitmap);
        return mBitmap;
    }

    private Bitmap bm,rotatedBitmap;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Camera.Parameters params = camera.getParameters();
        byteArray=data;

    }

    protected Bitmap newSave(){
        int w = params.getPreviewSize().width;
        int h = params.getPreviewSize().height;
        int format = params.getPreviewFormat();
        YuvImage image = new YuvImage(byteArray, format, w, h, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rect area = new Rect(0, 0, w, h);
        image.compressToJpeg(area, 100, out);
        bm = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        rotatedBitmap = Bitmap.createBitmap(bm, 0, 0,w, h, matrix, true);

        saveBitmap(rotatedBitmap);
        return rotatedBitmap;
    }

    void saveBitmap(Bitmap mBitmap){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + System.currentTimeMillis()+"newsave.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
        }
    }
}
