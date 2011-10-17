/**
 * 
 */
package com.phonegap.plugin.facebook;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;
import com.facebook.android.*;
import com.facebook.android.Facebook.*;


/**
 * @author mmihok
 *
 */
public class FacebookGapPlugin extends Plugin {

	//public static String FACEBOOK_APP_ID = "";
	public Facebook facebook;
	private SharedPreferences preferences;
	
	/* (non-Javadoc)
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		PluginResult result = new PluginResult(Status.NO_RESULT);
		result.setKeepCallback(true);
		//JSONObject response = new JSONObject();
		String accessToken = "0";
		long expires;
		
		Log.d("FacebookGapPlugin","Plugin called");
		
		// Check if app id is provided
		preferences = ctx.getPreferences(Context.MODE_PRIVATE);
		
		// Check if authorization exists in phone, SSO
		accessToken = preferences.getString("access_token", null);
        expires = preferences.getLong("access_expires", 0);
		
        // Create facebook object
    	try {
			facebook = new Facebook(data.getString(0));
			Log.d("FacebookGapPlugin", "Initializing Facebook(" + facebook.getAppId() + ") object");
		} catch(JSONException ex) {
			Log.d("FacebookGapPlugin", "JSONError: " + ex.getMessage());
		}

        if(accessToken != null) {
            facebook.setAccessToken(accessToken);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }
		
        if(action.equals("oauth")) {
        	Log.d("FacebookGapPlugin", "Attempting to authorize");
        	if(facebook != null) {
        		if(facebook.isSessionValid()) {
        			Log.d("FacebookGapPlugin", "Already authorized, skipping...");
        			result = new PluginResult(Status.OK);
        		} else {
        			final String[] permissions = new String[data.length() - 1];
        			try {
        				for(int i = 0; i < (data.length() - 1); i++) {
        					permissions[i] = data.getString(i + 1);
        				}
        			} catch(JSONException ex) {
        				Log.d("FacebookGapPlugin", "JSON Error: " + ex.getMessage());
        				result = new PluginResult(Status.JSON_EXCEPTION);
        			}
        			
        			ctx.setActivityResultCallback(this);
        			final FacebookGapPlugin me = this;
        			
        			new Thread(new Runnable() {
        			    public void run() {
        			    	me.webView.post(new Runnable(){
    			    			public void run() {
    			    				me.facebook.authorize(me.ctx, permissions, 1234567890, new DialogListener() {
    			    					@Override
    			    					public void onComplete(Bundle values) {
										   Log.d("FacebookGapPlugin", "Facebook authorized [" + facebook.getAccessToken() + "]");
										   SharedPreferences.Editor editor = preferences.edit();
										   editor.putString("access_token", facebook.getAccessToken());
										   editor.putLong("access_expires", facebook.getAccessExpires());
										   editor.commit();
    			    					}
									
    			    					@Override
    			    					public void onFacebookError(FacebookError error) {
										   Log.d("FacebookGapPlugin", "Facebook Error: " + error.getMessage());
    			    					}
									
    			    					@Override
    			    					public void onError(DialogError e) {
										   Log.d("FacebookGapPlugin", "Facebook Error: " + e.getMessage());
    			    					}
									
    			    					@Override
    			    					public void onCancel() {}
    			    				});
    			    			}
        			    	});
        			    }
        			}).start();
        		}
        	} else {
        		
        	}
        }
		
		return result;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
        facebook.authorizeCallback(requestCode, resultCode, intent);
	}
}
