package ir.personal.smartinvoice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String COMPANY_NAME = "گروه فنی مهندسی ایمن سولار سيستم";
    private static final String COMPANY_ADDRESS = "ارومیه، آزادگان ۱، کوی ۴";
    private static final String COMPANY_PHONE = "۰۹۱۴ ۶۰۷۷ ۶۲۶";

    private static final String TYPE_INVOICE = "فاکتور فروش کالا/خدمات";
    private static final String TYPE_PREINVOICE = "پیش‌فاکتور فروش کالا/خدمات";

    private DB db;
    private EditText customer;
    private EditText phone;
    private EditText notes;
    private TextView totalView;
    private TextView issueDateView;
    private LinearLayout root;
    private LinearLayout itemContainer;
    private Spinner documentTypeSpinner;

    private Calendar issueDate = Calendar.getInstance();
    private final ArrayList<ItemRow> itemRows = new ArrayList<>();

    private final int backgroundColor = 0xFFF4F6F8;
    private final int primaryColor = 0xFF176B87;

    private class ItemRow {
        EditText description;
        EditText qty;
        EditText price;
        EditText discount;
        EditText taxPercent;
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

        root.addView(text("نوع سند", 14, 0xFF34495E));
        documentTypeSpinner = new Spinner(this);
        String[] documentTypes = {TYPE_INVOICE, TYPE_PREINVOICE};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                documentTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        documentTypeSpinner.setAdapter(adapter);
        root.addView(documentTypeSpinner);

        root.addView(text("تاریخ صدور", 14, 0xFF34495E));
        issueDateView = text("", 16, primaryColor);
        issueDateView.setGravity(Gravity.CENTER);
        root.addView(issueDateView);
        updateIssueDateLabel();

        root.addView(button("انتخاب تاریخ صدور", this::showDatePicker));

        customer = field("نام مشتری");
        phone = field("شماره تماس مشتری (اختیاری)");
        notes = field("توضیحات فاکتور (مثال: اعتبار قیمت‌ها دو روز است)");

        addField("نام مشتری", customer);
        addField("شماره تماس مشتری", phone);

        root.addView(text("کالاها و خدمات", 18, primaryColor));

        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemContainer);

        itemRows.clear();
        addItemRow();

        root.addView(button("＋ افزودن ردیف کالا یا خدمات", this::addItemRow));

        totalView = text("مبلغ نهایی: ۰ تومان", 19, primaryColor);
        totalView.setGravity(Gravity.CENTER);
        root.addView(totalView);

        addField("توضیحات فاکتور", notes);

        root.addView(button("محاسبه مبلغ", this::calculate));
        root.addView(button("ذخیره فاکتور", this::save));
        root.addView(button("فاکتورها و پیش‌فاکتورهای ذخیره‌شده", this::showList));

        root.addView(text(
                "اطلاعات در همین گوشی ذخیره می‌شود. برای نگهداری بلندمدت PDF بگیر.",
                12,
                0xFF666666
        ));
    }

    private void showDatePicker() {
        int year = issueDate.get(Calendar.YEAR);
        int month = issueDate.get(Calendar.MONTH);
        int day = issueDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, selectedYear, selectedMonth, selectedDay) -> {
                    issueDate.set(Calendar.YEAR, selectedYear);
                    issueDate.set(Calendar.MONTH, selectedMonth);
                    issueDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                    issueDate.set(Calendar.HOUR_OF_DAY, 12);
                    issueDate.set(Calendar.MINUTE, 0);
                    issueDate.set(Calendar.SECOND, 0);
                    issueDate.set(Calendar.MILLISECOND, 0);
                    updateIssueDateLabel();
                },
                year, month, day
        );
        dialog.show();
    }

    private void updateIssueDateLabel() {
        if (issueDateView != null) {
            issueDateView.setText("تاریخ انتخاب‌شده: " + displayDate(issueDate));
        }
    }

    private String displayDate(Calendar date) {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(date.getTime());
    }

    private String databaseDate(Calendar date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date.getTime());
    }

    private String selectedDocumentType() {
        if (documentTypeSpinner == null || documentTypeSpinner.getSelectedItem() == null) {
            return TYPE_INVOICE;
        }
        return documentTypeSpinner.getSelectedItem().toString();
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

        row.discount = field("تخفیف (تومان)");
        row.discount.setInputType(8194);
        row.discount.setText("0");

        row.taxPercent = field("درصد مالیات");
        row.taxPercent.setInputType(8194);
        row.taxPercent.setText("0");

        row.card.addView(text("ردیف " + (itemRows.size() + 1), 16, primaryColor));
        row.card.addView(row.description);
        row.card.addView(row.qty);
        row.card.addView(row.price);
        row.card.addView(row.discount);
        row.card.addView(row.taxPercent);

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

    private long rowBeforeTax(ItemRow row) {
        long quantity = number(row.qty);
        long price = number(row.price);
        long discount = number(row.discount);
        return Math.max(0, quantity * price - discount);
    }

    private long rowTaxAmount(ItemRow row) {
        long subtotal = rowBeforeTax(row);
        long taxPercent = number(row.taxPercent);
        if (taxPercent <= 0) return 0;
        return Math.round(subtotal * taxPercent / 100.0);
    }

    private long rowTotalWithTax(ItemRow row) {
        return rowBeforeTax(row) + rowTaxAmount(row);
    }

    private long invoiceAmount() {
        long total = 0;
        for (ItemRow row : itemRows) {
            total += rowTotalWithTax(row);
        }
        return total;
    }

    private void updateTotal() {
        if (totalView != null) {
            totalView.setText("مبلغ نهایی: " + money(invoiceAmount()) + " تومان");
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

            if (number(row.price) < 0) {
                toast("قیمت ردیف " + (i + 1) + " نامعتبر است");
                return false;
            }

            if (number(row.discount) < 0) {
                toast("تخفیف ردیف " + (i + 1) + " نامعتبر است");
                return false;
            }

            if (number(row.taxPercent) < 0) {
                toast("درصد مالیات ردیف " + (i + 1) + " نامعتبر است");
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
        invoice.put("notes", notes.getText().toString().trim());
        invoice.put("total", invoiceAmount());
        invoice.put("created", System.currentTimeMillis());
        invoice.put("document_type", selectedDocumentType());
        invoice.put("issue_date", databaseDate(issueDate));

        long invoiceId = -1;
        boolean saved = false;

        sql.beginTransaction();
        try {
            invoiceId = sql.insertOrThrow("invoices", null, invoice);

            for (ItemRow row : itemRows) {
                long subtotal = rowBeforeTax(row);
                long taxAmount = rowTaxAmount(row);
                long rowTotal = subtotal + taxAmount;

                ContentValues item = new ContentValues();
                item.put("invoice_id", invoiceId);
                item.put("description", row.description.getText().toString().trim());
                item.put("qty", number(row.qty));
                item.put("price", number(row.price));
                item.put("discount", number(row.discount));
                item.put("tax_percent", number(row.taxPercent));
                item.put("tax_amount", taxAmount);
                item.put("total", rowTotal);

                sql.insertOrThrow("invoice_items", null, item);
            }

            sql.setTransactionSuccessful();
            saved = true;
        } catch (Exception e) {
            toast("ذخیره سند انجام نشد: " + e.getMessage());
        } finally {
            sql.endTransaction();
        }

        if (saved && invoiceId > 0) {
            updateTotal();
            hideKeyboard();

            long savedId = invoiceId;
            new AlertDialog.Builder(this)
                    .setMessage("سند ذخیره شد. می‌خواهی PDF آن را بسازی؟")
                    .setPositiveButton("ساخت PDF", (dialog, which) -> makePdf(savedId))
                    .setNegativeButton("بستن", null)
                    .show();
        }
    }

    private void showList() {
        base("فاکتورها و پیش‌فاکتورهای ذخیره‌شده");

        root.addView(button("بازگشت به فرم ثبت", this::showForm));

        Cursor cursor = db.getReadableDatabase().rawQuery(
                "SELECT id, customer, total, created, document_type, issue_date FROM invoices ORDER BY id DESC",
                null
        );

        if (cursor.getCount() == 0) {
            root.addView(text("هنوز سندی ذخیره نشده است.", 16, 0xFF555555));
        }

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String customerName = cursor.getString(1);
            long total = cursor.getLong(2);
            long created = cursor.getLong(3);
            String typeText = cursor.getString(4);
            String issueDateText = cursor.getString(5);

            if (typeText == null || typeText.trim().isEmpty()) {
                typeText = TYPE_INVOICE;
            }
            if (issueDateText == null || issueDateText.trim().isEmpty()) {
                issueDateText = dateFromTimestamp(created);
            } else {
                issueDateText = issueDateText.replace('-', '/');
            }

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(8), dp(10), dp(8));
            card.setBackgroundColor(Color.WHITE);

            card.addView(text(typeText, 16, primaryColor));
            card.addView(text("مشتری: " + customerName, 15, 0xFF222222));
            card.addView(text("تاریخ صدور: " + issueDateText, 13, 0xFF555555));
            card.addView(text("مبلغ: " + money(total) + " تومان", 14, primaryColor));

            card.addView(button("ساخت و اشتراک PDF", () -> makePdf(id)));
            card.addView(button("حذف سند", () ->
                    new AlertDialog.Builder(this)
                            .setMessage("این سند حذف شود؟")
                            .setPositiveButton("حذف", (dialog, which) -> {
                                db.delete(id);
                                showList();
                            })
                            .setNegativeButton("انصراف", null)
                            .show()
            ));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            root.addView(card, params);
        }

        cursor.close();
    }

    private String dateFromTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(new Date(timestamp));
    }

    private void makePdf(long id) {
        Cursor invoiceCursor = db.getReadableDatabase().rawQuery(
                "SELECT customer, phone, notes, total, created, document_type, issue_date " +
                        "FROM invoices WHERE id=?",
                new String[]{String.valueOf(id)}
        );

        if (!invoiceCursor.moveToFirst()) {
            invoiceCursor.close();
            toast("سند پیدا نشد");
            return;
        }

        String customerName = invoiceCursor.getString(0);
        String phoneNumber = invoiceCursor.getString(1);
        String invoiceNotes = invoiceCursor.getString(2);
        long total = invoiceCursor.getLong(3);
        long created = invoiceCursor.getLong(4);
        String docType = invoiceCursor.getString(5);
        String issueDateText = invoiceCursor.getString(6);
        invoiceCursor.close();

        if (docType == null || docType.trim().isEmpty()) {
            docType = TYPE_INVOICE;
        }
        if (issueDateText == null || issueDateText.trim().isEmpty()) {
            issueDateText = dateFromTimestamp(created);
        } else {
            issueDateText = issueDateText.replace('-', '/');
        }

        Cursor itemsCursor = db.getReadableDatabase().rawQuery(
                "SELECT description, qty, price, discount, tax_percent, tax_amount, total " +
                        "FROM invoice_items WHERE invoice_id=? ORDER BY id",
                new String[]{String.valueOf(id)}
        );

        ArrayList<ItemInfo> rows = new ArrayList<>();
        long subtotal = 0;
        long discountTotal = 0;
        long taxTotal = 0;

        while (itemsCursor.moveToNext()) {
            String description = itemsCursor.getString(0);
            long qty = itemsCursor.getLong(1);
            long price = itemsCursor.getLong(2);
            long discount = itemsCursor.getLong(3);
            long taxPercent = itemsCursor.getLong(4);
            long tax = itemsCursor.getLong(5);
            long totalRow = itemsCursor.getLong(6);

            subtotal += qty * price;
            discountTotal += discount;
            taxTotal += tax;

            rows.add(new ItemInfo(description, qty, price, discount, taxPercent, tax, totalRow));
        }
        itemsCursor.close();

        PdfDocument pdf = new PdfDocument();
        try {
            PdfPageWriter writer = new PdfPageWriter(
                    pdf,
                    customerName,
                    phoneNumber,
                    docType,
                    issueDateText,
                    created
            );

            writer.drawTableHeader();

            float yStart = writer.getCurrentY();
            float rowY = yStart;

            for (int i = 0; i < rows.size(); i++) {
                ItemInfo item = rows.get(i);
                writer.drawItemRow(
                        i + 1,
                        item.description,
                        item.qty,
                        item.price,
                        item.discount,
                        item.taxPercent,
                        item.taxAmount,
                        item.rowTotal
                );
                rowY = writer.getCurrentY();
            }

            writer.drawSummary(
                    subtotal,
                    discountTotal,
                    taxTotal,
                    total,
                    invoiceNotes
            );

            writer.finish();

            File dir = new File(getExternalFilesDir(null), "Invoices");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new Exception("ساخت پوشهٔ PDF ناموفق بود");
            }

            File file = new File(dir, "invoice-" + id + ".pdf");
            try (FileOutputStream out = new FileOutputStream(file)) {
                pdf.writeTo(out);
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

    private static class ItemInfo {
        String description;
        long qty;
        long price;
        long discount;
        long taxPercent;
        long taxAmount;
        long rowTotal;

        ItemInfo(String description, long qty, long price, long discount, long taxPercent, long taxAmount, long rowTotal) {
            this.description = description;
            this.qty = qty;
            this.price = price;
            this.discount = discount;
            this.taxPercent = taxPercent;
            this.taxAmount = taxAmount;
            this.rowTotal = rowTotal;
        }
    }

    private class PdfPageWriter {
        private final PdfDocument pdf;
        private final String customerName;
        private final String customerPhone;
        private final String documentType;
        private final String issueDateText;
        private final long created;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber = 0;
        private int y = 0;

        PdfPageWriter(
                PdfDocument pdf,
                String customerName,
                String customerPhone,
                String documentType,
                String issueDateText,
                long created
        ) {
            this.pdf = pdf;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.documentType = documentType;
            this.issueDateText = issueDateText;
            this.created = created;
            newPage();
        }

        private void newPage() {
            if (page != null) {
                pdf.finishPage(page);
            }

            pageNumber++;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
            page = pdf.startPage(info);
            canvas = page.getCanvas();

            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            canvas.drawRect(28, 28, 567, 814, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(Color.BLACK);

            paint.setTextSize(18);
            canvas.drawText(COMPANY_NAME, 548, 62, paint);

            paint.setTextSize(12);
            canvas.drawText("آدرس: " + COMPANY_ADDRESS, 548, 88, paint);
            canvas.drawText("تلفن شرکت: " + COMPANY_PHONE, 548, 111, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(17);
            canvas.drawText(documentType, 297, 145, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            canvas.drawLine(45, 160, 550, 160, paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(12);
            canvas.drawText("مشتری: " + customerName, 548, 188, paint);
            canvas.drawText("تلفن مشتری: " + (customerPhone == null ? "" : customerPhone), 548, 212, paint);
            canvas.drawText("تاریخ صدور: " + issueDateText, 548, 236, paint);

            String createdText = new SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US)
                    .format(new Date(created));
            canvas.drawText("زمان ثبت: " + createdText, 548, 260, paint);

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(45, 274, 550, 274, paint);
            paint.setStyle(Paint.Style.FILL);

            y = 300;
        }

        float getCurrentY() {
            return y;
        }

        void drawTableHeader() {
            // کادر جدول
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f);
            canvas.drawRect(45, 290, 550, 350, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(10);

            float[] xs = {50, 125, 180, 230, 290, 355, 420, 490, 530};
            String[] headers = {"ردیف", "شرح", "تعداد", "قیمت", "تخفیف", "مالیات %", "مالیات", "جمع ردیف", ""};

            // چون در برخی دستگاه‌ها ارتفاع کادر محدود است، متن‌ها با محور x مجزا می‌آیند.
            // قرار دادن ردیف‌های دکمه‌ای به صورت یکنواخت
            canvas.drawText(headers[0], xs[0], 320, paint);
            canvas.drawText(headers[1], xs[1], 320, paint);
            canvas.drawText(headers[2], xs[2], 320, paint);
            canvas.drawText(headers[3], xs[3], 320, paint);
            canvas.drawText(headers[4], xs[4], 320, paint);
            canvas.drawText(headers[5], xs[5], 320, paint);
            canvas.drawText(headers[6], xs[6], 320, paint);
            canvas.drawText(headers[7], xs[7], 320, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            y = 360;
        }

        void drawItemRow(int index, String description, long qty, long price, long discount, long taxPercent, long taxAmount, long rowTotal) {
            if (y > 700) {
                newPage();
                drawTableHeader();
            }

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(9);
            canvas.drawText(String.valueOf(index), 55, y, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            ArrayList<String> descLines = wrapText(description, 14);
            int lineCount = Math.min(descLines.size(), 2);
            for (int i = 0; i < lineCount; i++) {
                canvas.drawText(descLines.get(i), 160, y + (i * 14), paint);
            }

            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(qty), 200, y + 6, paint);
            canvas.drawText(String.format(Locale.US, "%,d", price), 250, y + 6, paint);
            canvas.drawText(String.format(Locale.US, "%,d", discount), 315, y + 6, paint);
            canvas.drawText(String.valueOf(taxPercent), 385, y + 6, paint);
            canvas.drawText(String.format(Locale.US, "%,d", taxAmount), 455, y + 6, paint);
            canvas.drawText(String.format(Locale.US, "%,d", rowTotal), 520, y + 6, paint);

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(45, y + 18, 550, y + 18, paint);
            paint.setStyle(Paint.Style.FILL);

            y += 28 + (lineCount > 1 ? 8 : 0);
        }

        ArrayList<String> wrapText(String text, int maxChars) {
            if (text == null || text.trim().isEmpty()) {
                return new ArrayList<>();
            }

            ArrayList<String> lines = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                if (word.length() > maxChars) {
                    if (current.length() > 0) {
                        lines.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                    int start = 0;
                    while (start < word.length()) {
                        int end = Math.min(start + maxChars, word.length());
                        lines.add(word.substring(start, end));
                        start = end;
                    }
                    continue;
                }

                if (current.length() + word.length() + 1 <= maxChars) {
                    if (current.length() > 0) current.append(" ");
                    current.append(word);
                } else {
                    if (current.length() > 0) {
                        lines.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                    current.append(word);
                }
            }

            if (current.length() > 0) {
                lines.add(current.toString().trim());
            }

            if (lines.isEmpty()) {
                lines.add("");
            }

            return lines;
        }

        void drawSummary(long subtotal, long discountTotal, long taxTotal, long grandTotal, String noteText) {
            if (y > 620) {
                newPage();
            }

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(45, y, 550, y + 120, paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(11);
            canvas.drawText("جمع ردیف‌ها: " + money(subtotal) + " تومان", 520, y + 22, paint);
            canvas.drawText("جمع تخفیف: " + money(discountTotal) + " تومان", 520, y + 42, paint);
            canvas.drawText("جمع مالیات: " + money(taxTotal) + " تومان", 520, y + 62, paint);
            canvas.drawText("جمع نهایی: " + money(grandTotal) + " تومان", 520, y + 92, paint);

            if (noteText != null && !noteText.trim().isEmpty()) {
                canvas.drawText("توضیحات: " + noteText, 520, y + 118, paint);
            }

            y += 140;
        }

        String money(long value) {
            return String.format(Locale.US, "%,d", value);
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
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(root.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private class DB extends SQLiteOpenHelper {
        DB(Context context) {
            super(context, "invoice_personal.db", null, 4);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE invoices(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "customer TEXT," +
                            "phone TEXT," +
                            "notes TEXT," +
                            "total INTEGER," +
                            "created INTEGER," +
                            "document_type TEXT NOT NULL DEFAULT 'فاکتور فروش کالا/خدمات'," +
                            "issue_date TEXT)"
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
                            "tax_percent INTEGER NOT NULL DEFAULT 0," +
                            "tax_amount INTEGER NOT NULL DEFAULT 0," +
                            "total INTEGER NOT NULL DEFAULT 0," +
                            "FOREIGN KEY(invoice_id) REFERENCES invoices(id))"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                createItemsTable(database);
                database.execSQL(
                        "INSERT INTO invoice_items " +
                                "(invoice_id, description, qty, price, discount) " +
                                "SELECT id, description, qty, price, discount FROM invoices"
                );
            } else {
                createItemsTable(database);
            }

            if (oldVersion < 3) {
                database.execSQL(
                        "ALTER TABLE invoices ADD COLUMN document_type TEXT NOT NULL DEFAULT 'فاکتور فروش کالا/خدمات'"
                );
                database.execSQL(
                        "ALTER TABLE invoices ADD COLUMN issue_date TEXT"
                );
                database.execSQL(
                        "UPDATE invoices SET issue_date = strftime('%Y-%m-%d', created / 1000, 'unixepoch') WHERE issue_date IS NULL OR issue_date=''"
                );
            }

            if (oldVersion < 4) {
                database.execSQL(
                        "ALTER TABLE invoice_items ADD COLUMN tax_percent INTEGER NOT NULL DEFAULT 0"
                );
                database.execSQL(
                        "ALTER TABLE invoice_items ADD COLUMN tax_amount INTEGER NOT NULL DEFAULT 0"
                );
                database.execSQL(
                        "ALTER TABLE invoice_items ADD COLUMN total INTEGER NOT NULL DEFAULT 0"
                );
            }
        }

        void delete(long id) {
            SQLiteDatabase database = getWritableDatabase();
            database.delete("invoice_items", "invoice_id=?", new String[]{String.valueOf(id)});
            database.delete("invoices", "id=?", new String[]{String.valueOf(id)});
        }
    }
                        }
