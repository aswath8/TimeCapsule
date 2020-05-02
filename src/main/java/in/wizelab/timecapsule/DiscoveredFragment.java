package in.wizelab.timecapsule;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import in.wizelab.timecapsule.adapters.ObjectAdapter;

public class DiscoveredFragment extends Fragment{
    public static final String TAG = DiscoveredFragment.class.getSimpleName();
    @BindView(R.id.textView) TextView locationText;
    @BindView(R.id.emptyGridView) TextView emptyTextView;
    MainActivity mainActivity;

    protected GridView mGridView;
    protected List<ParseObject> mObjects;

    protected Show mShow=new Show(getActivity());
    protected Boolean queryRunning;


    public DiscoveredFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discovered, container, false);
        ButterKnife.bind(this,rootView);
        mainActivity  = (MainActivity)getActivity();
        mGridView=(GridView)rootView.findViewById(R.id.gridView);
        mGridView.setEmptyView(emptyTextView);
        queryRunning=false;
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateGridView();

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                // Display the selected/clicked item text and position on TextView
                Log.d(TAG,"GridView item clicked : " +selectedItem
                        + "\nAt index position : " + position);
                ParseObject message = mObjects.get(position);
                String messageType =message.getString(ParseConstants.KEY_FILE_TYPE);
                ParseFile file = message.getParseFile(ParseConstants.KEY_FILE);
                Uri fileUri = Uri.parse(file.getUrl());
                if(messageType.equals(ParseConstants.TYPE_IMAGE)) {
                    //View Image
                    Intent intent = new Intent(getActivity(), ViewActivity.class);
                    intent.setData(fileUri);
                    startActivity(intent);
                }
            }
        });
    }

    @OnClick(R.id.button_refresh)
    void refresh(){

        if (!mainActivity.mRequestingLocationUpdates) {
            mainActivity.startLocationButtonClick();
        }else if(mainActivity.mCurrentLocation==null){
            mShow.Toast("Obtaining location");
        }else{
            mGridView.setVisibility(View.INVISIBLE);
            mGridView.setEnabled(false);
            doParseQuery(mainActivity.mCurrentLocation);
            locationText.setText("Current location: "+mainActivity.mCurrentLocation+"\n");
        }

    }

    void display(Location location){

    }

    void doParseQuery(final Location location){
        if(isNetworkAvailable()) {
            ParseRelation<ParseUser> fr;
            fr=ParseUser.getCurrentUser().getRelation(ParseConstants.KEY_FRIENDS_RELATION);
            final List<String> friendsId= new ArrayList<String>();
            ParseQuery<ParseUser> queryFriends = fr.getQuery();
            queryFriends.fromLocalDatastore();
            queryFriends.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> friends, ParseException e) {
                    if(e==null) {
                        for (ParseUser user : friends) {
                            friendsId.add(user.getObjectId());
                            Log.d(TAG,user.getObjectId());
                        }
                        friendsId.add(ParseUser.getCurrentUser().getObjectId());
                        getMessages(location,friendsId);
                    }else{
                        Log.e(TAG,e.getMessage());
                    }
                }
            });

        }else{
            mainActivity.showNoNetwork();
        }
    }

    void getMessages(Location location,List<String> friendsId){
        if(!queryRunning) {
            queryRunning = true;
            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            ParseQuery<ParseObject> circleQuery = ParseQuery.getQuery(ParseConstants.CLASS_MESSAGES);
            circleQuery.whereContainedIn(ParseConstants.KEY_SENDER_ID, friendsId);
            circleQuery.whereWithinMiles(ParseConstants.KEY_LOCATION_POINT, parseGeoPoint, 0.1);

            ParseQuery<ParseObject> socialQuery = ParseQuery.getQuery(ParseConstants.CLASS_MESSAGES);
            socialQuery.whereEqualTo(ParseConstants.KEY_SOCIAL, true);

            List<ParseQuery<ParseObject>> list = new ArrayList<ParseQuery<ParseObject>>();
            list.add(circleQuery);
            list.add(socialQuery);

            ParseQuery<ParseObject> query = ParseQuery.or(list);
            query.findInBackground();

            circleQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    queryRunning =false;
                    if (e == null) {
                        if (!objects.isEmpty()) {
                            showToast("Time Capsules discovered: " + objects.size());
                            // Iterating over the results
                            //we found messages
                            mObjects = objects;
                            String[] usernames = new String[mObjects.size()];
                            int i = 0;
                            for (ParseObject message : mObjects) {
                                usernames[i] = message.getString(ParseConstants.KEY_SENDER_NAME);
                                // Now get Latitude and Longitude of the object
                                double queryLatitude = message.getParseGeoPoint(ParseConstants.KEY_LOCATION_POINT).getLatitude();
                                double queryLongitude = message.getParseGeoPoint(ParseConstants.KEY_LOCATION_POINT).getLongitude();
                                locationText.append("\n" + queryLatitude + ", " + queryLongitude);
                                i++;
                            }
                            updateGridView();
                            mGridView.setEnabled(true);
                            mGridView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        locationText.append("\nNothing Found");
                    }
                }
            });
        }
    }

    void updateGridView(){
        if(mObjects!=null) {
            if (mGridView.getAdapter() == null) {
                ObjectAdapter adapter = new ObjectAdapter(getActivity(), mObjects);
                mGridView.setAdapter(adapter);
            } else {
                ((ObjectAdapter) mGridView.getAdapter()).refill(mObjects);
            }
        }
    }

    void showToast(String s){
        Toast.makeText(getActivity(),s,Toast.LENGTH_SHORT).show();
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
