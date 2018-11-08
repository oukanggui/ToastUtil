# ToastUtil
ToastUtil which **fixes the BadTokenException happened on the device 7.x while showing Toast which will cause your app to crash,just like this<br>**
android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@ba9eb53 is not valid; is your activity running?<br>
at android.view.ViewRootImpl.setView(ViewRootImpl.java:679)<br>
at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:342)<br>
at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:94)<br>
at android.widget.Toast$TN.handleShow(Toast.java:459)<br>
at android.widget.Toast$TN$2.handleMessage(Toast.java:342)<br>
at android.os.Handler.dispatchMessage(Handler.java:102)<br>
at android.os.Looper.loop(Looper.java:158)<br>
at android.app.ActivityThread.main(ActivityThread.java:6175)<br>
at java.lang.reflect.Method.invoke(Native Method)<br>
at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:893)<br>
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:783)<br>

## How it works
## How to reappear and check fix
## Thanks
Thanks for the detail analysis and ideas from QQ Music Technology Team, detail to see following articles:<br>
[[Android] Toast问题深度剖析(一)](http://www.cnblogs.com/qcloud1001/p/8421356.html)<br>
[[Android] Toast问题深度剖析(二)](http://www.itboth.com/d/jiY73qyeAzei/api-java-toast-android)<br>
