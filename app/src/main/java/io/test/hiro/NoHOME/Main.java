package io.test.hiro.NoHOME;

import static android.content.ContentValues.TAG;

import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;

public class Main implements IXposedHookLoadPackage {

    private Context appContext;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("com.miui.home")) { // Targeting MIUI home package
 //hookAllClassesInPackage(loadPackageParam.classLoader, loadPackageParam);
            XposedHelpers.findAndHookMethod(
                    "com.miui.home.launcher.LauncherStateManager$StartAnimRunnable",
                    loadPackageParam.classLoader,
                    "run",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                            Object launcherStateManager = XposedHelpers.getSurroundingThis(param.thisObject); // 外部クラスのインスタンス取得

                            // Context を取得する (まずは標準的な方法を試す)
                            Context context = null;
                            try {
                                context = (Context) XposedHelpers.callMethod(launcherStateManager, "getContext");
                            } catch (NoSuchMethodError | ClassCastException e) {
                                Log.e(TAG, "Failed to get context via getContext() method: " + e.getMessage());
                            }

                            // それでも取得できない場合、代替手段を試す
                            if (context == null) {
                                Log.w(TAG, "Fallback to using ActivityThread to get application context.");
                                context = (Context) XposedHelpers.callStaticMethod(
                                        XposedHelpers.findClass("android.app.ActivityThread", null),
                                        "currentApplication"
                                );
                            }

                            if (context == null) {
                                Log.e(TAG, "Unable to retrieve context. Exiting hook.");
                                return;
                            }

                            // アプリの起動処理
                            String targetPackage = "com.teslacoilsw.launcher";
                            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);

                            if (launchIntent != null) {
                                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                context.startActivity(launchIntent);
                                Log.d(TAG, "Successfully launched the app: " + targetPackage);
                            } else {
                                Log.e(TAG, "Launch intent not found for package: " + targetPackage);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    "com.miui.home.recents.NavStubView",
                    loadPackageParam.classLoader,
                    "performAppToHome",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mLauncher");

                            if (context == null) {
                                Log.e(TAG, "Unable to retrieve context. Exiting hook.");
                                return;
                            }

                            // アプリの起動処理
                            String targetPackage = "com.teslacoilsw.launcher";
                            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);

                            if (launchIntent != null) {
                                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                context.startActivity(launchIntent);
                                Log.d(TAG, "Successfully launched the app: " + targetPackage);
                            } else {
                                Log.e(TAG, "Launch intent not found for package: " + targetPackage);
                            }
                        }
                    }
            );

                    }

    }

    private void hookAllClassesInPackage(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            String apkPath = loadPackageParam.appInfo.sourceDir;
            if (apkPath == null) {
                XposedBridge.log("Could not get APK path.");
                return;
            }

            DexFile dexFile = new DexFile(new File(apkPath));
            Enumeration<String> classNames = dexFile.entries();
            while (classNames.hasMoreElements()) {
                String className = classNames.nextElement();

                // 指定されたパッケージで始まるクラスのみをフック
                //  if (className.startsWith("com.linecorp.line") || className.startsWith("jp.naver.line.android")) {
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    hookAllMethods(clazz);
                } catch (ClassNotFoundException e) {
                    XposedBridge.log("Class not found: " + className);
                } catch (Throwable e) {
                    XposedBridge.log("Error loading class " + className + ": " + e.getMessage());
                }
                //  }
            }
        } catch (Throwable e) {
            XposedBridge.log("Error while hooking classes: " + e.getMessage());
        }
    }
    private void hookAllMethods(Class<?> clazz) {
        // クラス内のすべてのメソッドを取得
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            // 抽象メソッドをスキップ
            if (java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }

            // 対象メソッドが特定のビュー関連メソッドであるか確認
            if (!"invokeSuspend".equals(method.getName()) &&
                    !"run".equals(method.getName()) &&
                    !"setOnTouchListener".equals(method.getName()) &&

                    !"setAlpha".equals(method.getName()) &&
                    !"setEnabled".equals(method.getName()) &&
                    !"setFocusable".equals(method.getName()) &&
                    !"setBackgroundColor".equals(method.getName()) &&

                    !"setHintTextColor".equals(method.getName()) &&  // 新しく追加されたメソッド
                    !"onStart".equals(method.getName()) &&
                    !"setCompoundDrawables".equals(method.getName()) &&
                    !"getActivity".equals(method.getName()) &&  // PendingIntent method
                    !"setState".equals(method.getName())) {   // PendingIntent method
                continue;
            }

            method.setAccessible(true); // アクセス可能に設定

            try {
                // メソッドをフックする
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");

                        // 引数が複数の場合、すべてを追加
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }

// メソッドに応じたログ出力
                        if ("invokeSuspend".equals(method.getName())) {
                            XposedBridge.log("Before calling invokeSuspend in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("run".equals(method.getName())) {
                            XposedBridge.log("Before calling run in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setAlpha".equals(method.getName())) {
                            XposedBridge.log("Before calling setAlpha in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setEnabled".equals(method.getName())) {
                            XposedBridge.log("Before calling setEnabled in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setFocusable".equals(method.getName())) {
                            XposedBridge.log("Before calling setFocusable in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setOnClickListener".equals(method.getName())) {
                            XposedBridge.log("Before calling setOnClickListener in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setBackgroundColor".equals(method.getName())) {
                            XposedBridge.log("Before calling setBackgroundColor in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setPadding".equals(method.getName())) {
                            XposedBridge.log("Before calling setPadding in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setLayoutParams".equals(method.getName())) {
                            XposedBridge.log("Before calling setLayoutParams in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("invalidate".equals(method.getName())) {
                            XposedBridge.log("Before calling invalidate in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setText".equals(method.getName())) {
                            XposedBridge.log("Before calling setText in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setTextColor".equals(method.getName())) {
                            XposedBridge.log("Before calling setTextColor in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setHint".equals(method.getName())) {
                            XposedBridge.log("Before calling setHint in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setHintTextColor".equals(method.getName())) {
                            XposedBridge.log("Before calling setHintTextColor in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setCompoundDrawables".equals(method.getName())) {
                            XposedBridge.log("Before calling setCompoundDrawables in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("onStart".equals(method.getName())) {
                            XposedBridge.log("Before calling onStart in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("getActivity".equals(method.getName())) {
                            XposedBridge.log("Before calling getActivity in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("onViewAdded".equals(method.getName())) {
                            XposedBridge.log("Before calling onViewAdded in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("getService".equals(method.getName())) {
                            XposedBridge.log("Before calling getService in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setState".equals(method.getName())) {
                            XposedBridge.log("Before setState invoke in class: " + clazz.getName() + " with args: " + argsString);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object result = param.getResult();
                        if ("invokeSuspend".equals(method.getName())) {
                            XposedBridge.log("Before calling invokeSuspend in class: " + clazz.getName() + (result != null ? result.toString() : "null"));
                        } else if ("run".equals(method.getName())) {
                            XposedBridge.log("After calling run in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setAlpha".equals(method.getName())) {
                            XposedBridge.log("After calling setAlpha in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setEnabled".equals(method.getName())) {
                            XposedBridge.log("After calling setEnabled in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setFocusable".equals(method.getName())) {
                            XposedBridge.log("After calling setFocusable in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setOnClickListener".equals(method.getName())) {
                            XposedBridge.log("After calling setOnClickListener in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setBackgroundColor".equals(method.getName())) {
                            XposedBridge.log("After calling setBackgroundColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setPadding".equals(method.getName())) {
                            XposedBridge.log("After calling setPadding in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setLayoutParams".equals(method.getName())) {
                            XposedBridge.log("After calling setLayoutParams in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("requestLayout".equals(method.getName())) {
                            XposedBridge.log("After calling requestLayout in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("invalidate".equals(method.getName())) {
                            XposedBridge.log("After calling invalidate in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setText".equals(method.getName())) {
                            XposedBridge.log("After calling setText in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setTextColor".equals(method.getName())) {
                            XposedBridge.log("After calling setTextColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setHint".equals(method.getName())) {
                            XposedBridge.log("After calling setHint in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setHintTextColor".equals(method.getName())) {
                            XposedBridge.log("After calling setHintTextColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setCompoundDrawables".equals(method.getName())) {
                            XposedBridge.log("After calling setCompoundDrawables in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("onStart".equals(method.getName())) {
                            XposedBridge.log("Before calling onStart in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("getActivity".equals(method.getName())) {
                            XposedBridge.log("After calling getActivity in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("onViewAdded".equals(method.getName())) {
                            XposedBridge.log("After calling onViewAdded in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("getService".equals(method.getName())) {
                            XposedBridge.log("After calling getService in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setState".equals(method.getName())) {
                            XposedBridge.log("setState " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        }
                    }
                });
            } catch (IllegalArgumentException e) {
                XposedBridge.log("Error hooking method " + method.getName() + " in class " + clazz.getName() + " : " + e.getMessage());
            } catch (Throwable e) {
                XposedBridge.log("Unexpected error hooking method " + method.getName() + " in class " + clazz.getName() + " : " + Log.getStackTraceString(e));
            }
        }
    }
}
