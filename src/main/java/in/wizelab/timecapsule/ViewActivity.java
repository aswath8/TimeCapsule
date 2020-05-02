package in.wizelab.timecapsule;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        ImageView imageView = (ImageView)findViewById(R.id.viewImage);
        Uri imageUri = getIntent().getData();
        Picasso.with(this).load(imageUri.toString()).fit().into(imageView);
    }
}
