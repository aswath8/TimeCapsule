package in.wizelab.timecapsule;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraFragment extends Fragment {

    private CameraPreview mCameraPreview;
    public static final String TAG = CameraFragment.class.getSimpleName();
    MainActivity mainActivity;

    @BindView(R.id.camera_preview)
    RelativeLayout rl;
    @BindView(R.id.button_capture) Button captureButton;
    @BindView(R.id.button_record) Button recordButton;
    @BindView(R.id.iv_camera_shot)
    ImageView ivcapture;
    SurfaceView preview;
    boolean recording=false;
    boolean microphone=false;

    private Handler mHandler = new Handler();
    private final Runnable mLoadCamera = new Runnable()
    {
        public void run()
        {
            startCamera();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        ButterKnife.bind(this,rootView);
        mainActivity  = (MainActivity)getActivity();
        captureButton.setVisibility(View.VISIBLE);
        Dexter.withActivity(getActivity()).withPermissions(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(cameraListener).check();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();

    }


    MultiplePermissionsListener cameraListener = new MultiplePermissionsListener() {
        @Override
        public void onPermissionsChecked(MultiplePermissionsReport report) {
            PermissionRequest pr = new PermissionRequest(Manifest.permission.CAMERA);
            report.getGrantedPermissionResponses().contains(pr);
            {
                ready();
                mHandler.postDelayed(mLoadCamera, 100);
            }
        }

        @Override
        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
            token.continuePermissionRequest();
        }
    };

    PermissionListener microphoneListener= new PermissionListener() {
        @Override
        public void onPermissionGranted(PermissionGrantedResponse response) {
            microphone=true;
        }

        @Override
        public void onPermissionDenied(PermissionDeniedResponse response) {
            if (response.isPermanentlyDenied()) {
                microphone=false;
                openSettings();
            }
        }

        @Override
        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
            microphone=false;
            token.continuePermissionRequest();
        }
    };

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    void ready(){
        captureButton.setVisibility(View.VISIBLE);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mainActivity.mRequestingLocationUpdates) {
                    mainActivity.startLocationButtonClick();
                } else if (mainActivity.mCurrentLocation == null) {
                    Toast.makeText(getActivity(), "Obtaining location...", Toast.LENGTH_SHORT).show();
                } else {
                    //ivcapture.setImageBitmap(mCameraPreview.getBitmap());
                    //mCameraPreview.takePic(mainActivity.mCurrentLocation);
                    //screenShot();
                    ivcapture.setImageBitmap(mCameraPreview.newSave());
                    ivcapture.setVisibility(View.VISIBLE);
                }
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity(getActivity()).withPermission(Manifest.permission.RECORD_AUDIO).withListener(microphoneListener).check();
                if (microphone) {
                    if (!recording) {
                        if (mainActivity.mCurrentLocation != null) {
                            recordButton.setText("STOP");
                            mCameraPreview.startCapturingVideo(mainActivity.mCurrentLocation);
                            recording = true;
                        }
                    } else {
                        recordButton.setText("RECORD");
                        recording = false;
                        mCameraPreview.stopCapturingVideo();
                    }
                }
            }
        });
    }
    private void startCamera()
    {
        preview = new SurfaceView(getActivity());
        try
        {
            mCameraPreview = new CameraPreview(getActivity().getApplicationContext(),preview);
        } catch(Exception e)
        {
            Log.d("debug", "Another exception");
            e.printStackTrace();
        }

        if(rl != null && preview != null) {
            rl.addView(preview);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            preview.setLayoutParams(lp);
        }
    }


}
