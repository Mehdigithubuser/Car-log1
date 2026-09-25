package com.mehdi.carlog;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;

public class CarLogDb extends SQLiteOpenHelper {
    public CarLogDb(Context c) { super(c, "carlog.db", null, 3); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE vehicle(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,make TEXT,model TEXT,trim TEXT,year INTEGER,odometer REAL,unit TEXT)");
        db.execSQL("CREATE TABLE fuel(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,type TEXT,liters REAL,price REAL,total REAL,full_tank INTEGER,station TEXT,note TEXT)");
        db.execSQL("CREATE TABLE maintenance(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,type TEXT,parts REAL,labor REAL,next_km REAL,next_date TEXT,note TEXT)");
        db.execSQL("CREATE TABLE expense(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,category TEXT,amount REAL,note TEXT)");
        db.execSQL("CREATE TABLE reminder(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,title TEXT,due_km REAL,due_date TEXT,done INTEGER DEFAULT 0,repeat_km REAL DEFAULT 0,repeat_months INTEGER DEFAULT 0,notify_km REAL DEFAULT 500,notify_days INTEGER DEFAULT 7,active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE service_template(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,interval_km REAL DEFAULT 0,interval_months INTEGER DEFAULT 0,custom INTEGER DEFAULT 0)");
        seedServiceTemplates(db);
    }

    private void seedServiceTemplates(SQLiteDatabase db) {
        String[][] data = {
            {"Engine oil + oil filter", "8000", "6"},
            {"Engine oil change", "8000", "6"},
            {"Oil filter", "8000", "6"},
            {"Air filter", "20000", "12"},
            {"Cabin / A/C filter", "15000", "12"},
            {"Fuel filter", "30000", "24"},
            {"Front brake pads", "30000", "24"},
            {"Rear brake pads", "40000", "24"},
            {"Brake discs", "60000", "36"},
            {"Brake fluid", "40000", "24"},
            {"Coolant", "40000", "24"},
            {"Spark plugs", "30000", "24"},
            {"Battery replacement", "50000", "36"},
            {"Timing belt", "80000", "60"},
            {"Gearbox oil", "60000", "48"},
            {"Tire rotation", "10000", "6"},
            {"Wheel alignment", "10000", "12"},
            {"Technical inspection", "0", "12"},
            {"Car wash / detailing", "0", "0"},
            {"Other", "0", "0"}
        };
        for (String[] x : data) {
            ContentValues v = new ContentValues();
            v.put("name", x[0]);
            v.put("interval_km", Double.parseDouble(x[1]));
            v.put("interval_months", Integer.parseInt(x[2]));
            v.put("custom", 0);
            db.insertWithOnConflict("service_template", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Defensive migration for older builds with a partial version-2 schema.
        db.execSQL("CREATE TABLE IF NOT EXISTS vehicle(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,make TEXT,model TEXT,trim TEXT,year INTEGER,odometer REAL,unit TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS fuel(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,type TEXT,liters REAL,price REAL,total REAL,full_tank INTEGER,station TEXT,note TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS maintenance(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,type TEXT,parts REAL,labor REAL,next_km REAL,next_date TEXT,note TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS expense(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,category TEXT,amount REAL,note TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reminder(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,title TEXT,due_km REAL,due_date TEXT,done INTEGER DEFAULT 0,repeat_km REAL DEFAULT 0,repeat_months INTEGER DEFAULT 0,notify_km REAL DEFAULT 500,notify_days INTEGER DEFAULT 7,active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS service_template(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,interval_km REAL DEFAULT 0,interval_months INTEGER DEFAULT 0,custom INTEGER DEFAULT 0)");
        addColumn(db,"reminder","repeat_km","REAL DEFAULT 0");
        addColumn(db,"reminder","repeat_months","INTEGER DEFAULT 0");
        addColumn(db,"reminder","notify_km","REAL DEFAULT 500");
        addColumn(db,"reminder","notify_days","INTEGER DEFAULT 7");
        addColumn(db,"reminder","active","INTEGER DEFAULT 1");
        seedServiceTemplates(db);
    }

    private void addColumn(SQLiteDatabase db,String table,String column,String definition) {
        try { db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition); } catch(Exception ignored) {}
    }

    public long addVehicle(String name,String make,String model,String trim,int year,double odo,String unit){
        ContentValues v=new ContentValues();
        v.put("name",name);v.put("make",make);v.put("model",model);v.put("trim",trim);v.put("year",year);v.put("odometer",odo);v.put("unit",unit);
        return getWritableDatabase().insert("vehicle",null,v);
    }
    public void updateOdometer(long id,double odo){
        ContentValues v=new ContentValues();v.put("odometer",odo);
        getWritableDatabase().update("vehicle",v,"id=?",new String[]{String.valueOf(id)});
    }
    public Cursor vehicle(){return getReadableDatabase().rawQuery("SELECT * FROM vehicle ORDER BY id LIMIT 1",null);}

    public void addFuel(long vid,String date,double odo,String type,double liters,double price,double total,boolean full,String station,String note){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("date",date);v.put("odometer",odo);v.put("type",type);
        v.put("liters",liters);v.put("price",price);v.put("total",total);v.put("full_tank",full?1:0);v.put("station",station);v.put("note",note);
        getWritableDatabase().insert("fuel",null,v); updateOdometer(vid, odo);
    }

    public long addMaintenance(long vid,String date,double odo,String type,double parts,double labor,double nextKm,String nextDate,String note){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("date",date);v.put("odometer",odo);v.put("type",type);
        v.put("parts",parts);v.put("labor",labor);v.put("next_km",nextKm);v.put("next_date",nextDate);v.put("note",note);
        long id=getWritableDatabase().insert("maintenance",null,v); updateOdometer(vid, odo); return id;
    }

    public void addExpense(long vid,String date,double odo,String cat,double amount,String note){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("date",date);v.put("odometer",odo);v.put("category",cat);v.put("amount",amount);v.put("note",note);
        getWritableDatabase().insert("expense",null,v); updateOdometer(vid, odo);
    }

    public long addReminder(long vid,String title,double dueKm,String dueDate,double repeatKm,int repeatMonths,double notifyKm,int notifyDays){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("title",title);v.put("due_km",dueKm);v.put("due_date",dueDate);
        v.put("repeat_km",repeatKm);v.put("repeat_months",repeatMonths);v.put("notify_km",notifyKm);v.put("notify_days",notifyDays);v.put("active",1);v.put("done",0);
        return getWritableDatabase().insert("reminder",null,v);
    }

    public void completeReminder(long id){
        Cursor c=getReadableDatabase().rawQuery("SELECT repeat_km,repeat_months,due_km,due_date FROM reminder WHERE id=?",new String[]{String.valueOf(id)});
        if(!c.moveToFirst()){c.close();return;}
        double rkm=c.getDouble(0), nextKm=c.getDouble(2); int rmo=c.getInt(1); String dueDate=c.getString(3);
        if(rkm>0) nextKm+=rkm; else nextKm=0;
        String nextDate=dueDate;
        if(rmo>0 && dueDate!=null && !dueDate.isEmpty()) {
            try{
                java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US);
                java.util.Calendar cal=java.util.Calendar.getInstance();
                cal.setTime(f.parse(dueDate)); cal.add(java.util.Calendar.MONTH,rmo); nextDate=f.format(cal.getTime());
            }catch(Exception ignored){}
        }
        c.close();
        ContentValues v=new ContentValues();
        if(rkm>0 || rmo>0){v.put("due_km",nextKm);v.put("due_date",nextDate);v.put("done",0);v.put("active",1);}
        else {v.put("done",1);v.put("active",0);}
        getWritableDatabase().update("reminder",v,"id=?",new String[]{String.valueOf(id)});
    }

