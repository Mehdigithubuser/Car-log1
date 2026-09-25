package com.mehdi.carlog;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        CarLogDb db = new CarLogDb(context);
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("reminders","CarLog reminders",NotificationManager.IMPORTANCE_DEFAULT));
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        String today=f.format(new Date());
        Calendar now=Calendar.getInstance();
        CursorWrap c=new CursorWrap(db.reminders());
        while(c.move()){
            String date=c.s(3); int lead=c.i(7);
            boolean warn=false;
            if(!date.isEmpty()){
                try{
                    Calendar due=Calendar.getInstance(); due.setTime(f.parse(date)); due.set(Calendar.HOUR_OF_DAY,0);due.set(Calendar.MINUTE,0);due.set(Calendar.SECOND,0);
                    Calendar limit=(Calendar)now.clone();limit.add(Calendar.DAY_OF_YEAR,lead);
                    warn=!due.after(limit);
                }catch(Exception ignored){}
            }
            if(warn){
                String key="date_"+c.l(0)+"_"+date;
                if(context.getSharedPreferences("reminder_notifications",Context.MODE_PRIVATE).getBoolean(key,false)){ c.next(); continue; }
                android.app.Notification.Builder b=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(context,"reminders"):new android.app.Notification.Builder(context);
                b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(c.s(1)).setContentText("Reminder due on "+date).setAutoCancel(true);
                nm.notify((int)(c.l(0)%100000),b.build());
                context.getSharedPreferences("reminder_notifications",Context.MODE_PRIVATE).edit().putBoolean("date_"+c.l(0)+"_"+date,true).apply();
            }
            c.next();
        }
        c.close();
    }

    static class CursorWrap{
        android.database.Cursor c;boolean valid=false;CursorWrap(android.database.Cursor x){c=x;}
        boolean move(){valid=c.moveToFirst();return valid;}void next(){valid=c.moveToNext();}void close(){c.close();}
        String s(int i){String x=c.getString(i);return x==null?"":x;}int i(int i){return c.isNull(i)?0:c.getInt(i);}long l(int i){return c.isNull(i)?0:c.getLong(i);}
    }
}