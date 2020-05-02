package in.wizelab.timecapsule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartTCService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(RestartTCService.class.getSimpleName(), "Restarting TCService!");
        context.startService(new Intent(context, TCService.class));;
    }
}