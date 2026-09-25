package com.mehdi.carlog;

import android.app.*;
import android.os.*;
import android.database.Cursor;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    CarLogDb db; TextView vehicleInfo,summary,recent;
    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);db=new CarLogDb(this);
        vehicleInfo=findViewById(R.id.vehicleInfo);summary=findViewById(R.id.summary);recent=findViewById(R.id.recent);
        findViewById(R.id.addFuel).setOnClickListener(v->fuelDialog());
        findViewById(R.id.addMaintenance).setOnClickListener(v->maintenanceDialog());
        findViewById(R.id.addExpense).setOnClickListener(v->expenseDialog());
        findViewById(R.id.settings).setOnClickListener(v->vehicleDialog());
        findViewById(R.id.reports).setOnClickListener(v->reportDialog());
        findViewById(R.id.backup).setOnClickListener(v->backupDialog());
        refresh();
    }
    void refresh(){Cursor c=db.vehicle(); if(c.moveToFirst()){vehicleInfo.setText(c.getString(1)+"  •  "+c.getString(2)+" "+c.getString(3)+"  •  "+c.getDouble(6)+" km");}else vehicleInfo.setText("No vehicle — tap Vehicle to add"); c.close();
        double fuel=db.sum("fuel","total"), maint=db.sum("maintenance","parts+labor"), exp=db.sum("expense","amount");
        summary.setText(String.format(Locale.US,"Fuel: %.0f   Maintenance: %.0f   Other: %.0f\nTotal: %.0f",fuel,maint,exp,fuel+maint+exp));
        StringBuilder s=new StringBuilder(); Cursor r=db.recent();while(r.moveToNext())s.append(r.getString(0)).append("  ").append(r.getString(1)).append("  ").append(r.getDouble(2)).append("\n");r.close();recent.setText(s.length()==0?"No records yet":s.toString());
    }
    long vid(){Cursor c=db.vehicle();long id=c.moveToFirst()?c.getLong(0):-1;c.close();return id;}
    EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);return e;}
    LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(32,8,32,8);return l;}
    double num(EditText e){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return 0;}}
    void vehicleDialog(){LinearLayout l=box();EditText name=field("Vehicle name"),make=field("Make"),model=field("Model"),trim=field("Trim"),year=field("Year"),odo=field("Current odometer");for(EditText e:new EditText[]{name,make,model,trim,year,odo})l.addView(e);new AlertDialog.Builder(this).setTitle("Vehicle profile").setView(l).setPositiveButton("Save",(d,w)->{db.addVehicle(name.getText().toString(),make.getText().toString(),model.getText().toString(),trim.getText().toString(),(int)num(year),num(odo),"km");refresh();}).setNegativeButton("Cancel",null).show();}
    void fuelDialog(){if(vid()<0){vehicleDialog();return;}LinearLayout l=box();EditText odo=field("Odometer km"),lit=field("Liters"),price=field("Price per liter"),type=field("Fuel type (Gasoline)"),station=field("Station"),note=field("Note");for(EditText e:new EditText[]{odo,lit,price,type,station,note})l.addView(e);new AlertDialog.Builder(this).setTitle("Add fuel").setView(l).setPositiveButton("Save",(d,w)->{double L=num(lit),p=num(price);db.addFuel(vid(),today(),num(odo),type.getText().toString().isEmpty()?"Gasoline":type.getText().toString(),L,p,L*p,false,station.getText().toString(),note.getText().toString());refresh();}).setNegativeButton("Cancel",null).show();}
    void maintenanceDialog(){if(vid()<0){vehicleDialog();return;}LinearLayout l=box();EditText odo=field("Odometer km"),type=field("Service type"),parts=field("Parts cost"),labor=field("Labor cost"),next=field("Next due km"),date=field("Next due date YYYY-MM-DD"),note=field("Note");for(EditText e:new EditText[]{odo,type,parts,labor,next,date,note})l.addView(e);new AlertDialog.Builder(this).setTitle("Add maintenance").setView(l).setPositiveButton("Save",(d,w)->{db.addMaintenance(vid(),today(),num(odo),type.getText().toString(),num(parts),num(labor),num(next),date.getText().toString(),note.getText().toString());refresh();}).setNegativeButton("Cancel",null).show();}
    void expenseDialog(){if(vid()<0){vehicleDialog();return;}LinearLayout l=box();EditText odo=field("Odometer km"),cat=field("Category"),amt=field("Amount"),note=field("Note");for(EditText e:new EditText[]{odo,cat,amt,note})l.addView(e);new AlertDialog.Builder(this).setTitle("Add expense").setView(l).setPositiveButton("Save",(d,w)->{db.addExpense(vid(),today(),num(odo),cat.getText().toString(),num(amt),note.getText().toString());refresh();}).setNegativeButton("Cancel",null).show();}
    void reportDialog(){new AlertDialog.Builder(this).setTitle("Reports").setMessage("Fuel total: "+db.sum("fuel","total")+"\nMaintenance total: "+db.sum("maintenance","parts+labor")+"\nOther expenses: "+db.sum("expense","amount")).setPositiveButton("OK",null).show();}
    void backupDialog(){new AlertDialog.Builder(this).setTitle("Backup / Restore").setMessage("Portable .carlog backup and CSV export are planned for the next build.").setPositiveButton("OK",null).show();}
}
