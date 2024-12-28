package io.test.hiro.NoHOME;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {

    private Context appContext;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("com.miui.home")) { 

            hookMiuiHomeMethods(loadPackageParam);

        } else {
            savePackageNameToFile(loadPackageParam.packageName);
            return;
        }
    }

    private void hookMiuiHomeMethods(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedHelpers.findAndHookMethod(
                "com.miui.home.launcher.LauncherStateManager$StartAnimRunnable",
                loadPackageParam.classLoader,
                "run",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    
                        handleLaunchIntent(param.thisObject);
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
                        handleLaunchIntent(param.thisObject);
                    }
                }
        );
    }

    private void handleLaunchIntent(Object paramThisObject) throws Throwable {
        Context context = (Context) XposedHelpers.getObjectField(paramThisObject, "mLauncher");
        if (context == null) {
        
            return;
        }

        String targetPackage = getTargetPackageName();
        if (targetPackage == null) {   
            return; 
        }

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
        if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launchIntent);
        
        } else {
        
        }
    }
    private void savePackageNameToFile(String packageName) {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Launcher");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return;
        }

        File file = new File(backupDir, "package_names.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(packageName + "\n");
        } catch (IOException e) {
        }
    }

    private String getTargetPackageName() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Launcher/package_names.txt");

        if (!file.exists()) {
    
            return null; 
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath());

            if (lines.isEmpty()) {
                return null; 
            }
            return lines.get(lines.size() - 1);
        } catch (IOException e) {

            return null;
        }
    }


}
