package com.baymax.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;

/**
 * @author oukanggui
 * @date 2018/11/7
 * @describe  ToastUtil：
 * 1、Toast manager：Design single Toast object to show non-blocking Toast and cancel Toast easily
 * 2、Fix the BadTokenException happened on the device 7.x while showing Toast which will cause your app to crash
 */

public class ToastUtil {
    private static final String TAG = "ToastUtil";
    private static Toast mToast;
    private static Field sField_TN;
    private static Field sField_TN_Handler;
    private static boolean sIsHookFieldInit = false;
    private static final String FIELD_NAME_TN = "mTN";
    private static final String FIELD_NAME_HANDLER = "mHandler";

    /**
     * Non-blocking showing Toast
     * @param context  context，Application or Activity
     * @param text     the text show on the Toast
     * @param duration Toast.LENGTH_SHORT（default,2s） or Toast.LENGTH_LONG（3.5s）
     */
    public static void showToast(final Context context, final CharSequence text, final int duration) {
        ToastRunnable toastRunnable = new ToastRunnable(context, text, duration);
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            if (activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(toastRunnable);
            }
        } else {
            Handler handler = new Handler(context.getMainLooper());
            handler.post(toastRunnable);
        }
    }

    /**
     * Non-blocking showing Toast,default duration is Toast.LENGTH_SHORT
     * @param context  context，Application or Activity
     * @param text     the text show on the Toast
     */
    public static void showToast(Context context, CharSequence text) {
        showToast(context, text, Toast.LENGTH_SHORT);
    }

    /**
     * cancel the toast
     */
    public static void cancelToast() {
        Looper looper = Looper.getMainLooper();
        if (looper.getThread() == Thread.currentThread()) {
            mToast.cancel();
        } else {
            new Handler(looper).post(new Runnable() {
                @Override
                public void run() {
                    mToast.cancel();
                }
            });
        }
    }

    /**
     * Hook Toast,fix the BadTokenException happened on the device 7.x while showing Toast which will cause your app to crash
     *
     * @param toast
     */
    private static void hookToast(Toast toast) {
        if (!isNeedHook()) {
            return;
        }
        try {
            if (!sIsHookFieldInit) {
                sField_TN = Toast.class.getDeclaredField(FIELD_NAME_TN);
                sField_TN.setAccessible(true);
                sField_TN_Handler = sField_TN.getType().getDeclaredField(FIELD_NAME_HANDLER);
                sField_TN_Handler.setAccessible(true);
                sIsHookFieldInit = true;
            }
            Object tn = sField_TN.get(toast);
            Handler originHandler = (Handler) sField_TN_Handler.get(tn);
            sField_TN_Handler.set(tn, new SafelyHandlerWarpper(originHandler));
        } catch (Exception e) {
            Log.e(TAG, "Hook toast exception=" + e);
        }
    }

    /**
     * Check if Toast need hook，only hook the device 7.x(api = 24/25)
     *
     * @return true for need hook to fit system bug,false for don't need hook
     */
    private static boolean isNeedHook() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.N;
    }

    private static class ToastRunnable implements Runnable {
        private Context context;
        private CharSequence text;
        private int duration;

        public ToastRunnable(Context context, CharSequence text, int duration) {
            this.context = context;
            this.text = text;
            this.duration = duration;
        }

        @Override
        public void run() {
            if (mToast == null) {
                mToast = Toast.makeText(context, text, duration);
            } else {
                mToast.setText(text);
                mToast.setDuration(duration);
            }
            hookToast(mToast);
            mToast.show();
        }
    }

    /**
     * Safe outside Handler class which just warps the system origin handler object in the Toast.class
     */
    private static class SafelyHandlerWarpper extends Handler {
        private Handler originHandler;

        public SafelyHandlerWarpper(Handler originHandler) {
            this.originHandler = originHandler;
        }

        @Override
        public void dispatchMessage(Message msg) {
            // The outside hanlder SafelyHandlerWarpper object just catches the Exception while dispatch the message
            // if the the inside system origin hanlder object throw the BadTokenException，the outside safe SafelyHandlerWarpper object
            // just catches the exception here to avoid the app crashing
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
                Log.e(TAG, "Catch system toast exception:" + e);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            //just pass the Message to the origin handler object to handle
            if (originHandler != null) {
                originHandler.handleMessage(msg);
            }
        }
    }
}
