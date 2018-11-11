# ToastUtil
#[中文版请见](https://blog.csdn.net/okg0111/article/details/83957680)<br>
**ToastUtil which fixes the "WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@ba9eb53 is not valid; is your activity running?" happened on the device 7.x while showing Toast which will cause your app crashing, just like this<br>**
```java 
android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@ba9eb53 is not valid; is your activity running?
at android.view.ViewRootImpl.setView(ViewRootImpl.java:679)
at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:342)
at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:94)
at android.widget.Toast$TN.handleShow(Toast.java:459)
at android.widget.Toast$TN$2.handleMessage(Toast.java:342)
at android.os.Handler.dispatchMessage(Handler.java:102)
at android.os.Looper.loop(Looper.java:158)
at android.app.ActivityThread.main(ActivityThread.java:6175)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:893)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:783)
```
## Dependency
First, add JitPack repository in your root build.gradle at the end of repositories:
```java
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
		}
	}
```
Second, add the dependency in your module build.gradle:
```java
dependencies {
	implementation 'com.github.oukanggui:ToastUtil:v1.0'
     }
```
## How the BadTokenException occurs
As we can see the invoked stack information on the above Exception message,the Exception occurs while the Hanlder inside TN receives the message and invokes Toast$TN.handleShow() method, the implementation of Toast$TN.handleShow() method is different from different Android SDK version<br>
  **The source system code in Android 7.x:**
```java
public void handleShow(IBinder windowToken) {
            if (localLOGV) Log.v(TAG， "HANDLE SHOW: " + this + " mView=" + mView
                    + " mNextView=" + mNextView);
            if (mView != mNextView) {
                ...
                mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                ....
                mParams.token = windowToken;
                ...
                mWM.addView(mView， mParams);
                ...
            }
        }
```
  **The source system code in Android 8.0:**
```java
public void handleShow(IBinder windowToken) {
            if (localLOGV) Log.v(TAG， "HANDLE SHOW: " + this + " mView=" + mView
                    + " mNextView=" + mNextView);
            if (mView != mNextView) {
                ...
                mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                ....
                mParams.token = windowToken;
                ...
                try {
                    mWM.addView(mView， mParams);
                    trySendAccessibilityEvent();
                } catch (WindowManager.BadTokenException e) {
                    /* ignore */
                }
                ...
            }
        }
```
  As we can see above,Google has realized the bug and just fixed it with catching the BadTokenException from Android 8.0 to avoid your app crashing.<br>
  The Exception is in low probability to accur while our app is in normal use.But when our app is tested with running Monkey,it is in high probability to accur and causes our app to crash.The reason why the Exception occurs maybe UI thread is handling time-consuming task ,eg,network request、database accessing,whick will block our UI thread and cause the message can't be handled by Hanlder in Toast$TN on time.Details can be seen in the following article:<br> 
 [[Android] Toast问题深度剖析(一)](http://www.cnblogs.com/qcloud1001/p/8421356.html)

## How it works
As analyzed above, we can catch the Exception which threw in Toast$TN.handleShow() method,and the method is invoked in Toast$TN$Hanlder.handleMessage() method, and before invoking the method Hanlder.handleMessage(),it will invoke Handler.dispatchMessage() method.So we can create a safe Hanlder object which overrides Handler.dispatchMessage() method and catches the Exception in it:
```java
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
 ```
 And then we need to use SafelyHandlerWarpper object to wrap Toast$TN$Hanlder object and replace the Toast$TN$Hanlder object with reflecting, as the method hookToast() shown below:
```java
    private static final String FIELD_NAME_TN = "mTN";
    private static final String FIELD_NAME_HANDLER = "mHandler";
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
 ```
 And we just choose the hook the Toast$TN$Hanlder object with SafelyHandlerWarpper object in Android 7.x device,whick SDK version is just 24 or 25, as the method isNeedHook() shown below:
```java  
  /**
     * Check if Toast need hook，only hook the device 7.x(api = 24/25)
     *
     * @return true for need hook to fit system bug,false for don't need hook
     */
    private static boolean isNeedHook() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.N;
    }
```
Last, before we show the Toast,we just hook the Toast with the method hookToast():
```java 
       if (mToast == null) {
                mToast = Toast.makeText(context, text, duration);
            } else {
                mToast.setText(text);
                mToast.setDuration(duration);
            }
            hookToast(mToast);
            mToast.show();
        }
```
## How to reappear and check fix
We can reappear the case easily with Thread.sleep() in main thread after showing the toast which will block the main thread and cause the Token invalid：
```java 
                Toast.makeText(MainActivity.this,"I am origin Toast without fix",Toast.LENGTH_SHORT).show();
                try {
                    // just sleep and block the main thread which will reappear the BadTokenException
                    Thread.sleep(10000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
```
As we can see at above code, the main thread will just sleep 10 seconds which will block the main thread.
The demo UI is designed as bolew:<br>
![](https://github.com/oukanggui/ToastUtil/blob/master/app/src/main/assets/main_test_ui.png)<br>
And the code runs as below when we click the first button,it just shows the toast with System Toast:
```java
btnUnfixed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"I am origin Toast without fix",Toast.LENGTH_SHORT).show();
                try {
                    // just sleep and block the main thread which will reappear the BadTokenException
                    Thread.sleep(10000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
```
And when we click the first button and after 10 seconds, the app just crashed as shown below:<br>
![](https://github.com/oukanggui/ToastUtil/blob/master/app/src/main/assets/toast_without_fix.png)<br>
And the code runs as below when we click the second button, it showed the toast with ToastUtil which we have fit the System bug above:
```java
btnFixed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // just sleep and block the main thread which will reappear the BadTokenException
                ToastUtil.showToast(MainActivity.this,"I am fixed Toast");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
```
And when we click the second button and after 10 seconds, the app runs normally and just catch the Exception to avoid the app crashing:
![](https://github.com/oukanggui/ToastUtil/blob/master/app/src/main/assets/toast_fix_catch.png)
![](https://github.com/oukanggui/ToastUtil/blob/master/app/src/main/assets/toast_fix_catch1.png)
## Thanks
Thanks for the detail analysis and ideas from QQ Music Technology Team, details can be seen in the following articles:<br>
[[Android] Toast问题深度剖析(一)](http://www.cnblogs.com/qcloud1001/p/8421356.html)<br>
[[Android] Toast问题深度剖析(二)](http://www.itboth.com/d/jiY73qyeAzei/api-java-toast-android)<br>
