package com.mehdi.carlog;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;

public class CarLogDb extends SQLiteOpenHelper {
    public CarLogDb(Context c) { super(c, "carlog.db", null, 1); }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE vehicle(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,make TEXT,model TEXT,trim TEXT,year INTEGER,odometer REAL,unit TEXT)");
        db.execSQL("CREATE TABLE fuel(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,type TEXT,liters REAL,price REAL,total REAL,full_tank INTEGER,station TEXT,note TEXT)");
        db.execSQL("CREATE TABLE maintenance(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,type TEXT,parts REAL,labor REAL,next_km REAL,next_date TEXT,note TEXT)");
        db.execSQL("CREATE TABLE expense(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,date TEXT,odometer REAL,category TEXT,amount REAL,note TEXT)");
        db.execSQL("CREATE TABLE reminder(id INTEGER PRIMARY KEY AUTOINCREMENT,vehicle_id INTEGER,title TEXT,due_km REAL,due_date TEXT,done INTEGER DEFAULT 0)");
    }
    public void onUpgrade(SQLiteDatabase db,int oldV,int newV) {}
    public long addVehicle(String name,String make,String model,String trim,int year,double odo,String unit){
        ContentValues v=new ContentValues(); v.put("name",name);v.put("make",make);v.put("model",model);v.put("trim",trim);v.put("year",year);v.put("odometer",odo);v.put("unit",unit);
        return getWritableDatabase().insert("vehicle",null,v);
    }
    public Cursor vehicle(){return getReadableDatabase().rawQuery("SELECT * FROM vehicle ORDER BY id LIMIT 1",null);}
    public void addFuel(long vid,String date,double odo,String type,double liters,double price,double total,boolean full,String station,String note){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("date",date);v.put("odometer",odo);v.put("type",type);v.put("liters",liters);v.put("price",price);v.put("total",total);v.put("full_tank",full?1:0);v.put("station",station);v.put("note",note);getWritableDatabase().insert("fuel",null,v);
    }
    public void addMaintenance(long vid,String date,double odo,String type,double parts,double labor,double nextKm,String nextDate,String note){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("date",date);v.put("odometer",odo);v.put("type",type);v.put("parts",parts);v.put("labor",labor);v.put("next_km",nextKm);v.put("next_date",nextDate);v.put("note",note);getWritableDatabase().insert("maintenance",null,v);
    }
    public void addExpense(long vid,String date,double odo,String cat,double amount,String note){
        ContentValues v=new ContentValues();v.put("vehicle_id",vid);v.put("date",date);v.put("odometer",odo);v.put("category",cat);v.put("amount",amount);v.put("note",note);getWritableDatabase().insert("expense",null,v);
    }
    public double sum(String table,String col){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM("+col+"),0) FROM "+table,null);double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public Cursor recent(){return getReadableDatabase().rawQuery("SELECT date,'Fuel: '||type AS item,total AS amount FROM fuel UNION ALL SELECT date,'Maintenance: '||type,parts+labor FROM maintenance UNION ALL SELECT date,'Expense: '||category,amount FROM expense ORDER BY date DESC LIMIT 20",null);}
}
