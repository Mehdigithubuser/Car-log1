package com.mehdi.carlog;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    CarLogDb db;
    View homeView, maintenanceView, reminderView, moreView;
    TextView vehicleTitle, vehicleSub, totalValue, fuelValue, serviceValue, expenseValue, upcomingText, recentList;
    int blue = Color.rgb(32, 107, 196);
    int teal = Color.rgb(26, 150, 140);
    int bg = Color.rgb(247, 249, 252);
    int dark = Color.rgb(25, 36, 50);
    int muted = Color.rgb(102, 115, 128);
    int card = Color.WHITE;
    String today(){ return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        try {
            setContentView(R.layout.activity_main);
            bind();
            setupBottomNav();
        } catch (Throwable error) {
            android.util.Log.e("CarLog","UI initialization failed",error);
            LinearLayout fallback = new LinearLayout(this);
            fallback.setOrientation(LinearLayout.VERTICAL);
            fallback.setPadding(32,48,32,32);
            fallback.setBackgroundColor(bg);
            TextView title = tv("CarLog",28,dark,true);
            TextView msg = tv("Starting CarLog…",16,muted,false);
            fallback.addView(title);
            fallback.addView(msg);
            setContentView(fallback);
            return;
        }

        showOnly(homeView);
        vehicleTitle.setText("My Vehicle");
        vehicleSub.setText("Loading your vehicle data…");
        totalValue.setText("0");
        fuelValue.setText("0");
        serviceValue.setText("0");
        expenseValue.setText("0");
        upcomingText.setText("No upcoming reminders");
        recentList.setText("No records yet");

        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},25);

        new Thread(() -> {
            try {
                CarLogDb loadedDb = new CarLogDb(getApplicationContext());
                loadedDb.getReadableDatabase();
                runOnUiThread(() -> {
                    try {
                        db = loadedDb;
                        refreshHome();
                        scheduleDailyReminderCheck();
                    } catch (Throwable error) {
                        android.util.Log.e("CarLog","Dashboard initialization failed",error);
                        vehicleSub.setText("Vehicle data is unavailable. You can still use CarLog.");
                        upcomingText.setText("No upcoming reminders");
                        recentList.setText("No records yet");
                        Toast.makeText(MainActivity.this,"CarLog started with an empty dashboard.",Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable error) {
                android.util.Log.e("CarLog","Database initialization failed",error);
                runOnUiThread(() -> {
                    vehicleSub.setText("Database could not be opened. The dashboard is still available.");
                    Toast.makeText(MainActivity.this,"CarLog database error — please try again.",Toast.LENGTH_LONG).show();
                });
            }
        }, "CarLog-DB").start();
    }

    void bind(){
        homeView=findViewById(R.id.homeView);
        maintenanceView=findViewById(R.id.maintenanceView);
        reminderView=findViewById(R.id.reminderView);
        moreView=findViewById(R.id.moreView);
        vehicleTitle=findViewById(R.id.vehicleTitle);
        vehicleSub=findViewById(R.id.vehicleSub);
        totalValue=findViewById(R.id.totalValue);
        fuelValue=findViewById(R.id.fuelValue);
        serviceValue=findViewById(R.id.serviceValue);
        expenseValue=findViewById(R.id.expenseValue);
        upcomingText=findViewById(R.id.upcomingText);
        recentList=findViewById(R.id.recentList);

        findViewById(R.id.quickFuel).setOnClickListener(v->fuelDialog());
        findViewById(R.id.quickService).setOnClickListener(v->maintenanceDialog());
        findViewById(R.id.quickExpense).setOnClickListener(v->expenseDialog());
        findViewById(R.id.quickReminder).setOnClickListener(v->reminderDialog());
        findViewById(R.id.addServicePage).setOnClickListener(v->maintenanceDialog());
        findViewById(R.id.addReminderPage).setOnClickListener(v->reminderDialog());
        findViewById(R.id.vehicleProfilePage).setOnClickListener(v->vehicleDialog());
        findViewById(R.id.reportsPage).setOnClickListener(v->reportDialog());
        findViewById(R.id.backupPage).setOnClickListener(v->backupDialog());
        findViewById(R.id.navHome).setOnClickListener(v->showHome());
        findViewById(R.id.navMaintenance).setOnClickListener(v->showMaintenance());
        findViewById(R.id.navReminders).setOnClickListener(v->showReminders());
        findViewById(R.id.navMore).setOnClickListener(v->showMore());
    }

    void setupBottomNav(){
        for(int id:new int[]{R.id.navHome,R.id.navMaintenance,R.id.navReminders,R.id.navMore}){
            TextView t=findViewById(id); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        }
        styleAction(R.id.quickFuel,blue);
        styleAction(R.id.quickService,teal);
        styleAction(R.id.quickExpense,Color.rgb(124,92,196));
        styleAction(R.id.quickReminder,Color.rgb(55,135,190));
        styleAction(R.id.addServicePage,teal);
        styleAction(R.id.addReminderPage,blue);
        styleSecondary(R.id.vehicleProfilePage);
        styleSecondary(R.id.reportsPage);
        styleSecondary(R.id.backupPage);
        updateNav(R.id.navHome);
    }
    void styleAction(int id,int color){
        Button b=findViewById(id);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(rounded(color,18));
        b.setPadding(16,0,16,0);
    }
    void styleSecondary(int id){
        Button b=findViewById(id);
        b.setAllCaps(false);
        b.setTextColor(dark);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        b.setPadding(18,0,18,0);
        b.setBackground(rounded(Color.WHITE,18));
    }
    void updateNav(int activeId){
        int[] ids={R.id.navHome,R.id.navMaintenance,R.id.navReminders,R.id.navMore};
        for(int id:ids){
            TextView t=findViewById(id);
            t.setTextColor(id==activeId?blue:muted);
            t.setTypeface(Typeface.DEFAULT,id==activeId?Typeface.BOLD:Typeface.NORMAL);
        }
    }

    void requestNotifications(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},25);
    }

    void scheduleDailyReminderCheck(){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(this,9001,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Calendar c=Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,9); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0);
        if(c.getTimeInMillis()<=System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR,1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
    }

    void showOnly(View active){
        homeView.setVisibility(active==homeView?View.VISIBLE:View.GONE);
        maintenanceView.setVisibility(active==maintenanceView?View.VISIBLE:View.GONE);
        reminderView.setVisibility(active==reminderView?View.VISIBLE:View.GONE);
        moreView.setVisibility(active==moreView?View.VISIBLE:View.GONE);
    }
    boolean ensureDb(){
        if(db!=null) return true;
        Toast.makeText(this,"CarLog is still loading. Please try again in a moment.",Toast.LENGTH_SHORT).show();
        return false;
    }
    void showHome(){ if(!ensureDb()) return; refreshHome(); showOnly(homeView); updateNav(R.id.navHome); }
    void showMaintenance(){ if(!ensureDb()) return; refreshMaintenance(); showOnly(maintenanceView); updateNav(R.id.navMaintenance); }
    void showReminders(){ if(!ensureDb()) return; checkMileageReminders(); refreshReminders(); showOnly(reminderView); updateNav(R.id.navReminders); }
    void showMore(){ showOnly(moreView); updateNav(R.id.navMore); }

    GradientDrawable rounded(int color,float radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;
    }
    TextView tv(String text,float size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    LinearLayout column(){ LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l; }
    LinearLayout row(){ LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l; }
    LinearLayout cardBox(){ LinearLayout l=column();l.setPadding(20,18,20,18);l.setBackground(rounded(card,28));return l; }
    void margin(View v,int l,int t,int r,int b){
        ViewGroup.LayoutParams p=v.getLayoutParams();
        if(p instanceof LinearLayout.LayoutParams)((LinearLayout.LayoutParams)p).setMargins(l,t,r,b);
    }
    LinearLayout.LayoutParams lp(int w,int h,float weight){return new LinearLayout.LayoutParams(w,h,weight);}
    Button actionButton(String text){
        Button b=new Button(this);b.setAllCaps(false);b.setText(text);b.setTextSize(14);b.setTextColor(Color.WHITE);
        b.setBackground(rounded(blue,20));b.setPadding(18,0,18,0);return b;
    }

    void refreshHome(){
        if(!ensureDb()) return;
        CursorWrap v=new CursorWrap(db.vehicle());
        if(v.move()){ vehicleTitle.setText(v.s(1).isEmpty()?"My Vehicle":v.s(1)); vehicleSub.setText((v.s(2)+" "+v.s(3)).trim()+"  •  "+String.format(Locale.US,"%.0f km",v.d(6))); }
        else { vehicleTitle.setText("My Vehicle");vehicleSub.setText("Tap Vehicle profile to add your car"); }
        v.close();

        double fuel=db.sum("fuel","total"), service=db.sum("maintenance","parts+labor"), exp=db.sum("expense","amount"), total=fuel+service+exp;
        fuelValue.setText(money(fuel));serviceValue.setText(money(service));expenseValue.setText(money(exp));totalValue.setText(money(total));
        String next="No upcoming reminders";
        CursorWrap r=new CursorWrap(db.reminders());
        long bestScore=Long.MAX_VALUE;
        while(r.move()){
            String title=r.s(1), date=r.s(3);
            double dueKm=r.d(2);
            StringBuilder line=new StringBuilder(title);
            long score=Long.MAX_VALUE;
            double currentOdo=currentOdometer();
            if(dueKm>0){
                long remainKm=Math.max(0,Math.round(dueKm-currentOdo));
                line.append("\\n").append(remainKm==0?"Due now":"In "+String.format(Locale.US,"%.0f km",remainKm));
                line.append("  •  ").append(String.format(Locale.US,"%.0f km",dueKm));
                score=Math.min(score,remainKm*1000L);
            }
            if(!date.isEmpty()){
                long days=daysUntil(date);
                line.append("\\n").append(days<=0?"Due today":days==1?"In 1 day":"In "+days+" days");
                line.append("  •  ").append(date);
                score=Math.min(score,Math.max(0,days)*1000L+500);
            }
            if(score<bestScore){bestScore=score;next=line.toString();}
            r.next();
        }
        r.close(); upcomingText.setText(next);
        StringBuilder recent=new StringBuilder(); CursorWrap h=new CursorWrap(db.recent());
        while(h.move()){ recent.append(h.s(0)).append("  •  ").append(h.s(1)); if(h.d(2)>0) recent.append("  ·  ").append(money(h.d(2))); recent.append("\n"); h.next(); } h.close();
        recentList.setText(recent.length()==0?"No records yet":recent.toString());
    }

    double currentOdometer(){
        CursorWrap c=new CursorWrap(db.vehicle());
        double x=c.move()?c.d(6):0;
        c.close();
        return x;
    }
    long daysUntil(String date){
        try{
            SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
            f.setLenient(false);
            Date d=f.parse(date);
            Calendar due=Calendar.getInstance(); due.setTime(d);
            Calendar now=Calendar.getInstance();
            due.set(Calendar.HOUR_OF_DAY,0); due.set(Calendar.MINUTE,0); due.set(Calendar.SECOND,0); due.set(Calendar.MILLISECOND,0);
            now.set(Calendar.HOUR_OF_DAY,0); now.set(Calendar.MINUTE,0); now.set(Calendar.SECOND,0); now.set(Calendar.MILLISECOND,0);
            return Math.round((due.getTimeInMillis()-now.getTimeInMillis())/86400000.0);
        }catch(Exception e){return Long.MAX_VALUE/4;}
    }
    String money(double x){return String.format(Locale.US,"%.0f",x);}

    void refreshMaintenance(){
        if(!ensureDb()) return;
        LinearLayout list=findViewById(R.id.maintenanceList); list.removeAllViews();
        CursorWrap c=new CursorWrap(db.maintenance());
        if(!c.move()){TextView e=tv("No services recorded yet.\nTap + Add service to start your maintenance history.",16,muted,false);e.setPadding(8,24,8,24);list.addView(e);}
        while(c.valid){
            LinearLayout box=cardBox(); TextView title=tv(c.s(1),17,dark,true);box.addView(title);
            TextView meta=tv(c.s(0)+"  •  "+String.format(Locale.US,"%.0f km",c.d(2)),13,muted,false);box.addView(meta);
            TextView cost=tv("Cost  "+money(c.d(3)),14,dark,true);box.addView(cost);margin(cost,0,7,0,0);
            if(c.d(4)>0 || !c.s(5).isEmpty()){
                String due="Next due";
                if(c.d(4)>0)due+="  "+String.format(Locale.US,"%.0f km",c.d(4));
                if(!c.s(5).isEmpty())due+="  •  "+c.s(5);
                TextView dt=tv(due,13,teal,true);box.addView(dt);margin(dt,0,6,0,0);
            }
            LinearLayout.LayoutParams cp=lp(-1,-2,0);cp.setMargins(0,0,0,12);list.addView(box,cp);
            c.next();
        }
        c.close();
    }

    void refreshReminders(){
        if(!ensureDb()) return;
        LinearLayout list=findViewById(R.id.reminderList); list.removeAllViews();
        CursorWrap c=new CursorWrap(db.reminders());
        if(!c.move()){TextView e=tv("No reminders yet.\nCreate one for oil, inspection, insurance, tires or anything else.",16,muted,false);e.setPadding(8,24,8,24);list.addView(e);}
        while(c.valid){
            final long id=c.l(0); LinearLayout box=cardBox(); LinearLayout top=row();
            TextView title=tv(c.s(1),17,dark,true);top.addView(title,lp(0,-2,1));
            TextView done=tv("✓",22,teal,true);top.addView(done,lp(32,40,0));box.addView(top);
            String due="";
            if(c.d(2)>0)due+=String.format(Locale.US,"Odometer: %.0f km",c.d(2));
            if(!c.s(3).isEmpty()){if(!due.isEmpty())due+="\n";due+="Date: "+c.s(3);}
            TextView dt=tv(due,14,muted,false);box.addView(dt);margin(dt,0,5,0,0);
            String repeat="";if(c.d(4)>0)repeat+=String.format(Locale.US,"Every %.0f km",c.d(4));if(c.i(5)>0){if(!repeat.isEmpty())repeat+="  •  ";repeat+=c.i(5)+" months";}
            if(!repeat.isEmpty()){TextView rp=tv("Repeats: "+repeat,12,blue,false);box.addView(rp);}
            Button b=actionButton("Mark done");box.addView(b,lp(-1,44,0));margin(b,10,10,10,0);
            b.setOnClickListener(v->{db.completeReminder(id);checkMileageReminders();refreshReminders();});
            LinearLayout.LayoutParams cp=lp(-1,-2,0);cp.setMargins(0,0,0,12);list.addView(box,cp);
            c.next();
        }
        c.close();
    }

    void serviceTemplateDialog(){
        if(!ensureDb()) return;
        LinearLayout l=column();l.setPadding(28,10,28,8);
        CursorWrap c=new CursorWrap(db.serviceTemplates());ArrayList<String> names=new ArrayList<>();ArrayList<String> hints=new ArrayList<>();
        while(c.move()){names.add(c.s(1));String h="";if(c.d(2)>0)h+=String.format(Locale.US,"every %.0f km",c.d(2));if(c.i(3)>0){if(!h.isEmpty())h+=" / ";h+=c.i(3)+" mo";}hints.add(h);c.next();}c.close();
        LinearLayout list=column();
        for(int i=0;i<names.size();i++){
            final String n=names.get(i), h=hints.get(i);
            LinearLayout item=cardBox();TextView t=tv(n,16,dark,true);item.addView(t);
            if(!h.isEmpty())item.addView(tv(h,12,muted,false));
            item.setOnClickListener(v->{Toast.makeText(this,"Service selected: "+n,Toast.LENGTH_SHORT).show();});
            LinearLayout.LayoutParams ip=lp(-1,-2,0);ip.setMargins(0,0,0,8);list.addView(item,ip);
        }
        ScrollView sv=new ScrollView(this);sv.addView(list);l.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        Button add=actionButton("＋ Add service type");l.addView(add,lp(-1,48,0));margin(add,0,10,0,0);
        add.setOnClickListener(v->{addTemplateDialog();});
        new AlertDialog.Builder(this).setTitle("Service list").setView(l).setNegativeButton("Close",null).show();
    }

    void addTemplateDialog(){
        if(!ensureDb()) return;
        LinearLayout l=box();EditText name=field("Service name"),km=field("Default interval km (optional)"),mo=field("Default interval months (optional)");
        l.addView(name);l.addView(km);l.addView(mo);
        new AlertDialog.Builder(this).setTitle("Add service type").setView(l).setPositiveButton("Add",(d,w)->{
            String n=name.getText().toString().trim(); if(!n.isEmpty()){db.addServiceTemplate(n,num(km),(int)num(mo));Toast.makeText(this,"Added to service list",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("Cancel",null).show();
    }

    void maintenanceDialog(){
        if(!ensureDb()) return;
        if(vid()<0){vehicleDialog();return;}
        LinearLayout l=box();
        LinearLayout head=row();TextView label=tv("Service type",14,muted,false);head.addView(label,lp(0,-2,1));
        Button manage=actionButton("Manage list");head.addView(manage,lp(-2,44,0));l.addView(head);
        Spinner service=new Spinner(this);ArrayList<String> names=new ArrayList<>();CursorWrap c=new CursorWrap(db.serviceTemplates());
        while(c.move()){names.add(c.s(1));c.next();}c.close();
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names);service.setAdapter(ad);l.addView(service);margin(service,0,4,0,8);
        EditText odo=field("Odometer km"),parts=field("Parts cost"),labor=field("Labor cost"),next=field("Next due km"),date=field("Next due date YYYY-MM-DD"),note=field("Note");
        l.addView(odo);l.addView(parts);l.addView(labor);l.addView(next);l.addView(date);l.addView(note);
        TextView helper=tv("Selecting a service can pre-fill its default interval. You can change it.",12,muted,false);l.addView(helper);
        CursorWrap vv=new CursorWrap(db.vehicle()); if(vv.move())odo.setText(String.format(Locale.US,"%.0f",vv.d(6)));vv.close();
        service.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                String n=names.get(pos);CursorWrap x=new CursorWrap(db.findServiceTemplate(n));
                if(x.move()){
                    double k=x.d(0);int m=x.i(1);double now=num(odo);if(k>0)next.setText(String.format(Locale.US,"%.0f",now+k));
                    if(m>0)date.setText(addMonths(today(),m));}
                x.close();
            }
        });
        manage.setOnClickListener(v->serviceTemplateDialog());
        new AlertDialog.Builder(this).setTitle("Add service").setView(l).setPositiveButton("Save",(d,w)->{
            String type=(String)service.getSelectedItem();double current=num(odo);db.addMaintenance(vid(),today(),current,type,num(parts),num(labor),num(next),date.getText().toString(),note.getText().toString());
            double nk=num(next);String nd=date.getText().toString();if(nk>0||!nd.isEmpty())db.addReminder(vid(),type,nk,nd,serviceIntervalKm(type),serviceIntervalMonths(type),500,30);
            Toast.makeText(this,"Service saved",Toast.LENGTH_SHORT).show();showMaintenance();
        }).setNegativeButton("Cancel",null).show();
    }

    double serviceIntervalKm(String n){CursorWrap c=new CursorWrap(db.findServiceTemplate(n));double x=c.move()?c.d(0):0;c.close();return x;}
    int serviceIntervalMonths(String n){CursorWrap c=new CursorWrap(db.findServiceTemplate(n));int x=c.move()?c.i(1):0;c.close();return x;}
    String addMonths(String d,int months){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);Calendar c=Calendar.getInstance();c.setTime(f.parse(d));c.add(Calendar.MONTH,months);return f.format(c.getTime());}catch(Exception e){return "";}}
    
    void reportDialog(){
        if(!ensureDb()) return;
        new AlertDialog.Builder(this).setTitle("Reports")
            .setMessage("Fuel total: "+money(db.sum("fuel","total"))+"\\nMaintenance total: "+money(db.sum("maintenance","parts+labor"))+"\\nOther expenses: "+money(db.sum("expense","amount")))
            .setPositiveButton("OK",null).show();
    }

    void backupDialog(){
        if(!ensureDb()) return;
        new AlertDialog.Builder(this).setTitle("Backup / Restore")
            .setMessage("Local backup/export can be added next. Your current records stay on the phone and work offline.")
            .setPositiveButton("OK",null).show();
    }

    void reminderDialog(){
        if(!ensureDb()) return;
        if(vid()<0){vehicleDialog();return;}
        LinearLayout l=box();EditText title=field("Reminder title (e.g. Oil change)"),dueKm=field("Due odometer km (optional)"),dueDate=field("Due date YYYY-MM-DD (optional)"),repeatKm=field("Repeat every km (optional)"),repeatMo=field("Repeat every months (optional)"),notifyKm=field("Notify before km"),notifyDays=field("Notify before days");
        for(EditText e:new EditText[]{title,dueKm,dueDate,repeatKm,repeatMo,notifyKm,notifyDays})l.addView(e);
        notifyKm.setText("500");notifyDays.setText("7");
        TextView hint=tv("Use date, mileage, or both. Repeating reminders automatically move to the next interval when completed.",12,muted,false);l.addView(hint);
        new AlertDialog.Builder(this).setTitle("New reminder").setView(l).setPositiveButton("Create",(d,w)->{
            if(title.getText().toString().trim().isEmpty())return;
            db.addReminder(vid(),title.getText().toString().trim(),num(dueKm),dueDate.getText().toString().trim(),num(repeatKm),(int)num(repeatMo),num(notifyKm),(int)num(notifyDays));
            checkMileageReminders();Toast.makeText(this,"Reminder created",Toast.LENGTH_SHORT).show();showReminders();
        }).setNegativeButton("Cancel",null).show();
    }

    void checkMileageReminders(){
        CursorWrap v=new CursorWrap(db.vehicle());double odo=v.move()?v.d(6):0;v.close();
        if(odo<=0)return;
        CursorWrap r=new CursorWrap(db.reminders());
        while(r.move()){
            double due=r.d(2), lead=r.d(6);if(due>0 && odo>=due-lead)notifyOnce(r.l(0),r.s(1),"Due at "+String.format(Locale.US,"%.0f km",due));
            r.next();
        }r.close();
    }

    void notifyOnce(long id,String title,String text){
        String key="n_"+id+"_"+title+"_"+text;
        if(getPreferences(MODE_PRIVATE).getBoolean(key,false))return;
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("reminders","CarLog reminders",NotificationManager.IMPORTANCE_DEFAULT));
        android.app.Notification.Builder b=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(this,"reminders"):new android.app.Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(text).setAutoCancel(true);
        nm.notify((int)(id%100000),b.build());getPreferences(MODE_PRIVATE).edit().putBoolean(key,true).apply();
    }

    void vehicleDialog(){
        if(!ensureDb()) return;
        LinearLayout l=box();EditText name=field("Vehicle name"),make=field("Make"),model=field("Model / type"),trim=field("Trim"),year=field("Year"),odo=field("Current odometer km");
        for(EditText e:new EditText[]{name,make,model,trim,year,odo})l.addView(e);
        new AlertDialog.Builder(this).setTitle("Vehicle profile").setView(l).setPositiveButton("Save",(d,w)->{
            db.addVehicle(name.getText().toString(),make.getText().toString(),model.getText().toString(),trim.getText().toString(),(int)num(year),num(odo),"km");refreshHome();
        }).setNegativeButton("Cancel",null).show();
    }

    void fuelDialog(){
        if(!ensureDb()) return;
        if(vid()<0){vehicleDialog();return;}
        LinearLayout l=box();EditText odo=field("Odometer km"),lit=field("Liters"),price=field("Price per liter"),type=field("Fuel type (Gasoline)"),station=field("Station"),note=field("Note");
        for(EditText e:new EditText[]{odo,lit,price,type,station,note})l.addView(e);
        CursorWrap v=new CursorWrap(db.vehicle());if(v.move())odo.setText(String.format(Locale.US,"%.0f",v.d(6)));v.close();
        new AlertDialog.Builder(this).setTitle("Add fuel").setView(l).setPositiveButton("Save",(d,w)->{
            double L=num(lit),p=num(price);db.addFuel(vid(),today(),num(odo),type.getText().toString().isEmpty()?"Gasoline":type.getText().toString(),L,p,L*p,false,station.getText().toString(),note.getText().toString());showHome();
        }).setNegativeButton("Cancel",null).show();
    }

    void expenseDialog(){
        if(!ensureDb()) return;
        if(vid()<0){vehicleDialog();return;}
        LinearLayout l=box();EditText odo=field("Odometer km"),cat=field("Category (e.g. insurance)"),amt=field("Amount"),note=field("Note");
        for(EditText e:new EditText[]{odo,cat,amt,note})l.addView(e);
        CursorWrap v=new CursorWrap(db.vehicle());if(v.move())odo.setText(String.format(Locale.US,"%.0f",v.d(6)));v.close();
        new AlertDialog.Builder(this).setTitle("Add expense").setView(l).setPositiveButton("Save",(d,w)->{
            db.addExpense(vid(),today(),num(odo),cat.getText().toString(),num(amt),note.getText().toString());showHome();
        }).setNegativeButton("Cancel",null).show();
    }

    long vid(){if(db==null)return -1;CursorWrap c=new CursorWrap(db.vehicle());long id=c.move()?c.l(0):-1;c.close();return id;}
    EditText field(String hint){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(dark);
        e.setHintTextColor(muted);
        e.setPadding(16,0,16,0);
        e.setMinHeight(54);
        e.setBackground(rounded(Color.rgb(244,247,250),18));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,54);
        p.setMargins(0,0,0,12);
        e.setLayoutParams(p);
        return e;
    }
    LinearLayout box(){LinearLayout l=column();l.setPadding(24,16,24,12);return l;}
    double num(EditText e){try{return Double.parseDouble(e.getText().toString().replace(",",""));}catch(Exception x){return 0;}}

    static class CursorWrap {
        Cursor c; boolean valid=false; CursorWrap(Cursor x){c=x;}
        boolean move(){valid=c.moveToFirst();return valid;}
        void next(){valid=c.moveToNext();}
        void close(){c.close();}
        String s(int i){String x=c.getString(i);return x==null?"":x;}
        double d(int i){return c.isNull(i)?0:c.getDouble(i);}
        int i(int i){return c.isNull(i)?0:c.getInt(i);}
        long l(int i){return c.isNull(i)?0:c.getLong(i);}
    }
}
