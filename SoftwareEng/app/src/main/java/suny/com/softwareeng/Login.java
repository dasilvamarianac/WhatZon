package suny.com.softwareeng;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AppEventsLogger;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.LoginButton;

import java.util.HashMap;
import java.util.Map;

public class Login extends FragmentActivity {
    private LoginButton loginButton;
    private PendingAction pendingAction = PendingAction.NONE;
    private ViewGroup controlsContainer;
    private GraphUser user;
    public Context context;
    public static String name,id;

    private enum PendingAction {
        NONE,
    }
    private UiLifecycleHelper uiHelper;

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
        @Override
        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
            Log.d("WhatZon", String.format("Error: %s", error.toString()));
        }
        @Override
        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            Log.d("WhatZon", "Success!");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                Login.this.user = user;
                Session session = Session.getActiveSession();
                if(session.isOpened()) {
                   updateUI();
                    handlePendingAction();
                }
            }
        });
        controlsContainer = (ViewGroup) findViewById(R.id.main_ui_container);
        final FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            // If we're being re-created and have a fragment, we need to a) hide the main UI controls and
            // b) hook up its listeners again.
            controlsContainer.setVisibility(View.GONE);
        }
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fm.getBackStackEntryCount() == 0) {
                    // We need to re-show our UI.
                    controlsContainer.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);
        updateUI();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception){
        if (pendingAction != PendingAction.NONE &&
                (exception instanceof FacebookOperationCanceledException ||
                        exception instanceof FacebookAuthorizationException)){
            new AlertDialog.Builder(Login.this)
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED){
            handlePendingAction();
        }
        if (session.isOpened()) {

            getUserData(session, session.getState());
            final Context context = this;
            Intent intent = new Intent(context, Events.class);
            startActivity(intent);
        }

    }
    private void getUserData(Session session, SessionState state){
        if (state.isOpened()){
            Request.newMeRequest(session, new Request.GraphUserCallback(){
                @Override
                public void onCompleted(GraphUser user, Response response){
                    if (response != null){
                        try{
                             name = user.getName();
                             id = user.getId();

                            String url = "http://moxie.cs.oswego.edu:19991/whatzon/saveUser";
                            final RequestQueue queue1 = Volley.newRequestQueue(context);
                            StringRequest sr = new StringRequest(com.android.volley.Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {

                                }
                            }, new com.android.volley.Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                }
                            }) {
                                @Override
                                protected Map<String, String> getParams() {
                                    Map<String, String> params = new HashMap<String, String>();

                                    params.put("facebook", id);
                                    params.put("picture", "NoTNeed");
                                    params.put("tags", "Music,Food,Politics,Party,Education.");
                                    params.put("min_distance", "0");
                                    params.put("max_distance", "10");
                                    params.put("time","4");
                                    return params;
                                }
                            };
                            queue1.add(sr);

                            setID(user.getId());

                        }
                        catch (Exception e){
                            e.printStackTrace();
                            Log.d("LOGIN INFO", e.toString());
                        }
                    }
                }
            }).executeAsync();
        }
    }
    private void updateUI() {
        Session session = Session.getActiveSession();
        getUserData(session, session.getState());
       if (session.isOpened()) {
            final Context context = this;
            Intent intent = new Intent(context, Events.class);
            startActivity(intent);
        }


    }
    public void setID(String id){
        this.id=id;
    }
    public String getId(){
        return this.id;
    }
    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() {

        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;
    }
}
