package eu.mcomputing.cohave.funfi.drawer;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import eu.mcomputing.cohave.authentication.User;
import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.general.AboutFragment;
import eu.mcomputing.cohave.funfi.general.LeaderBoardFragment;
import eu.mcomputing.cohave.funfi.general.ProfileFragment;
import eu.mcomputing.cohave.funfi.general.SettingsFragment;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.fragments.DislikedFragment;
import eu.mcomputing.cohave.funfi.rating.fragments.FunFiFragment;
import eu.mcomputing.cohave.funfi.rating.fragments.LikedFragment;
import eu.mcomputing.cohave.funfi.rating.fragments.PlacesFragment;

/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), mComputing (www.mcomputig.eu)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class DrawerActivity extends AppCompatActivity {

    public static final String[] menuTitles = {"Profil", "FunFi", "Miesta", "Archív","LeaderBoard","Nastavenia", "O aplikácii"};
    public static final String searchTitle = "Hladať";
    public static final int[] menuIcons = {
            R.drawable.ic_account,
            R.drawable.ic_wifi,
            R.drawable.ic_places,
            R.drawable.ic_history,
            R.drawable.ic_chart_bar,
            R.drawable.ic_settings,
            R.drawable.ic_email
    };

    public static final int PROFILE_FRAGMENT = 0;
    public static final int FUNFI_FRAGMENT = 1;
    public static final int PLACES_FRAGMENT = 2;
    public static final int HISTORY_FRAGMENT = 3;
    public static final int LIKED_FRAGMENT = 30;
    public static final int DISLIKED_FRAGMENT = 31;
    public static final int LEADERBOARD_FRAGMENT = 4;
    public static final int SETTING_FRAGMENT = 5;
    public static final int ABOUT_FRAGMENT = 6;


    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    protected CharSequence mTitle = "";
    protected CharSequence mDrawerTitle = "";
    protected ActionBarDrawerToggle mDrawerToggle;
    protected Toolbar topToolBar;
    private int selectedPosition;

    protected void initAuthDrawer(Bundle savedInstanceState) {

        mTitle = mDrawerTitle = getTitle();
        topToolBar = (Toolbar) findViewById(R.id.drawer_toolbar);
        setSupportActionBar(topToolBar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        LayoutInflater inflater = getLayoutInflater();
        View listHeaderView = inflater.inflate(R.layout.drawer_header_list, null, false);

        mDrawerList.addHeaderView(listHeaderView);
        List<MenuItemObject> listViewItems = new ArrayList<MenuItemObject>();
        for (int i = 1; i < menuTitles.length; i++) {
            listViewItems.add(new MenuItemObject(menuTitles[i], menuIcons[i]));
        }

        mDrawerList.setAdapter(new CustomAdapter(this, listViewItems));

        mDrawerToggle = new ActionBarDrawerToggle(DrawerActivity.this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // make Toast when click
                MyLog.log(getClass(), "Position " + position);
                selectItemFragment(position);
            }
        });

        if (savedInstanceState == null) {
            selectItemFragment(FUNFI_FRAGMENT);
        }

    }


    protected void selectItemFragment(int position) {
        selectedPosition = position;

        if (position == SETTING_FRAGMENT){
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.main_fragment_container, new SettingsFragment()).commit();

            mDrawerList.setItemChecked(SETTING_FRAGMENT, true);
            mDrawerLayout.closeDrawer(mDrawerList);
            setTitle(menuTitles[SETTING_FRAGMENT]);
            MyLog.log(getClass(), "select fragment " + position + " - " + menuTitles[SETTING_FRAGMENT]);
            return;
        }

        String title = "";
        Fragment fragment = null;
        switch (position) {
            default:
            case FUNFI_FRAGMENT:
                fragment = new FunFiFragment();
                title = menuTitles[position];
                break;
            case PLACES_FRAGMENT:
                fragment = new PlacesFragment();
                title = menuTitles[position];
                break;
            case HISTORY_FRAGMENT:
            case LIKED_FRAGMENT:
                fragment = new LikedFragment();
                title = getResources().getString(R.string.rating_like);
                break;
            case DISLIKED_FRAGMENT:
                fragment = new DislikedFragment();
                title = getResources().getString(R.string.rating_dislike);
                break;
            case ABOUT_FRAGMENT:
                fragment = new AboutFragment();
                title= menuTitles[ABOUT_FRAGMENT];
                break;
            case PROFILE_FRAGMENT:
                fragment = new ProfileFragment();
                title = menuTitles[PROFILE_FRAGMENT];
                break;
            case LEADERBOARD_FRAGMENT:
                fragment = new LeaderBoardFragment();
                title = menuTitles[LEADERBOARD_FRAGMENT];
                break;
        }

        Bundle args = new Bundle();
        fragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.main_fragment_container, fragment).commit();

        int reducedPos = position >= 10 ? position / 10 : position;
        if (reducedPos >= 0 && reducedPos < 10) {
            mDrawerList.setItemChecked(reducedPos, true);
            mDrawerLayout.closeDrawer(mDrawerList);
        }

        setTitle(title);
        MyLog.log(getClass(), "select fragment " + position + " - " + title);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle!=null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle!=null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MyLog.log(getClass(), "creating menu " + selectedPosition);


        int reducedPos = selectedPosition >= 10 ? selectedPosition / 10 : selectedPosition;
        if (reducedPos == HISTORY_FRAGMENT) {
            getMenuInflater().inflate(R.menu.history_menu, menu);
        }
        if (reducedPos == FUNFI_FRAGMENT) {
            getMenuInflater().inflate(R.menu.menu_main, menu);

            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        if (mDrawerLayout!=null) {
            boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
            MyLog.log(getClass(), "drawer is " + drawerOpen);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        MyLog.log(getClass(), "onOptionsItemSelected");

        if (mDrawerToggle!=null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        int reducedPos = selectedPosition >= 10 ? selectedPosition / 10 : selectedPosition;
        if (reducedPos == HISTORY_FRAGMENT) {
            switch (id) {
                default:
                case R.id.action_like:
                    selectItemFragment(LIKED_FRAGMENT);
                    break;
                case R.id.action_dislike:
                    selectItemFragment(DISLIKED_FRAGMENT);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    protected void setAppUIForUser(AccountManager acm, Account account) {
        TextView name = (TextView) findViewById(R.id.profile_name);
        TextView email = (TextView) findViewById(R.id.profile_email);

        String first_name = acm.getUserData(account,User.first_name_field);
        String last_name = acm.getUserData(account,User.last_name_field);

        name.setText(first_name+" "+last_name);
        email.setText(account.name);

    }

}
