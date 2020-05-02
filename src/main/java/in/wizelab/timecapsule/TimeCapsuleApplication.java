package in.wizelab.timecapsule;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;

public class TimeCapsuleApplication extends Application{
    private static TimeCapsuleApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance=this;
        String API_ENDPOINT= getResources().getString(R.string.back4app_server_url);
        String APP_ID=getResources().getString(R.string.back4app_app_id);
        String CLIENT_KEY=getResources().getString(R.string.back4app_client_key);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(APP_ID)
                .clientKey(CLIENT_KEY)
                .server(API_ENDPOINT)
                .enableLocalDataStore()
                .build());
        ParseInstallation.getCurrentInstallation().saveInBackground();

    }

    public static synchronized TimeCapsuleApplication getInstance() {
        return mInstance;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.connectivityReceiverListener = listener;
    }
}
