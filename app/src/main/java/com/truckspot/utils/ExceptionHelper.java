package com.truckspot.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.truckspot.BuildConfig;
import com.truckspot.fragment.Dashboard;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ExceptionHelper {
    private static final String FILENAME = "stacktrace.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public static void init(Context context) {
        if (Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context));
    }

    public static boolean checkForCrash(PrefRepository prefRepository, Activity activity) {
        try {

            FileInputStream file = activity.openFileInput(FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(file);
            BufferedReader stacktrace = new BufferedReader(inputStreamReader);
            final StringBuilder report = new StringBuilder();
            PackageManager pm = activity.getPackageManager();
            PackageInfo packageInfo;
            String release = Build.VERSION.RELEASE;
            int sdkVersion = Build.VERSION.SDK_INT;
            String deviceName = getDeviceName();
            try {
                packageInfo = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNATURES);
                report.append("Device: ").append(deviceName).append('\n');
                report.append("Android SDK: ").append(sdkVersion).append(" (").append(release).append(")").append('\n');
                report.append("Version: ").append(packageInfo.versionName).append('\n');
                report.append(String.format(Locale.ROOT, "Version: %s(%d) ", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)).append('\n');
                report.append("Last Update: ").append(DATE_FORMAT.format(new Date(packageInfo.lastUpdateTime))).append('\n');
                report.append('\n');
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            String line;
            while ((line = stacktrace.readLine()) != null) {
                report.append(line);
                report.append('\n');
            }

            new AlertDialog.Builder(activity).setTitle("Crash Report").setMessage(report.toString()).setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(prefRepository.getRememberMe() && prefRepository.getLoggedIn() && !prefRepository.getToken().isEmpty())
                    {
                        activity.startActivity(new Intent(activity, Dashboard.class));
                        activity.finish();
                    }
                }
            }).create().show();
            file.close();
            activity.deleteFile(FILENAME);
            /*final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.crash_report_title));
            builder.setMessage(activity.getText(R.string.crash_report_message));
            builder.setPositiveButton(activity.getText(R.string.send_now), (dialog, which) -> {
                Log.d(Config.LOGTAG, "using account=" + account.getJid().asBareJid() + " to send in stack trace");
                final Conversation conversation = service.findOrCreateConversation(account, Config.BUG_REPORTS, false, true);
                final Message message = new Message(conversation, report.toString(), Message.ENCRYPTION_NONE);
                service.sendMessage(message, "");
            });
            builder.setNegativeButton(activity.getText(R.string.send_never), (dialog, which) -> preferences.edit().putBoolean("never_send", true).apply());
            builder.create().show();*/

            return true;
        } catch (final Exception ignored) {
            return false;
        }
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }

    static void writeToStacktraceFile(Context context, String msg) {
        try {
            OutputStream os = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            os.write(msg.getBytes());
            os.flush();
            os.close();
        } catch (IOException ignored) {
        }
    }
}
