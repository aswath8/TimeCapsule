package in.wizelab.timecapsule;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SendActivity extends AppCompatActivity {
    public static final String TAG=SendActivity.class.getSimpleName();

    protected Uri mMediaUri;
    protected String mFileType;
    ImageView imageView;
    VideoView mVideoView;
    MediaController mediaController;
    Boolean social;

    Location location;
    protected String fileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        ButterKnife.bind(this);
        imageView = (ImageView)findViewById(R.id.imageView);
        mVideoView = (VideoView)findViewById(R.id.videoView);
        social=false;
        mMediaUri=getIntent().getData();
        mFileType=getIntent().getExtras().getString(ParseConstants.KEY_FILE_TYPE);
        location=getIntent().getParcelableExtra("location");
        Log.d(TAG,mMediaUri.toString());


        if(mFileType.equals(ParseConstants.TYPE_VIDEO)) {
            imageView.setVisibility(View.GONE);
            mediaController= new MediaController(this);
            mediaController.setAnchorView(mVideoView);
            mVideoView.setVideoURI(mMediaUri);
            mVideoView.setMediaController(mediaController);
            mVideoView.requestFocus();
            mVideoView.start();
        }else {
            mVideoView.setVisibility(View.GONE);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            Picasso.with(this).load(mMediaUri).fit().into(imageView);
        }
    }

    @OnClick(R.id.button_send)
    public void sendButton(){
        Toast.makeText(SendActivity.this, "Sending...", Toast.LENGTH_LONG).show();
        ParseObject message = createMessage();
        if(message==null){
            //error
            Toast.makeText(SendActivity.this, "Error with the file", Toast.LENGTH_LONG).show();

            AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
            builder.setMessage("There was an error with the selected file")
                    .setTitle("Sorry!")
                    .setPositiveButton(android.R.string.ok,null);
            AlertDialog dialog= builder.create();
            dialog.show();
        }else {
            send(message);
            finish();
        }
    }

    protected ParseObject createMessage(){
        ParseObject message = new ParseObject(ParseConstants.CLASS_MESSAGES);
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(ParseConstants.KEY_SENDER_NAME,ParseUser.getCurrentUser().getUsername());
        message.put(ParseConstants.KEY_FILE_TYPE,mFileType);
        message.put(ParseConstants.KEY_SOCIAL,social);
        ParseGeoPoint point = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        message.put(ParseConstants.KEY_LOCATION_POINT, point);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses  = null;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String zip = addresses.get(0).getPostalCode();
        String country = addresses.get(0).getCountryName();
       message.put(ParseConstants.KEY_LOCATION_LOCALITY, city);
        message.put(ParseConstants.KEY_LOCATION_POSTALCODE, zip);
        message.put(ParseConstants.KEY_LOCATION_AREA, state);
       message.put(ParseConstants.KEY_LOCATION_COUNTRY, country);

        byte[] fileBytes=FileHelper.getByteArrayFromFile(this,mMediaUri);
        if(fileBytes==null){
            return null;
        }else{
            if(mFileType.equals(ParseConstants.TYPE_IMAGE)){
                fileBytes=FileHelper.reduceImageForUpload(fileBytes);
            }
            String fileName=FileHelper.getFileName(this,mMediaUri,mFileType);

            Log.d(TAG,"Filename: "+fileName);
            ParseFile file = new ParseFile(fileName,fileBytes);
            message.put(ParseConstants.KEY_FILE,file);
            return message;
        }
    }

    protected void send(ParseObject message){
        message.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e==null){
                    //Sucess!
                    Toast.makeText(SendActivity.this,"Message sent",Toast.LENGTH_SHORT).show();
                }else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
                    builder.setMessage("Error sending message")
                            .setTitle("Sorry!")
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }
            }

        });
    }

}
