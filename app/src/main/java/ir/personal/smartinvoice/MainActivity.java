package ir.personal.smartinvoice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private DB db;

    private EditText customer;
    private EditText phone;
    private EditText notes;
    private TextView totalView;
    private LinearLayout root;
    private LinearLayout itemContainer;

    private final ArrayList<ItemRow> itemRows = new ArrayList<>();

    private final int backgroundColor = 0xFFF4F6F8;
    private final int primaryColor = 0xFF176B87;
private static final String COMPANY_NAME = " گروه فنی مهندسی ایمن سولار سيستم";
private static final String COMPANY_ADDRESS = "ارومیه، آزادگان ۱،کوی۴";
private static final String COMPANY_PHONE = "۰۹۱۴ ۶۰۷۷ ۶۲۶";
    private class ItemRow {
        EditText description;
        EditText qty;
        EditText price;
        EditText discount;
        LinearLayout card;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(0xFF124D63);
        getWindow().setNavigationBarColor(0xFF124D63);

        db = new DB(this);
        showForm();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(dp(4), dp(7), dp(4), dp(7));
        view.setGravity(Gravity.RIGHT);
        return view;
    }

    private EditText field(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(15);
        editText.setSingleLine(true);
        editText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        editText.setPadding(dp(12), dp(8), dp(12), dp(8));
        editText.setBackgroundColor(Color.WHITE);
        editText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return editText;
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(16));
        root.setBackgroundColor(backgroundColor);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        setContentView(scrollView);

        TextView heading = text(title, 22, 0xFF124D63);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);
    }

    private void addField(String label, EditText editText) {
        root.addView(text(label, 14, 0xFF34495E));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );
        params.bottomMargin = dp(5);
        root.addView(editText, params);
    }

    private void showForm() {
        base("فاکتور ساز شخصی");

        customer = field("نام مشتری");
        phone = field("شماره تماس (اختیاری)");
        notes = field("توضیحات فاکتور (اختیاری)");

        addField("نام مشتری", customer);
        addField("شماره تماس", phone);

        root.addView(text("کالاها و خدمات", 18, primaryColor));

        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemContainer);

        itemRows.clear();
        addItemRow();

        root.addView(button(
                "＋ افزودن ردیف کالا یا خدمات",
                this::addItemRow
        ));

        totalView = text("مبلغ نهایی: ۰ تومان", 19, primaryColor);
        totalView.setGravity(Gravity.CENTER);
        root.addView(totalView);

        addField("توضیحات فاکتور", notes);

        root.addView(button("محاسبه مبلغ", this::calculate));
        root.addView(button("ذخیره فاکتور", this::save));
        root.addView(button("فاکتورهای ذخیره‌شده", this::showList));

        root.addView(text(
                "اطلاعات در همین گوشی ذخیره می‌شود. برای نگهداری بلندمدت PDF بگیر.",
                12,
                0xFF666666
        ));
    }

    private void addItemRow() {
        ItemRow row = new ItemRow();

        row.card = new LinearLayout(this);
        row.card.setOrientation(LinearLayout.VERTICAL);
        row.card.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.card.setBackgroundColor(Color.WHITE);

        row.description = field("شرح کالا یا خدمات");

        row.qty = field("تعداد");
        row.qty.setInputType(8194);
        row.qty.setText("1");

        row.price = field("قیمت واحد (تومان)");
        row.price.setInputType(8194);

        row.discount = field("تخفیف این ردیف (تومان)");
        row.discount.setInputType(8194);
        row.discount.setText("0");

        row.card.addView(text("ردیف " + (itemRows.size() + 1), 16, primaryColor));
        row.card.addView(row.description);
        row.card.addView(row.qty);
        row.card.addView(row.price);
        row.card.addView(row.discount);

        row.card.addView(button("حذف این ردیف", () -> {
            if (itemRows.size() <= 1) {
                toast("فاکتور باید حداقل یک ردیف داشته باشد");
                return;
            }

            itemContainer.removeView(row.card);
            itemRows.remove(row);
            updateRowNumbers();
            updateTotal();
        }));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = dp(8);

        itemContainer.addView(row.card, params);
        itemRows.add(row);
    }

    private void updateRowNumbers() {
        for (int i = 0; i < itemRows.size(); i++) {
            ItemRow row = itemRows.get(i);
            TextView title = (TextView) row.card.getChildAt(0);
            title.setText("ردیف " + (i + 1));
        }
    }

    private long number(EditText editText) {
        String value = editText.getText().toString()
                .replace('۰', '0')
                .replace('۱', '1')
                .replace('۲', '2')
                .replace('۳', '3')
                .replace('۴', '4')
                .replace('۵', '5')
                .replace('۶', '6')
                .replace('۷', '7')
                .replace('۸', '8')
                .replace('۹', '9')
                .replace('٠', '0')
                .replace('١', '1')
                .replace('٢', '2')
                .replace('٣', '3')
                .replace('٤', '4')
                .replace('٥', '5')
                .replace('٦', '6')
                .replace('٧', '7')
                .replace('٨', '8')
                .replace('٩', '9')
                .replaceAll("[^0-9]", "");

        try {
            return value.isEmpty() ? 0 : Long.parseLong(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String money(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private long rowAmount(ItemRow row) {
        long quantity = number(row.qty);
        long price = number(row.price);
        long discount = number(row.discount);
        return Math.max(0, quantity * price - discount);
    }

    private long invoiceAmount() {
        long total = 0;
        for (ItemRow row : itemRows) {
            total += rowAmount(row);
        }
        return total;
    }

    private void updateTotal() {
        if (totalView != null) {
            totalView.setText(
                    "مبلغ نهایی: " + money(invoiceAmount()) + " تومان"
            );
        }
    }

    private boolean validateItems() {
        if (itemRows.isEmpty()) {
            toast("حداقل یک ردیف کالا یا خدمات اضافه کن");
            return false;
        }

        for (int i = 0; i < itemRows.size(); i++) {
            ItemRow row = itemRows.get(i);

            if (row.description.getText().toString().trim().isEmpty()) {
                toast("شرح ردیف " + (i + 1) + " را وارد کن");
                return false;
            }

            if (number(row.qty) < 1) {
                toast("تعداد ردیف " + (i + 1) + " باید حداقل ۱ باشد");
                return false;
            }
        }

        return true;
    }

    private void calculate() {
        if (!validateItems()) return;
        updateTotal();
    }

    private void save() {
        String customerName = customer.getText().toString().trim();

        if (customerName.isEmpty()) {
            toast("نام مشتری را وارد کن");
            return;
        }

        if (!validateItems()) return;

        SQLiteDatabase sql = db.getWritableDatabase();
        ContentValues invoice = new ContentValues();

        invoice.put("customer", customerName);
        invoice.put("phone", phone.getText().toString().trim());

        // این ستون‌ها برای سازگاری فاکتورهای نسخهٔ قبلی حفظ شده‌اند.
        ItemRow first = itemRows.get(0);
        invoice.put("description", first.description.getText().toString().trim());
        invoice.put("qty", number(first.qty));
        invoice.put("price", number(first.price));
        invoice.put("discount", number(first.discount));
        invoice.put("notes", notes.getText().toString().trim());
        invoice.put("total", invoiceAmount());
        invoice.put("created", System.currentTimeMillis());

        long invoiceId = -1;
        boolean saved = false;

        sql.beginTransaction();
        try {
            invoiceId = sql.insertOrThrow("invoices", null, invoice);

            for (ItemRow row : itemRows) {
                ContentValues item = new ContentValues();
                item.put("invoice_id", invoiceId);
                item.put(
                        "description",
                        row.description.getText().toString().trim()
                );
                item.put("qty", number(row.qty));
                item.put("price", number(row.price));
                item.put("discount", number(row.discount));

                sql.insertOrThrow("invoice_items", null, item);
            }

            sql.setTransactionSuccessful();
            saved = true;
        } catch (Exception e) {
            toast("ذخیره فاکتور انجام نشد: " + e.getMessage());
        } finally {
            sql.endTransaction();
        }

        if (saved && invoiceId > 0) {
            updateTotal();
            hideKeyboard();

            long savedId = invoiceId;
            new AlertDialog.Builder(this)
                    .setMessage("فاکتور ذخیره شد. می‌خواهی PDF آن را بسازی؟")
                    .setPositiveButton(
                            "ساخت PDF",
                            (dialog, which) -> makePdf(savedId)
                    )
                    .setNegativeButton("بستن", null)
                    .show();
        }
    }

    private void showList() {
        base("فاکتورهای ذخیره‌شده");

        root.addView(button(
                "بازگشت به فرم ثبت فاکتور",
                this::showForm
        ));

        Cursor cursor = db.getReadableDatabase().rawQuery(
                "SELECT id, customer, total, created " +
                        "FROM invoices ORDER BY id DESC",
                null
        );

        if (cursor.getCount() == 0) {
            root.addView(text("هنوز فاکتوری ذخیره نشده است.", 16, 0xFF555555));
        }

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String customerName = cursor.getString(1);
            long total = cursor.getLong(2);
            long created = cursor.getLong(3);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(8), dp(10), dp(8));
            card.setBackgroundColor(Color.WHITE);

            card.addView(text(
                    "مشتری: " + customerName,
                    16,
                    0xFF222222
            ));
            card.addView(text(
                    "مبلغ: " + money(total) + " تومان",
                    14,
                    primaryColor
            ));
            card.addView(text(
                    new SimpleDateFormat(
                            "yyyy/MM/dd - HH:mm",
                            Locale.getDefault()
                    ).format(new Date(created)),
                    12,
                    0xFF666666
            ));

            card.addView(button("ساخت و اشتراک PDF", () -> makePdf(id)));
            card.addView(button("حذف فاکتور", () ->
                    new AlertDialog.Builder(this)
                            .setMessage("این فاکتور حذف شود؟")
                            .setPositiveButton("حذف", (dialog, which) -> {
                                db.delete(id);
                                showList();
                            })
                            .setNegativeButton("انصراف", null)
                            .show()
            ));

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            params.bottomMargin = dp(8);
            root.addView(card, params);
        }

        cursor.close();
    }

    private void makePdf(long id) {
        Cursor invoiceCursor = db.getReadableDatabase().rawQuery(
                "SELECT customer, phone, notes, total, created " +
                        "FROM invoices WHERE id=?",
                new String[]{String.valueOf(id)}
        );

        if (!invoiceCursor.moveToFirst()) {
            invoiceCursor.close();
            toast("فاکتور پیدا نشد");
            return;
        }

        String customerName = invoiceCursor.getString(0);
        String phoneNumber = invoiceCursor.getString(1);
        String invoiceNotes = invoiceCursor.getString(2);
        long total = invoiceCursor.getLong(3);
        long created = invoiceCursor.getLong(4);
        invoiceCursor.close();

        PdfDocument pdf = new PdfDocument();

        try {
            PdfPageWriter writer = new PdfPageWriter(
                    pdf,
                    customerName,
                    phoneNumber,
                    created
            );

            Cursor items = db.getReadableDatabase().rawQuery(
                    "SELECT description, qty, price, discount " +
                            "FROM invoice_items WHERE invoice_id=? ORDER BY id",
                    new String[]{String.valueOf(id)}
            );

            int index = 1;
            while (items.moveToNext()) {
                String description = items.getString(0);
                long quantity = items.getLong(1);
                long price = items.getLong(2);
                long discount = items.getLong(3);
                long amount = Math.max(0, quantity * price - discount);

                writer.line(index + ") " + description);
                writer.line(
                        "تعداد: " + quantity +
                                " | قیمت واحد: " + money(price) +
                                " | تخفیف: " + money(discount)
                );
                writer.line("مبلغ این ردیف: " + money(amount) + " تومان");
                writer.line("");
                index++;
            }
            items.close();

            writer.line("مبلغ نهایی: " + money(total) + " تومان");

            if (invoiceNotes != null && !invoiceNotes.trim().isEmpty()) {
                writer.line("توضیحات: " + invoiceNotes);
            }

            writer.finish();

            File directory = new File(getExternalFilesDir(null), "Invoices");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new Exception("ساخت پوشهٔ PDF ناموفق بود");
            }

            File file = new File(directory, "invoice-" + id + ".pdf");
            try (FileOutputStream output = new FileOutputStream(file)) {
                pdf.writeTo(output);
            }

            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "ir.personal.smartinvoice.fileprovider",
                    file
            );

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(send, "اشتراک‌گذاری PDF"));
        } catch (Exception e) {
            toast("ساخت PDF انجام نشد: " + e.getMessage());
        } finally {
            pdf.close();
        }
    }

    private class PdfPageWriter {
        private final PdfDocument pdf;
        private final String customerName;
        private final String phoneNumber;
        private final long created;

        private final Paint paint = new Paint();
        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber = 0;
        private int y = 0;

        PdfPageWriter(
                PdfDocument pdf,
                String customerName,
                String phoneNumber,
                long created
        ) {
            this.pdf = pdf;
            this.customerName = customerName;
            this.phoneNumber = phoneNumber;
            this.created = created;
            newPage();
        }

        private void newPage() {
            if (page != null) {
                pdf.finishPage(page);
            }

            pageNumber++;
            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(595, 842, pageNumber)
                            .create();

            page = pdf.startPage(info);
            canvas = page.getCanvas();

            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(18);

            canvas.drawText("فاکتور کالا و خدمات", 560, 60, paint);

            paint.setTextSize(13);
            canvas.drawText("مشتری: " + customerName, 560, 105, paint);
            canvas.drawText(
                    "تلفن: " + (phoneNumber == null ? "" : phoneNumber),
                    560,
                    135,
                    paint
            );

            String date = new SimpleDateFormat(
                    "yyyy/MM/dd - HH:mm",
                    Locale.getDefault()
            ).format(new Date(created));

            canvas.drawText("تاریخ: " + date, 560, 165, paint);
            y = 210;
        }

        void line(String value) {
            if (y > 780) {
                newPage();
            }

            paint.setTextSize(12);
            canvas.drawText(value, 560, y, paint);
            y += 28;
        }

        void finish() {
            if (page != null) {
                pdf.finishPage(page);
                page = null;
            }
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(
                            INPUT_METHOD_SERVICE
                    );
            manager.hideSoftInputFromWindow(root.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private class DB extends SQLiteOpenHelper {
        DB(Context context) {
            super(context, "invoice_personal.db", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE invoices(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "customer TEXT," +
                            "phone TEXT," +
                            "description TEXT," +
                            "qty INTEGER," +
                            "price INTEGER," +
                            "discount INTEGER," +
                            "notes TEXT," +
                            "total INTEGER," +
                            "created INTEGER)"
            );

            createItemsTable(database);
        }

        private void createItemsTable(SQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS invoice_items(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "invoice_id INTEGER NOT NULL," +
                            "description TEXT NOT NULL," +
                            "qty INTEGER NOT NULL," +
                            "price INTEGER NOT NULL," +
                            "discount INTEGER NOT NULL DEFAULT 0," +
                            "FOREIGN KEY(invoice_id) REFERENCES invoices(id))"
            );
        }

        @Override
        public void onUpgrade(
                SQLiteDatabase database,
                int oldVersion,
                int newVersion
        ) {
            createItemsTable(database);

            // فاکتورهای نسخهٔ قبلی به‌صورت یک ردیف کالا منتقل می‌شوند.
            if (oldVersion < 2) {
                database.execSQL(
                        "INSERT INTO invoice_items " +
                                "(invoice_id, description, qty, price, discount) " +
                                "SELECT id, description, qty, price, discount " +
                                "FROM invoices"
                );
            }
        }

        void delete(long id) {
            SQLiteDatabase database = getWritableDatabase();

            database.delete(
                    "invoice_items",
                    "invoice_id=?",
                    new String[]{String.valueOf(id)}
            );

            database.delete(
                    "invoices",
                    "id=?",
                    new String[]{String.valueOf(id)}
            );
        }
    }
                          }
