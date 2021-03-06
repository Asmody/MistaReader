package com.mistareader;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mistareader.NavigationDrawer.DropDownNav;
import com.mistareader.NavigationDrawer.NavDrawer_Main;
import com.mistareader.NavigationDrawer.NavDrawer_MenuItem;

public class Topics_Activity extends BaseActivity implements Topics_Fragment.OnTopicSelectedListener, Topics_Fragment.OnUnsubscribeListener,
        OnNavigationListener, Forum.iOnPOSTRequestExecuted, Forum.iOnLoggedIn {

    NavDrawer_Main    mND;
    DropDownNav       ddN;
    Forum             forum;
    Topics_Fragment   topics_Fragment;

    int               selectedForumPosition     = 1;
    int               selectedSectionPosition   = 0;
    String            selectedForumName         = "";
    String            selectedSectionName       = "";
    boolean           isInternetConnection;
    boolean           isOpenedFromNotifications = false;

    BroadcastReceiver subscriptionsBroadcastReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sPref = getPreferences(MODE_PRIVATE);
        ThemesManager.CurrentTheme = sPref.getInt(ThemesManager.SETTINGS_THEME, ThemesManager.THEME_DEFAULT);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        init_CreateForum(sPref);
        init_RegisterBroadcastReciever();
        init_ProcessNotifications();

        if (isOpenedFromNotifications) {
            init_CreateNavigationDrawer();
        }
        else {
            init_LoadSavedInstaces(savedInstanceState, sPref);
            init_CreateNavigationDrawer();
            init_CreateTopicsFragment();
            init_CreateActionBarNavigation();

        }

    }

    private void init_ProcessNotifications() {

        Intent inpIntent = getIntent();
        if (inpIntent.hasExtra(Subscriptions.NOTIFICATIONS_EXTRA_ID)) {

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(Subscriptions.NOTIFICATIONS_UNIQUE_ID);

            Bundle extras = inpIntent.getExtras();
            if (extras != null) {

                boolean isMultipleSubs = extras.getBoolean(Subscriptions.NOTIFICATIONS_EXTRA_IS_MULTIPLE, false);
                long curTopicId = extras.getLong(Subscriptions.NOTIFICATIONS_EXTRA_TOPIC_ID);
                int curTopicAnsw = extras.getInt(Subscriptions.NOTIFICATIONS_EXTRA_TOPIC_ANSW);
                inpIntent.removeExtra(Subscriptions.NOTIFICATIONS_EXTRA_ID);

                if (isMultipleSubs) {
                    selectedForumName = NavDrawer_Main.MENU_SUBSCRIPTIONS;
                }
                else {

                    isOpenedFromNotifications = true;
                    Topic currentTopic = Forum.getInstance().getTopicByid(curTopicId);
                    if (currentTopic == null) {
                        currentTopic = new Topic();
                        currentTopic.id = curTopicId;
                        currentTopic.answ = curTopicAnsw;
                        forum.topics.add(currentTopic);
                    }
                    else
                        currentTopic.answ = curTopicAnsw;

                    Intent intent = new Intent();
                    intent.setClass(this, Messages_Activity.class);
                    intent.putExtra("topicId", curTopicId);
                    intent.putExtra("section", getString(R.string.sSubscriptions));
                    intent.putExtra("focusOn", forum.mainDB.getLastPositionForMessage(curTopicId));

                    startActivity(intent);

                    new markAsReadedAsync().execute(curTopicId);

                }
            }
        }
    }

    private void init_CreateActionBarNavigation() {
        ddN = new DropDownNav();
        ddN.reBuildSubmenu(this, selectedForumName, selectedSectionPosition);
    }

    private void init_CreateNavigationDrawer() {
        mND = new NavDrawer_Main(Topics_Activity.this, forum.accountName, selectedForumPosition, !forum.sessionID.isEmpty());
        mND.mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private void init_CreateTopicsFragment() {
        topics_Fragment = (Topics_Fragment) getFragmentManager().findFragmentByTag("TOPICS");
        if (topics_Fragment == null)
            createTopicsFragment(true);
    }

    private void init_CreateForum(SharedPreferences sPref) {
        isInternetConnection = WebIteraction.isInternetAvailable(this);
        if (forum == null) {
            forum = Forum.getInstance();
            forum.loadSettings(sPref);
            forum.initialize(isInternetConnection, this);
        }
    }

    private void init_LoadSavedInstaces(Bundle savedInstanceState, SharedPreferences sPref) {

        if (savedInstanceState != null) {
            selectedForumName = savedInstanceState.getString("forum", "");
            selectedSectionName = savedInstanceState.getString("section", "");
            selectedForumPosition = savedInstanceState.getInt("selectedForumPos", 1);
            selectedSectionPosition = savedInstanceState.getInt("selectedSectionPos", 0);
        }
        else {
            forum.showWhatsNew(sPref, this);
            forum.delayedLogin(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // После recreate() открывает Drawer. Не знаю почему.
        if (mND.mDrawerLayout.isDrawerOpen(mND.mDrawerList)) {
            mND.mDrawerLayout.closeDrawer(mND.mDrawerList);
            mND.mDrawerToggle.syncState();
        }

        if (!isOpenedFromNotifications && topics_Fragment == null) {
            init_CreateTopicsFragment();
            init_CreateActionBarNavigation();
        }

    }

    @Override
    protected void onPause() {
        isOpenedFromNotifications = false;
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("forum", selectedForumName);
        outState.putString("section", selectedSectionName);
        outState.putInt("selectedForumPos", selectedForumPosition);
        outState.putInt("selectedSectionPos", selectedSectionPosition);

    }

    private void createTopicsFragment(boolean createNewFragment) {

        topics_Fragment = new Topics_Fragment();

        Bundle args = new Bundle();

        args.putString("sForum", selectedForumName);
        args.putString("sSection", selectedSectionName);
        args.putBoolean("isOpenedFromNotifications", isOpenedFromNotifications);

        topics_Fragment.setArguments(args);

        if (createNewFragment) {
            getFragmentManager().beginTransaction().add(R.id.content_frame, topics_Fragment, "TOPICS").commit();
        }
        else {
            getFragmentManager().beginTransaction().replace(R.id.content_frame, topics_Fragment, "TOPICS").commit();
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem mi_addNew = menu.findItem(R.id.menu_add);
        MenuItem mi_mark = menu.findItem(R.id.menu_markAll);
        if (forum.sessionID == null || forum.sessionID.isEmpty() || selectedForumName == NavDrawer_Main.MENU_SUBSCRIPTIONS) {
            mi_addNew.setVisible(false);
        }
        else {
            mi_addNew.setVisible(true);
        }
        if (selectedForumName == NavDrawer_Main.MENU_SUBSCRIPTIONS) {
            mi_mark.setVisible(true);
        }
        else {
            mi_mark.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            mND.mDrawerToggle.onOptionsItemSelected(item);
            return true;
        }
        else if (id == R.id.menu_reload) {
            isInternetConnection = WebIteraction.isInternetAvailable(Topics_Activity.this);
            forum.isInternetConnection = isInternetConnection;

            if (isInternetConnection && topics_Fragment != null) {
                if (selectedForumName.equals(NavDrawer_Main.MENU_SUBSCRIPTIONS)) {
                    Intent serviceIntent = new Intent(Topics_Activity.this, Subscriptions.class);
                    serviceIntent.putExtra(Subscriptions.NOTIFICATIONS_EXTRA_RELOAD_MODE, true);

                    startService(serviceIntent);
                }
                else
                    topics_Fragment.reLoad();
            }

            return true;
        }
        else if (id == R.id.menu_add) {
            forum.addNewTopic(Topics_Activity.this);
            return true;
        }
        else if (id == R.id.menu_markAll) {
            forum.mainDB.markAllSubscriptionsAsReaded();

            rebuildNavDrawer();

            isInternetConnection = WebIteraction.isInternetAvailable(Topics_Activity.this);
            forum.isInternetConnection = isInternetConnection;

            if (isInternetConnection && topics_Fragment != null) {
                topics_Fragment.reLoad();
            }

            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Forum.ACTIVITY_RESULT_NEWTOPIC:

                if (data == null || resultCode != RESULT_OK) {
                    return;
                }

                Bundle args = data.getExtras();
                if (args != null) {

                    String commandName = args.getString("commandName", null);
                    if (commandName == null) {
                        return;
                    }

                    if (commandName.equals(Forum.COMMAND_CREATE_NEW_TOPIC)) {
                        forum.createNewTopic(Topics_Activity.this, args);
                    }

                    break;

                }

            case Forum.ACTIVITY_RESULT_SETTINGS:
                if (data == null)
                    return;

                boolean isLoginChanged = data.getBooleanExtra("isLoginChanged", false);
                boolean isThemeChanged = data.getBooleanExtra("isThemeChanged", false);
                boolean isSubscriptionChanged = data.getBooleanExtra("isSubscriptionChanged", false);

                if (isSubscriptionChanged) {
                    Subscriptions.updateNotifications(Topics_Activity.this);
                }

                if (isLoginChanged) {
                    onLoggedIn(!forum.sessionID.isEmpty());
                }

                if (isThemeChanged) {
                    recreate();
                }

                break;
            default:
                break;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_DOWN) {
            mND.mDrawerLayout.openDrawer(Gravity.LEFT);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {

        if (selectedForumName == NavDrawer_Main.MENU_SUBSCRIPTIONS) {

            selectedForumPosition = 1;
            mND.mSelectedPosition = selectedForumPosition;
            selectedForumName = mND.getSelectedItemID();

            invalidateOptionsMenu();
            openSelectedForum();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {

        LocalBroadcastManager.getInstance(Topics_Activity.this).unregisterReceiver(subscriptionsBroadcastReciever);

        forum = Forum.getInstance();
        if (forum != null) {
            SharedPreferences sPref = getPreferences(MODE_PRIVATE);
            forum.saveSettings(sPref);
            forum.mainDB.close();
        }

        super.onDestroy();

    }

    @Override
    public void onTopicSelected(Topic selectedTopic, boolean focusLast, boolean forceFirst) {

        if (!isInternetConnection) {
            return;
        }

        Intent intent = new Intent();
        intent.setClass(this, Messages_Activity.class);
        intent.putExtra("topicId", selectedTopic.id);
        intent.putExtra("section", selectedTopic.sect1);
        intent.putExtra("forum", selectedTopic.forum);
        intent.putExtra("focusLast", focusLast);
        if (!forceFirst) {
            intent.putExtra("focusOn", forum.mainDB.getLastPositionForMessage(selectedTopic.id));
        }

        startActivity(intent);

        new markAsReadedAsync().execute(selectedTopic.id);

    }

    public class markAsReadedAsync extends AsyncTask<Long, Void, Boolean> {
        boolean mIsSubscriptionPage;

        @Override
        protected void onPostExecute(Boolean isTopicMarked) {

            if (!isTopicMarked) {
                return;
            }

            rebuildNavDrawer();
            if (mIsSubscriptionPage) {
                if (topics_Fragment != null) {
                    topics_Fragment.reLoad();
                }
            }

        }

        @Override
        protected Boolean doInBackground(Long... arg0) {

            Long topicId = arg0[0];

            mIsSubscriptionPage = selectedForumName.equals(NavDrawer_Main.MENU_SUBSCRIPTIONS);

            if (mIsSubscriptionPage || forum.mainDB.isTopicInSubscriptions(topicId)) {
                forum.mainDB.markTopicAsReaded(topicId, 0);
                return true;
            }

            return false;
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mND.mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mND.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            NavDrawer_MenuItem selectedMenu = mND.getMenuItem(position);

            if (!selectedMenu.isExpandable) {

                String menuId = selectedMenu.id;

                switch (menuId) {
                    case NavDrawer_Main.MENU_SETTINGS:
                        startActivityForResult(new Intent(Topics_Activity.this, Settings_Activity.class), Forum.ACTIVITY_RESULT_SETTINGS);
                        break;

                    case NavDrawer_Main.MENU_LOGOFF:

                        forum.sessionID = "";
                        invalidateOptionsMenu();
                        rebuildNavDrawer();

                        break;

                    case NavDrawer_Main.MENU_ABOUT:

                        forum.showAbout(Topics_Activity.this);
                        break;

                    case NavDrawer_Main.MENU_SUBSCRIPTIONS:

                        selectedForumName = NavDrawer_Main.MENU_SUBSCRIPTIONS;
                        selectedForumPosition = position;
                        mND.mSelectedPosition = selectedForumPosition;

                        final ActionBar actionBar = getActionBar();
                        actionBar.setDisplayShowTitleEnabled(true);

                        actionBar.setTitle(R.string.sSubscriptions);
                        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

                        invalidateOptionsMenu();
                        createTopicsFragment(false);
                        break;

                    default:

                        selectedForumPosition = position;
                        mND.mSelectedPosition = selectedForumPosition;
                        selectedForumName = mND.getSelectedItemID();

                        invalidateOptionsMenu();
                        openSelectedForum();
                        break;
                }

            }
            else {

                // mND.setItemChecked(mND.mSelectedPosition);

                if (selectedMenu.isExpandable) {

                    selectedMenu.isExpandable = false;
                    selectedMenu.icon = R.drawable.ic_action_expand;
                    selectedMenu.label = getString(R.string.sNavDrawerLess);

                    mND.collapseMenu(position, selectedMenu.forum);
                    mND.mListAdapter.notifyDataSetChanged();
                }
                else {

                    selectedMenu.setIsExpanded(true);
                    selectedMenu.icon = R.drawable.ic_action_collapse;
                    selectedMenu.label = getString(R.string.sNavDrawerLess);

                    mND.expandMenu(position, selectedMenu.forum);
                    mND.mListAdapter.notifyDataSetChanged();
                }

            }

            mND.mDrawerLayout.closeDrawer(mND.mDrawerList);
        }
    }

    public void openSelectedForum() {

        selectedSectionPosition = 0;
        selectedSectionName = "";

        ddN = new DropDownNav();
        ddN.reBuildSubmenu(this, selectedForumName, selectedSectionPosition);

        createTopicsFragment(false);

    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {

        if (selectedSectionPosition == position) {
            return false;
        }

        selectedSectionPosition = position;

        if (selectedForumName.equals(API.TOPICS_WITH_ME)) {
            if (selectedSectionPosition == 1)
                selectedSectionName = API.MYTOPICS;
            else
                selectedSectionName = API.TOPICS_WITH_ME;
        }
        else {
            if (selectedSectionPosition != 0)
                selectedSectionName = ddN.shortSectionsNamesList.get(selectedSectionPosition);
            else
                selectedSectionName = "";
        }
        createTopicsFragment(false);

        return true;

    }

    @Override
    public void onUnsubscribe() {
        rebuildNavDrawer();

    }

    private void rebuildNavDrawer() {
        mND.mListAdapter.notifyDataSetInvalidated();
        mND.buildMenu(Topics_Activity.this);
        mND.mListAdapter.notifyDataSetChanged();
    }

    private void init_RegisterBroadcastReciever() {
        IntentFilter intFilt = new IntentFilter(Settings.SUBSCRIPTIONS_UPDATED_BROADCAST);
        subscriptionsBroadcastReciever = new subscriptionsResponseReceiver();
        LocalBroadcastManager.getInstance(Topics_Activity.this).registerReceiver(subscriptionsBroadcastReciever, intFilt);
    }

    private class subscriptionsResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            rebuildNavDrawer();

            if (selectedForumName == NavDrawer_Main.MENU_SUBSCRIPTIONS) {
                if (topics_Fragment != null) {
                    topics_Fragment.reLoad();
                }
            }
        }
    }

    @Override
    public void onPOSTRequestExecuted(String result) {

        topics_Fragment.reLoad();

    }

    @Override
    public void onLoggedIn(boolean isLoggedIn) {

        if (mND.mIsLoggedIn == isLoggedIn) {
            return;
        }

        invalidateOptionsMenu();
        mND.mCurrentAccout = forum.accountName;
        mND.mIsLoggedIn = isLoggedIn;
        rebuildNavDrawer();

    }

}
