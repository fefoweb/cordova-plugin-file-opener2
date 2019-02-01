/*
The MIT License (MIT)

Copyright (c) 2013 pwlin - pwlin05@gmail.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.github.pwlin.cordova.plugins.fileopener2;

import java.io.File;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import io.github.pwlin.cordova.plugins.fileopener2.FileProvider;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaResourceApi;

public class FileOpener2 extends CordovaPlugin {

	public static boolean enabledLog = true;
    public static Integer LOG_DEBUG = 1;
    public static Integer LOG_ERROR = 0;
    private static String TAG = "KioskActivity@FILE";

	/**
	 * Executes the request and returns a boolean.
	 *
	 * @param action
	 *            The action to execute.
	 * @param args
	 *            JSONArry of arguments for the plugin.
	 * @param callbackContext
	 *            The callback context used when calling back into JavaScript.
	 * @return boolean.
	 */
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("open")) {
			String fileUrl = args.getString(0);
			String contentType = args.getString(1);
			Boolean openWithDefault = true;
			if(args.length() > 2){
				openWithDefault = args.getBoolean(2);
			}

			FileOpener2.toAndroidLog("open file # fileUrl: " + fileUrl + " - contentType " + contentType + " - " + openWithDefault + " - " + callbackContext);
			this._open(fileUrl, contentType, openWithDefault, callbackContext);
		}
		else if (action.equals("uninstall")) {
			this._uninstall(args.getString(0), callbackContext);
		}
		else if (action.equals("appIsInstalled")) {
			JSONObject successObj = new JSONObject();
			if (this._appIsInstalled(args.getString(0))) {
				successObj.put("status", PluginResult.Status.OK.ordinal());
				successObj.put("message", "Installed");
			}
			else {
				successObj.put("status", PluginResult.Status.NO_RESULT.ordinal());
				successObj.put("message", "Not installed");
			}
			callbackContext.success(successObj);
		}
		else {
			JSONObject errorObj = new JSONObject();
			errorObj.put("status", PluginResult.Status.INVALID_ACTION.ordinal());
			errorObj.put("message", "Invalid action");
			FileOpener2.toAndroidLog("Invalid action", 0);

			callbackContext.error(errorObj);
		}
		return true;
	}

	private void _open(String fileArg, String contentType, Boolean openWithDefault, CallbackContext callbackContext) throws JSONException {
		String fileName = "";
		try {
			CordovaResourceApi resourceApi = webView.getResourceApi();
			Uri fileUri = resourceApi.remapUri(Uri.parse(fileArg));
			fileName = this.stripFileProtocol(fileUri.toString());

			FileOpener2.toAndroidLog("open file # metodo 1 fileName " + fileName);
		} catch (Exception e) {
			fileName = fileArg;

			FileOpener2.toAndroidLog("open file # metodo 2 fileName " + fileName);
		}
		File file = new File(fileName);
		if (file.exists()) {
			try {
				Intent intent;
				if (contentType.equals("application/vnd.android.package-archive")) {
					// https://stackoverflow.com/questions/9637629/can-we-install-an-apk-from-a-contentprovider/9672282#9672282
					intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
					Uri path;
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
						path = Uri.fromFile(file);
					} else {
						Context context = cordova.getActivity().getApplicationContext();
						path = FileProvider.getUriForFile(context, cordova.getActivity().getPackageName() + ".opener.provider", file);
					}
					FileOpener2.toAndroidLog("APK intent.SetDataAndType # " + path + " " + contentType);

					intent.setDataAndType(path, contentType);
					intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

				} else {
					intent = new Intent(Intent.ACTION_VIEW);
					Context context = cordova.getActivity().getApplicationContext();
					Uri path = FileProvider.getUriForFile(context, cordova.getActivity().getPackageName() + ".opener.provider", file);
					intent.setDataAndType(path, contentType);
					intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
					
					FileOpener2.toAndroidLog("intent.SetDataAndType # " + path + " " + contentType);
				}

				/*
				 * @see
				 * http://stackoverflow.com/questions/14321376/open-an-activity-from-a-cordovaplugin
				 */
				 if(openWithDefault){
					 cordova.getActivity().startActivity(intent);
					 Log.d(TAG, "@FILE open with default");
					 FileOpener2.toAndroidLog("open with default");
				 }
				 else{
					 cordova.getActivity().startActivity(Intent.createChooser(intent, "Open File in..."));
					 FileOpener2.toAndroidLog("open with chooser");
				 }

				callbackContext.success();
			} catch (android.content.ActivityNotFoundException e) {
				JSONObject errorObj = new JSONObject();
				errorObj.put("status", PluginResult.Status.ERROR.ordinal());
				errorObj.put("message", "Activity not found: " + e.getMessage());
				FileOpener2.toAndroidLog("Activity not found: " + e.getMessage(), 0);

				callbackContext.error(errorObj);
			}
		} else {
			JSONObject errorObj = new JSONObject();
			errorObj.put("status", PluginResult.Status.ERROR.ordinal());
			errorObj.put("message", "File not found");
			FileOpener2.toAndroidLog("File not found", 0);

			callbackContext.error(errorObj);
		}
	}

	private void _uninstall(String packageId, CallbackContext callbackContext) throws JSONException {
		if (this._appIsInstalled(packageId)) {
			Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
			intent.setData(Uri.parse("package:" + packageId));
			cordova.getActivity().startActivity(intent);
			callbackContext.success();
		}
		else {
			JSONObject errorObj = new JSONObject();
			errorObj.put("status", PluginResult.Status.ERROR.ordinal());
			errorObj.put("message", "This package is not installed");
			FileOpener2.toAndroidLog("This package " + packageId + " is not installed", 0);

			callbackContext.error(errorObj);
		}
	}

	private boolean _appIsInstalled(String packageId) {
		PackageManager pm = cordova.getActivity().getPackageManager();
        boolean appInstalled = false;
        try {
            pm.getPackageInfo(packageId, PackageManager.GET_ACTIVITIES);
            appInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            appInstalled = false;
        }
        return appInstalled;
	}

	private String stripFileProtocol(String uriString) {
		if (uriString.startsWith("file://")) {
			uriString = uriString.substring(7);
		} else if (uriString.startsWith("content://")) {
			uriString = uriString.substring(10);
		}
		return uriString;
	}

	private static void toAndroidLog(String str, Integer... t) {
        Integer type = t.length > 0 ? t[0] : FileOpener2.LOG_DEBUG;

        if(FileOpener2.enabledLog) {
            if(type == FileOpener2.LOG_DEBUG) {
                Log.d(FileOpener2.TAG, str);
            } else {
                Log.e(FileOpener2.TAG, str);
            }
        }
    }

}
