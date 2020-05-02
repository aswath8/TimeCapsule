package in.wizelab.timecapsule;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FriendsFragment extends ListFragment {
    public static final String TAG=FriendsFragment.class.getSimpleName();

    protected List<ParseUser> mFriends;
    protected List<ParseUser> mAcceptedFriends;
    protected ParseObject mAccepted;
    protected ParseObject mFriendRequest;

    protected ParseRelation<ParseUser> mFriendsRelation;

    protected MainActivity mainActivity;

    protected ParseUser mCurrentUser;
    List<String> usernames;
    ArrayAdapter<String> adapter;
    List<String> addfriendList= new ArrayList<String>();

    protected Boolean queryRunning;


    @BindView(R.id.tvFriends)TextView tvFriends;

    @Override
    public View onCreateView( LayoutInflater inflater,  ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends, container, false);
        ButterKnife.bind(this,rootView);
        mainActivity=(MainActivity)getActivity();
        usernames = new ArrayList<String>();
        queryRunning=false;
        doQuery();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        doOfflineQuery();
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final ParseUser friend  = mFriends.get(position);

                ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>(ParseConstants.CLASS_RELATIONS);
                query2.whereEqualTo(ParseConstants.KEY_USERID, friend.getObjectId());
                query2.setLimit(1);
                query2.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if(e==null){
                            if(objects.size()==0) {
                                mFriendRequest = new ParseObject(ParseConstants.CLASS_RELATIONS);
                                mFriendRequest.put(ParseConstants.KEY_USERID,friend.getObjectId());
                            }else {
                                Log.d(TAG, String.valueOf(objects.size()));
                                mFriendRequest = objects.get(0);
                                addfriendList = mFriendRequest.getList(ParseConstants.KEY_FRIENDS_DELETED);

                                if(!addfriendList.contains(friend.getObjectId())) {
                                    addfriendList.add(friend.getObjectId());
                                    Toast.makeText(getActivity(),"Removing Friend: "+friend.getUsername(),Toast.LENGTH_SHORT).show();
                                    mFriendRequest.add(ParseConstants.KEY_FRIENDS_DELETED, friend.getObjectId().toString());
                                    mFriendRequest.saveInBackground();
                                }
                            }
                        }else{
                            Log.d(TAG,e.getMessage());
                        }
                    }
                });

                mFriendsRelation.remove(friend);
                return true;
            }
        });
    }

    void updateListview(){
        if (adapter == null){
            adapter = new ArrayAdapter<String>(getListView().getContext(),
                    android.R.layout.simple_list_item_1, usernames);
            setListAdapter(adapter);
        }else {
            adapter.clear();
            adapter.addAll(usernames);
            adapter.notifyDataSetChanged();
        }
    }

    void doQuery(){
        if(!queryRunning) {
            queryRunning = true;
            mCurrentUser = ParseUser.getCurrentUser();
            mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

            final ParseQuery<ParseObject> query1 = new ParseQuery<ParseObject>(ParseConstants.CLASS_RELATIONS);
            query1.whereEqualTo(ParseConstants.KEY_USERID, ParseUser.getCurrentUser().getObjectId());
            query1.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() == 1) {
                            mAccepted = objects.get(0);
                            final List<String> acceptedUsers = mAccepted.getList(ParseConstants.KEY_FRIENDS_REQUEST_ACCEPTED);
                            final List<String> deletedUsers = mAccepted.getList(ParseConstants.KEY_FRIENDS_DELETED);

                            ParseQuery<ParseUser> query2 = ParseUser.getQuery();
                            query2.whereContainedIn(ParseConstants.KEY_USER_OBJECTID, acceptedUsers);
                            query2.findInBackground(new FindCallback<ParseUser>() {
                                @Override
                                public void done(List<ParseUser> objects, ParseException e) {
                                    mAcceptedFriends = objects;
                                    int i = 0;
                                    ArrayList<String> acceptedIdsToRemove = new ArrayList<String>();
                                    ArrayList<String> deletedIdsToRemove = new ArrayList<String>();
                                    for (ParseUser user : mAcceptedFriends) {
                                        if(acceptedUsers.contains(user.getObjectId())) {
                                            mFriendsRelation.add(user);
                                            acceptedIdsToRemove.add(user.getObjectId());
                                        }else if(deletedUsers.contains(user.getObjectId())){
                                            mFriendsRelation.remove(user);
                                            deletedIdsToRemove.add(user.getObjectId());
                                        }
                                        i++;
                                    }
                                    mAccepted.removeAll(ParseConstants.KEY_FRIENDS_REQUEST_ACCEPTED, acceptedIdsToRemove);
                                    mAccepted.removeAll(ParseConstants.KEY_FRIENDS_DELETED, deletedIdsToRemove);
                                    mAccepted.saveInBackground();
                                    mCurrentUser.saveInBackground();


                                    ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
                                    query.addAscendingOrder(ParseConstants.KEY_USERNAME);
                                    tvFriends.setText("Loading");
                                    query.findInBackground(new FindCallback<ParseUser>() {
                                        @Override
                                        public void done(List<ParseUser> friends, ParseException e) {
                                            queryRunning=false;
                                            if (e == null) {
                                                tvFriends.setText("Sucess!");
                                                mFriends = friends;
                                                ParseUser.pinAllInBackground(mFriends);
                                                int i = 0;
                                                for (ParseUser user : mFriends) {
                                                    usernames.add(user.getUsername());
                                                    i++;
                                                }
                                                ParseUser.pinAllInBackground(friends);
                                                updateListview();

                                            } else {
                                                tvFriends.setText("Error: " + e.getMessage());
                                                Log.e(TAG, e.getMessage());
                                            }
                                        }
                                    });


                                }
                            });

                        }
                    } else {
                        Log.e(TAG, e.getMessage());
                    }
                }
            });

        }
    }

    void doOfflineQuery(){
        ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
        query.addAscendingOrder(ParseConstants.KEY_USERNAME);
        query.fromLocalDatastore();
        tvFriends.setText("Loading");
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> friends, ParseException e) {
                queryRunning=false;
                if (e == null) {
                    tvFriends.setText("Sucess!");
                    mFriends = friends;
                    ParseUser.pinAllInBackground(mFriends);
                    int i = 0;
                    for (ParseUser user : mFriends) {
                        usernames.add(user.getUsername());
                        i++;
                    }
                    updateListview();

                } else {
                    tvFriends.setText("Error: " + e.getMessage());
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    @OnClick(R.id.bAddFriends)
    void addFriends(){
        startActivity(new Intent(getActivity(),AddFriendsActivity.class));
    }


    @OnClick(R.id.bAcceptFriends)
    void acceptFriends(){
        startActivity(new Intent(getActivity(),AcceptRequestActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();

    }

}
