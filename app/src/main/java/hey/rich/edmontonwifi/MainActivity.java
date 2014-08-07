package hey.rich.edmontonwifi;

import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hey.rich.edmontonwifi.SortWifiListDialogFragment.SortWifiListDialogListener;

public class MainActivity extends Activity implements OnNavigationListener,
        SortWifiListDialogListener {

    private List<Wifi> wifis;
    private WifiArrayAdapter adapter;
    private SharedPreferences prefs;
    private int sortChoice;

    // Navigation Drawer Variables
    private String[] mDrawerTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiList wifiList;
        wifiList = EdmontonWifi.getWifiList(getApplicationContext());
        wifis = wifiList.getAllWifis();

        setupDrawer();

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    /**
     * Sets up the Navigation Drawer
     */
    private void setupDrawer() {
        mTitle = mDrawerTitle = getTitle();

        mDrawerTitles = getResources().getStringArray(R.array.drawer_array);
        mDrawerList = (ListView) findViewById(R.id.main_left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDrawerTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }

    private void setupRefreshLocationButton() {
        ImageButton button = (ImageButton) findViewById(R.id.main_activity_refresh_location_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocation();
            }
        });
    }

    private void updateLocation() {
        Location l = EdmontonWifi.getLocation(this);
        if (l == null) {
            return;
        }
        Toast.makeText(this, "Getting location", Toast.LENGTH_SHORT).show();

        for (Wifi wifi : wifis) wifi.setDistanceToLocation(l);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onStart();

        prefs = getSharedPreferences("hey.rich.EdmontonWifi",
                Context.MODE_PRIVATE);
        // From this beauty: http://stackoverflow.com/a/5878986
        sortChoice = prefs.getInt("sort_choice", 0);

        if (prefs.getBoolean("firstrun", false)) {
            prefs.edit().putBoolean("firstrun", false).apply();
            new ShowcaseView.Builder(this, true)
                    .setTarget(new ViewTarget(findViewById(R.id.main_activity_listview)))
                    .setContentTitle("Select a Wifi")
                    .setContentText("To get more information about it and to perform actions")
                    .setStyle(R.style.ShowcaseViewTheme)
                    .build();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Editor edit = prefs.edit();
        edit.putInt("sort_choice", sortChoice);
        edit.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.main, menu);

        // Get the searchview and set the searchable conf
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search)
                .getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        // Don't iconify the widget; expand it by default
        searchView.setIconifiedByDefault(true);
        return true;
    }

    /* Called whenever we call invalidateOptionsMenu*/
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.menu_sort_wifi_list).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_clear_search_history).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // The action bar home/up action should open or close the drawer.
        // ActionBarToggle will take care of this
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_sort_wifi_list:
                showSortListDialog();
                return false;
            case R.id.menu_clear_search_history:
                clearSearchHistory();
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearSearchHistory() {
        ClearSearchHistoryDialogFragment dialog = new ClearSearchHistoryDialogFragment();
        dialog.show(getFragmentManager(), "ClearSearchHistoryDialogFragment");
    }

    private void showSortListDialog() {
        SortWifiListDialogFragment dialog = new SortWifiListDialogFragment();
        dialog.show(getFragmentManager(), "SortWifiListDialogFragment");
    }

    @Override
    /** Callback from SortWifiListDialog, when called, sort our list of wifis by the choice selected.*/
    public void onDialogClick(int position) {
        /*
         * User clicked a position From the string-array:
		 * R.array.array_sort_wifi_list Order is: Name, Address, Status,
		 * Facility, Distance
		 */
        sortChoice = position;
        switch (position) {
            case 0: // Name
                Collections.sort(wifis, new NameComparator());
                break;
            case 1: // Address
                Collections.sort(wifis, new AddressComparator());
                break;
            case 2: // Facility
                Collections.sort(wifis, new FacilityComparator());
                break;
            case 3: // Distance
                Collections.sort(wifis, new DistanceComparator());
                break;
            default: // Invalid
                return;
        }
        // Only if we got a non-invalid position we will be here
        adapter.notifyDataSetChanged();
    }

    public int getSortChoice() {
        return sortChoice;
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // TODO: Make comparators and sort list
        switch (position) {
            case 1: // Name
                return true;
            case 2: // Status
                return true;
            default: // Unknown
                return false;
        }
    }

    /**
     * Used to compare wifis by name their name in alphabetical order
     */
    public class NameComparator implements Comparator<Wifi> {

        @Override
        public int compare(Wifi w1, Wifi w2) {
            return w1.getName().compareTo(w2.getName());
        }

    }

    /**
     * Used to compare wifis by their address in alphabetical order
     */
    public class AddressComparator implements Comparator<Wifi> {

        @Override
        public int compare(Wifi w1, Wifi w2) {
            return w1.getAddress().compareTo(w2.getAddress());
        }
    }

    /**
     * Used to compare wifis by their Status. This comparator will compare the
     * wifis in alphabetical order.
     * <p/>
     * The order is: ACTIVE, IN_PROGRESS, IN_FUTURE
     */
    public class StatusComparator implements Comparator<Wifi> {
        @Override
        public int compare(Wifi w1, Wifi w2) {
            return w1.getStatusString().compareTo(w2.getStatusString());
        }
    }

    /**
     * Used to compare wifis by their facility type. This comparator will
     * compare the wifis in alphabetical order.
     * <p/>
     * The order is: CITY, TRANSIT
     */
    public class FacilityComparator implements Comparator<Wifi> {
        @Override
        public int compare(Wifi w1, Wifi w2) {
            return w1.getProvider().compareTo(w2.getProvider());
        }
    }

    public class DistanceComparator implements Comparator<Wifi> {
        @Override
        public int compare(Wifi w1, Wifi w2) {
            double d1 = w1.getDistance();
            double d2 = w2.getDistance();

            if (d1 == d2) {
                return 0;
            } else if (d1 != Wifi.INVALID_DISTANCE
                    && d2 == Wifi.INVALID_DISTANCE) {
                // D2 is invalid so d1 must be closer
                return -1;
            } else if (d1 == Wifi.INVALID_DISTANCE
                    && d2 != Wifi.INVALID_DISTANCE) {
                // D1 is invalid so d2 must be closer
                return 1;
            } else {
                return (int) (d1 - d2);
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = new DataFragment();
        Bundle args = new Bundle();
        args.putInt(DataFragment.ARG_DATA_NUMBER, position);
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mDrawerTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any config change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Fragment that appears in main_container, shows list of whatever the data is
     */

    public static class DataFragment extends ListFragment {
        public static final String ARG_DATA_NUMBER = "data_number";

        public DataFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            int i = getArguments().getInt(ARG_DATA_NUMBER);
            String data = getResources().getStringArray(R.array.drawer_array)[i];

            // TODO: Set up whatever goes in the fragment here
            ListView lView;
            lView = (ListView) rootView.findViewById(R.id.main_activity_listview);

          /*  WifiArrayAdapter adapter = new WifiArrayAdapter(getActivity(), wifis);
            lView.setAdapter(adapter);

            lView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Intent i = new Intent(getApplicationContext(),
                            WifiViewActivity.class);
                    i.putExtra(WifiViewActivity.WIFI_ID, position);
                    startActivity(i);
                }
            });

            setupRefreshLocationButton();

            updateLocation();
            // By default let's sort by distance
            Collections.sort(wifis, new DistanceComparator());
            adapter.notifyDataSetChanged();
            setListAdapter(adapter);*/

            return rootView;
        }
    }


}
