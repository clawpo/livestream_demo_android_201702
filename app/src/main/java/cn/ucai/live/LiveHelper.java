package cn.ucai.live;

import android.content.Context;
import android.content.Intent;

import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.model.EasePreferenceManager;
import com.hyphenate.util.EMLog;

import cn.ucai.live.data.restapi.LiveException;
import cn.ucai.live.data.restapi.LiveManager;
import cn.ucai.live.ui.activity.MainActivity;

import static com.ucloud.ulive.UStreamingContext.appContext;

/**
 * Created by clawpo on 2017/6/8.
 */

public class LiveHelper {
    private static final String TAG = "LiveHelper";

    private static LiveHelper instance = null;
    private String username;
    LiveModel model = null;
    private User currentAppUser= null;

    private LiveHelper() {
    }

    public synchronized static LiveHelper getInstance() {
        if (instance == null) {
            instance = new LiveHelper();
        }
        return instance;
    }

    public void init(final Context context){
        model = new LiveModel();
        EaseUI.getInstance().init(context, null);
        EMClient.getInstance().setDebugMode(true);

        EMClient.getInstance().addConnectionListener(new EMConnectionListener() {
            @Override public void onConnected() {

            }

            @Override public void onDisconnected(int error) {
                EMLog.d("global listener", "onDisconnect" + error);
                if (error == EMError.USER_REMOVED) {
                    onUserException(LiveConstants.ACCOUNT_REMOVED);
                } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                    onUserException(LiveConstants.ACCOUNT_CONFLICT);
                } else if (error == EMError.SERVER_SERVICE_RESTRICTED) {
                    onUserException(LiveConstants.ACCOUNT_FORBIDDEN);
                }
            }
        });
    }

    /**
     * user met some exception: conflict, removed or forbidden
     */
    protected void onUserException(String exception){
        EMLog.e(TAG, "onUserException: " + exception);
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(exception, true);
        appContext.startActivity(intent);
    }

    /**
     * set current username
     * @param username
     */
    public void setCurrentUserName(String username){
        this.username = username;
        model.setCurrentUserName(username);
    }

    /**
     * get current user's id
     */
    public String getCurrentUsernName(){
        if(username == null){
            username = model.getCurrentUsernName();
        }
        return username;
    }

    public void syncUserInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    User user = LiveManager.getInstance().loadUserInfo(EMClient.getInstance().getCurrentUser());
                    if (user!=null){
                        setCurrentAppUserNick(user.getMUserNick());
                        setCurrentAppUserAvatar(user.getAvatar());
                    }
                } catch (LiveException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void setCurrentAppUserNick(String nickname) {
        getCurrentAppUserInfo().setMUserNick(nickname);
        EasePreferenceManager.getInstance().setCurrentUserNick(nickname);
    }

    private void setCurrentAppUserAvatar(String avatar) {
        getCurrentAppUserInfo().setAvatar(avatar);
        EasePreferenceManager.getInstance().setCurrentUserAvatar(avatar);
    }

    public synchronized User getCurrentAppUserInfo() {
        if (currentAppUser == null) {
            String username = EMClient.getInstance().getCurrentUser();
            currentAppUser = new User(username);
            String nick = getCurrentUserNick();
            currentAppUser.setMUserNick((nick != null) ? nick : username);
            currentAppUser.setAvatar(getCurrentUserAvatar());
        }
        return currentAppUser;
    }

    private String getCurrentUserNick() {
        return EasePreferenceManager.getInstance().getCurrentUserNick();
    }

    private String getCurrentUserAvatar() {
        return EasePreferenceManager.getInstance().getCurrentUserAvatar();
    }

    public synchronized void reset() {
        currentAppUser = null;
        EasePreferenceManager.getInstance().removeCurrentUserInfo();
    }
}