    public double sum(String table,String col){
        Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM("+col+"),0) FROM "+table,null);
        double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;
    }

    public Cursor recent(){return getReadableDatabase().rawQuery(
        "SELECT date,'Fuel · '||type AS item,total AS amount FROM fuel " +
        "UNION ALL SELECT date,'Service · '||type,parts+labor FROM maintenance " +
        "UNION ALL SELECT date,'Expense · '||category,amount FROM expense ORDER BY date DESC LIMIT 20",null);}

    public Cursor maintenance(){return getReadableDatabase().rawQuery("SELECT date,type,odometer,parts+labor,next_km,next_date FROM maintenance ORDER BY date DESC,id DESC",null);}
    public Cursor reminders(){return getReadableDatabase().rawQuery("SELECT id,title,due_km,due_date,repeat_km,repeat_months,notify_km,notify_days FROM reminder WHERE active=1 AND done=0 ORDER BY CASE WHEN due_date IS NULL OR due_date='' THEN 1 ELSE 0 END,due_date,due_km",null);}
    public Cursor serviceTemplates(){return getReadableDatabase().rawQuery("SELECT id,name,interval_km,interval_months,custom FROM service_template ORDER BY custom,name",null);}

    public long addServiceTemplate(String name,double km,int months){
        ContentValues v=new ContentValues();v.put("name",name);v.put("interval_km",km);v.put("interval_months",months);v.put("custom",1);
        return getWritableDatabase().insertWithOnConflict("service_template",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor findServiceTemplate(String name){return getReadableDatabase().rawQuery("SELECT interval_km,interval_months FROM service_template WHERE name=?",new String[]{name});}
}
