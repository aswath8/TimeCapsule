package in.wizelab.timecapsule;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

public class Show {
    Context mContext;
    public Show(Context context){
        mContext=context;
    }

    public void Toast(String s){
        Toast.makeText(mContext,s,Toast.LENGTH_LONG).show();
    }

    public void SimpleAlertDialog(String title,String message){
        AlertDialog.Builder builder =new AlertDialog.Builder(mContext);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok,null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
