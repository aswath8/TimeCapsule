package in.wizelab.timecapsule;

import android.app.ListActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddFriendsActivity extends ListActivity {

    public static String TAG= AddFriendsActivity.class.getSimpleName();

    protected List<ParseUser> mUsers;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseUser mCurrentUser;
    protected ParseObject mFriendRequest;

    List<String> addfriendList= new ArrayList<String>();


    @BindView(R.id.tvAddfriends)
    TextView tvAddFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);
        ButterKnife.bind(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvAddFriends.setText("Refreshing...");
        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation=mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

        setProgressBarIndeterminateVisibility(true);
        ParseQuery<ParseUser> query= ParseUser.getQuery();
        query.orderByAscending(ParseConstants.KEY_USERNAME);
        ArrayList<String> idsToRemove = new ArrayList<String>();
        idsToRemove.add(ParseUser.getCurrentUser().getObjectId());
        query.whereNotContainedIn(ParseConstants.KEY_USER_OBJECTID,idsToRemove);
        query.setLimit(100);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> users, ParseException e) {
                setProgressBarIndeterminateVisibility(false);

                if(e==null){
                    //Sucess
                    tvAddFriends.setText("Sucess!");
                    mUsers = users;
                    String[] usernames = new String[mUsers.size()];
                    int i = 0;
                    for(ParseUser user: mUsers){
                        usernames[i]=user.getUsername();
                        i++;
                    }
                    ArrayAdapter<String> adapter  = new ArrayAdapter<String>(AddFriendsActivity.this,
                            android.R.layout.simple_list_item_checked,usernames);
                    setListAdapter(adapter);
                    addFriendCheckmarks();
                }else{
                    tvAddFriends.setText("Error\n"+e.getMessage());
                    Log.e(TAG,e.getMessage());
                }
            }
        });

        ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(ParseConstants.CLASS_RELATIONS);
        query2.whereEqualTo(ParseConstants.KEY_USERID, ParseUser.getCurrentUser().getObjectId());
        query2.setLimit(1);
        query2.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null){
                    if(objects.size()==0) {
                        mFriendRequest = new ParseObject(ParseConstants.CLASS_RELATIONS);
                        mFriendRequest.put(ParseConstants.KEY_USERID,ParseUser.getCurrentUser().getObjectId());
                    }else {
                        Log.d(TAG, String.valueOf(objects.size()));
                        mFriendRequest = objects.get(0);
                        addfriendList = mFriendRequest.getList(ParseConstants.KEY_FRIENDS_REQUEST_PENDING);
                        Log.d(TAG, addfriendList.toString());
                    }
                }else{
                    Log.d(TAG,e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if(getListView().isItemChecked(position)){
            //add friend
            if(!addfriendList.contains(mUsers.get(position).getObjectId())) {
                addfriendList.add(mUsers.get(position).getObjectId());
                mFriendRequest.add(ParseConstants.KEY_FRIENDS_REQUEST_PENDING, mUsers.get(position).getObjectId());
                mFriendRequest.saveInBackground();
            }
        }else{
            //remove friend
            mFriendsRelation.remove(mUsers.get(position));
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

    private void addFriendCheckmarks(){
        mFriendsRelation.getQuery().findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> friends, ParseException e) {
                if(e==null){
                    //list returned - look for a match
                    for(int i=0;i<mUsers.size();i++){
                        ParseUser user = mUsers.get(i);
                        for(ParseUser friend: friends){
                            if(friend.getObjectId().equals(user.getObjectId())){
                                getListView().setItemChecked(i,true);
                            }
                        }
                    }
                }else{
                    Log.e(TAG,e.getMessage());
                }
            }
        });
    }

}
