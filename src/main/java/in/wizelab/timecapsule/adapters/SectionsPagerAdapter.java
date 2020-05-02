package in.wizelab.timecapsule.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import in.wizelab.timecapsule.AboutFragment;
import in.wizelab.timecapsule.CameraFragment;
import in.wizelab.timecapsule.DiscoveredFragment;
import in.wizelab.timecapsule.EmptyFragment;
import in.wizelab.timecapsule.FriendsFragment;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {
    protected Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext=context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        switch(position) {
            case 0:
                return new AboutFragment();
            case 1:
                return new EmptyFragment();
            case 2:
                return new DiscoveredFragment();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }
}