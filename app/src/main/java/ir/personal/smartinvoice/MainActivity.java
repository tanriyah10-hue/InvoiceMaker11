package ir.personal.smartinvoice;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    DB db;
    EditText customer, phone, notes;
TextView totalView;
LinearLayout root, itemContainer;
final ArrayList<ItemRow> itemRows = new ArrayList<>();

class ItemRow {
    EditText description, qty, price, discount;
    LinearLayout card;
}
    long lastId=-1;
    int bg=0xFFF4F6F8, primary=0xFF176B87;

    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(0xFF124D63); getWindow().setNavigationBarColor(0xFF124D63); db=new DB(this); showForm(); }
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size, int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setPadding(dp(4),dp(7),dp(4),dp(7)); t.setGravity(Gravity.RIGHT); return t; }
    EditText field(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setSingleLine(true); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); e.setPadding(dp(12),dp(8),dp(12),dp(8)); e.setBackgroundColor(0xFFFFFFFF); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return e; }
    Button button(String s, Runnable r){ Button b=new Button(this); b.setText(s); b.setOnClickListener(v->r.run()); return b; }
    void base(String title){ root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(12),dp(16),dp(16)); root.setBackgroundColor(bg); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); ScrollView sc=new ScrollView(this); sc.addView(root); setContentView(sc); TextView h=text(title,22,0xFF124D63); h.setGravity(Gravity.CENTER); root.addView(h); }
    void addField(String label, EditText e){ root.addView(text(label,14,0xFF34495E)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50)); p.bottomMargin=dp(5); root.addView(e,p); }
    void showForm(){
        base("فاکتور ساز شخصی");
        customer=field("نام مشتری"); phone=field("شماره تماس (اختیاری)"); description=field("شرح کالا یا خدمات"); qty=field("تعداد"); qty.setInputType(8194); qty.setText("1"); price=field("قیمت واحد به تومان"); price.setInputType(8194); discount=field("تخفیف به تومان"); discount.setInputType(8194); discount.setText("0"); notes=field("توضیحات (اختیاری)");
        addField("نام مشتری",customer); addField("شماره تماس",phone); addField("شرح کالا یا خدمات",description); addField("تعداد",qty); addField("قیمت واحد (تومان)",price); addField("تخفیف (تومان)",discount); addField("توضیحات",notes);
        totalView=text("مبلغ نهایی: ۰ تومان",19,primary); totalView.setGravity(Gravity.CENTER); root.addView(totalView);
        root.addView(button("محاسبه مبلغ",()->calculate())); root.addView(button("ذخیره فاکتور",()->save())); root.addView(button("فاکتورهای ذخیره‌شده",()->showList()));
        root.addView(text("اطلاعات در حافظهٔ همین گوشی ذخیره می‌شود. برای نگهداری بلندمدت، از فایل PDF خروجی بگیر.",12,0xFF666666));
    }
    long number(EditText e){try{return Long.parseLong(e.getText().toString().replaceAll("[^0-9]",""));}catch(Exception x){return 0;}}
    String money(long n){return String.format(Locale.US,"%,d",n);}
    long finalAmount(){ long q=number(qty), p=number(price), d=number(discount); return Math.max(0,q*p-d); }
    void calculate(){long q=number(qty),p=number(price); if(q<1||p<0){toast("تعداد و قیمت را بررسی کن");return;} totalView.setText("مبلغ نهایی: "+money(finalAmount())+" تومان");}
    void save(){String c=customer.getText().toString().trim(), desc=description.getText().toString().trim(); long q=number(qty),p=number(price),d=number(discount); if(c.isEmpty()||desc.isEmpty()||q<1||p<0){toast("نام مشتری، شرح کالا، تعداد و قیمت را کامل کن");return;} long id=db.insert(c,phone.getText().toString(),desc,q,p,d,notes.getText().toString(),finalAmount(),System.currentTimeMillis()); if(id>0){lastId=id; calculate(); toast("فاکتور ذخیره شد"); hideKeyboard(); new AlertDialog.Builder(this).setMessage("فاکتور ذخیره شد. اکنون PDF هم تهیه شود؟").setPositiveButton("تهیه PDF",(a,b)->makePdf(id)).setNegativeButton("بعداً",null).show();}else toast("ذخیره انجام نشد"); }
    void showList(){base("فاکتورهای ذخیره‌شده"); root.addView(button("بازگشت به فرم ثبت فاکتور",()->showForm())); Cursor c=db.getReadableDatabase().rawQuery("SELECT id,customer,description,total,created FROM invoices ORDER BY id DESC",null); if(c.getCount()==0)root.addView(text("هنوز فاکتوری ثبت نشده است.",16,0xFF555555)); while(c.moveToNext()){final long id=c.getLong(0); LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(10),dp(8),dp(10),dp(8)); card.setBackgroundColor(-1); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(8); root.addView(card,cp); card.addView(text("مشتری: "+c.getString(1),16,0xFF222222)); card.addView(text(c.getString(2)+" | "+money(c.getLong(3))+" تومان",14,primary)); card.addView(text(new SimpleDateFormat("yyyy/MM/dd - HH:mm",Locale.getDefault()).format(new Date(c.getLong(4))),12,0xFF666666)); LinearLayout actions=new LinearLayout(this); actions.addView(button("PDF",()->makePdf(id))); actions.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("این فاکتور حذف شود؟").setPositiveButton("حذف",(a,b)->{db.delete(id);showList();}).setNegativeButton("انصراف",null).show())); card.addView(actions); } c.close(); }
    void makePdf(long id){ Cursor c=db.getReadableDatabase().rawQuery("SELECT customer,phone,description,qty,price,discount,notes,total,created FROM invoices WHERE id=?",new String[]{String.valueOf(id)}); if(!c.moveToFirst()){c.close();toast("فاکتور پیدا نشد");return;} String[] v=new String[9]; for(int i=0;i<9;i++)v[i]=c.getString(i); c.close(); PdfDocument pdf=new PdfDocument(); PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,1).create(); PdfDocument.Page page=pdf.startPage(info); Canvas canvas=page.getCanvas(); Paint paint=new Paint(3); paint.setColor(Color.BLACK); paint.setTextSize(18); paint.setTextAlign(Paint.Align.RIGHT); int y=65; canvas.drawText("فاکتور فروش / خدمات",550,y,paint); paint.setTextSize(13); y+=45; String[] lines={"مشتری: "+v[0],"تلفن: "+v[1],"تاریخ: "+new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(Long.parseLong(v[8]))),"شرح: "+v[2],"تعداد: "+v[3],"قیمت واحد: "+money(Long.parseLong(v[4]))+" تومان","تخفیف: "+money(Long.parseLong(v[5]))+" تومان","مبلغ نهایی: "+money(Long.parseLong(v[7]))+" تومان","توضیحات: "+v[6]}; for(String line:lines){canvas.drawText(line,550,y,paint);y+=35;} pdf.finishPage(page); File dir=new File(getExternalFilesDir(null),"Invoices"); if(!dir.exists())dir.mkdirs(); File file=new File(dir,"invoice-"+id+".pdf"); try{FileOutputStream out=new FileOutputStream(file);pdf.writeTo(out);out.close();pdf.close(); Uri uri=androidx.core.content.FileProvider.getUriForFile(this,"ir.personal.smartinvoice.fileprovider",file); Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/pdf");send.putExtra(Intent.EXTRA_STREAM,uri);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"اشتراک‌گذاری فاکتور PDF"));}catch(Exception e){pdf.close();toast("خطا در ساخت PDF: "+e.getMessage());} }
    void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    class DB extends SQLiteOpenHelper{DB(Context c){super(c,"invoice_personal.db",null,1);} public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT, customer TEXT, phone TEXT, description TEXT, qty INTEGER, price INTEGER, discount INTEGER, notes TEXT, total INTEGER, created INTEGER)");} public void onUpgrade(SQLiteDatabase d,int a,int b){} long insert(String c,String ph,String desc,long q,long p,long dis,String n,long total,long time){ContentValues v=new ContentValues();v.put("customer",c);v.put("phone",ph);v.put("description",desc);v.put("qty",q);v.put("price",p);v.put("discount",dis);v.put("notes",n);v.put("total",total);v.put("created",time);return getWritableDatabase().insert("invoices",null,v);} void delete(long id){getWritableDatabase().delete("invoices","id=?",new String[]{String.valueOf(id)});} }
}
