package cordova.plugin.nativeRingtones;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.database.Cursor;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class echoes a string called from JavaScript.
 */
public class NativeRingtones extends CordovaPlugin {

    public static Timer mTimer;
    public static Ringtone ringtone;

    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        if(action.equals("getDefaultRingtone")){
            return getDefaultRingtone(callbackContext);
        }else {
            if (action.equals("get")) {
                return this.get(args.getString(0), callbackContext);
            }
            else
            if (action.equals("play")) {
                return this.play(args.getString(0), callbackContext);
            }
            else
            if (action.equals("stop")) {
                return this.stop(callbackContext);
            }
            else
            if (action.equals("isRingtonePlaying")) {
                return this.isRingtonePlaying(callbackContext);
            }
        }
        return false;
    }

  private boolean get(String ringtoneType, final CallbackContext callbackContext){
      cordova.getThreadPool().execute(() -> {
              RingtoneManager manager = new RingtoneManager(cordova.getActivity().getBaseContext());

              //The default value if ringtone type is "notification"
              if (ringtoneType == "alarm") {
                  manager.setType(RingtoneManager.TYPE_ALARM);
              } else if (ringtoneType == "ringtone"){
                  manager.setType(RingtoneManager.TYPE_RINGTONE);
              } else {
                  manager.setType(RingtoneManager.TYPE_NOTIFICATION);
              }

              Cursor cursor = manager.getCursor();
              JSONArray ringtoneList = new JSONArray();

              while (cursor.moveToNext()) {
                  String notificationTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                  String notificationUri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + cursor.getString(RingtoneManager.ID_COLUMN_INDEX);

                  /****   Transfer Content URI to file URI   ******* /
                   /* String filePath;

                   if (notificationUri != null && "content".equals(notificationUri.getScheme())) {
                   Cursor cursor1 = this.cordova.getActivity().getBaseContext().getContentResolver().query(notificationUri, new String[] {
                   android.provider.MediaStore.Images.ImageColumns.DATA
                   }, null, null, null);
                   cursor1.moveToFirst();
                   filePath = cursor1.getString(0);
                   cursor1.close();
                   } else {
                   filePath = notificationUri.getPath();
                   }*/

                  JSONObject json = new JSONObject();
                  try {
                      json.put("Name", notificationTitle);
                      json.put("Url", notificationUri);
                  } catch (JSONException e) {
                      e.printStackTrace();
                      callbackContext.error("Can't get system Ringtone list");
                  }

                  ringtoneList.put(json);
              }

              if (ringtoneList.length() > 0) {
                  callbackContext.success(ringtoneList);
              } else {
                  callbackContext.error("Can't get system Ringtone list");
              }
      });


        return true;
    }

    private boolean play(String ringtoneUri, final CallbackContext callbackContext){
        cordova.getThreadPool().execute(() -> {
            try {
                if (ringtone != null && ringtone.isPlaying()) {
                    callbackContext.error("Already playing a ringtone!");
                }
                else {
                    ringtone = RingtoneManager.getRingtone(cordova.getActivity(), Uri.parse(ringtoneUri));
                    ringtone.play();
                    mTimer = new Timer();
                    mTimer.scheduleAtFixedRate(new TimerTask() {
                        public void run() {
                            if (!ringtone.isPlaying()) {
                                ringtone.play();
                                Log.d("Stoppato", "Ha stoppato");
                            }
                        }
                    }, 1000 * 1, 1000 * 1);
                }
            }catch (Exception e) {
                e.printStackTrace();
                callbackContext.error("Can't play the ringtone!");
            }
            callbackContext.success("Play the ringtone successfully!");
        });

        return true;
    }

    private boolean stop(final CallbackContext callbackContext){
        cordova.getThreadPool().execute(() -> {
            if(ringtone != null) {
                    mTimer.cancel();  // Terminates this timer, discarding any currently scheduled tasks.
                    mTimer.purge();   // Removes all cancelled tasks from this timer's task queue.
                    ringtone.stop();
                    ringtone = null;
                    callbackContext.success("Ringtone stopped successfully!");
            } else {
                callbackContext.error("Any ringtone found!");
            }
        });

        return true;
    }
    private boolean getDefaultRingtone(final CallbackContext callbackContext){
        cordova.getThreadPool().execute(() -> {
            String path = Settings.System.DEFAULT_RINGTONE_URI.toString();

            if(!path.isEmpty()){
                callbackContext.success(path);
            }
            else{
                callbackContext.error("Not found any default ringtone");
            }
        });

        return true;
    }

    private boolean isRingtonePlaying(final CallbackContext callbackContext){

        cordova.getThreadPool().execute(() -> {
            if(ringtone != null) {
                if (ringtone.isPlaying()) {
                    callbackContext.success(1);
                } else {
                    callbackContext.success(0);
                }
            } else {
                callbackContext.success(0);
            }
        });

        return true;
    }
}
