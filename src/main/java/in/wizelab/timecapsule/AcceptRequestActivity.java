package in.wizelab.timecapsule;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class AcceptRequestActivity extends ListActivity {

    String TAG = AcceptRequestActivity.class.getSimpleName();

    protected List<ParseObject> mPending;
    protected List<ParseUser> mPendingUsers;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseUser mCurrentUser;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_request);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        final TextView tvEmpty = findViewById(android.R.id.empty);

        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation=mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

        final List<String> users=new ArrayList<>();
        ParseQuery<ParseObject> query=new ParseQuery<ParseObject>(ParseConstants.CLASS_RELATIONS);
        query.whereEqualTo(ParseConstants.KEY_FRIENDS_REQUEST_PENDING,ParseUser.getCurrentUser().getObjectId());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null) {
                    if(objects.size()>0) {
                        mPending = objects;
                        int i = 0;
                        for (ParseObject obj : mPending) {
                            users.add(obj.getString(ParseConstants.KEY_USERID));
                            i++;
                            Log.d(TAG,"requested userId: "+String.valueOf(users));
                        }

                        ParseQuery<ParseUser> query2= ParseUser.getQuery();
                        query2.whereContainedIn(ParseConstants.KEY_USER_OBJECTID,users);
                        query2.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> objects, ParseException e) {
                                if(e==null) {
                                    Log.d(TAG,"Sucessfully fetched users! "+objects.size());
                                    mPendingUsers = objects;
                                    users.clear();
                                    String[] usernames = new String[mPendingUsers.size()];
                                    int i = 0;
                                    for (ParseUser user : mPendingUsers) {
                                        usernames[i] = user.getUsername();
                                        users.add(user.getString(ParseConstants.KEY_USERID));
                                        Log.d(TAG,user.getUsername());
                                        i++;
                                    }
                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getListView().getContext(),
                                                android.R.layout.simple_list_item_1, usernames);
                                        setListAdapter(adapter);

                                }else{
                                    Log.d(TAG,e.getMessage());
                                }
                            }
                        });
                    }else{
                        tvEmpty.setText("No friend requests");
                    }
                }else{
                    tvEmpty.setText(e.getMessage());
                }
            }
        });


    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        ParseObject m=mPending.get(position);
        if(getListView().isItemChecked(position)) {
            //add friend
            mFriendsRelation.add(mPendingUsers.get(position));
            ArrayList<String> idsToRemove = new ArrayList<String>();
            idsToRemove.add(mCurrentUser.getObjectId());
            m.removeAll(ParseConstants.KEY_FRIENDS_REQUEST_PENDING, idsToRemove);
            m.addAll(ParseConstants.KEY_FRIENDS_REQUEST_ACCEPTED, idsToRemove);
            m.saveInBackground();
        }else{
            //remove friend
            //mPendingRelation.remove(mPending.get(position));
        }
        mCurrentUser.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e!=null){
                    Log.e(TAG,e.getMessage());
                }
            }
        });
    }
}
