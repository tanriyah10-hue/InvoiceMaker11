
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
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String COMPANY_NAME =
            " گروه فنی مهندسی حبیبی ایمن سولار سيستم";
        
    private static final String COMPANY_ADDRESS =
            "ارومیه، آزادگان ۱، کوی ۴";

    private static final String COMPANY_PHONE = "۰۹۱۴۶۰۷۷۶۲۶";

    private static final String TYPE_INVOICE =
            "فاکتور فروش کالا/خدمات";

    private static final String TYPE_PREINVOICE =
            "پیش‌فاکتور فروش کالا/خدمات";

    private static final float TABLE_LEFT = 45f;
    private static final float TABLE_RIGHT = 550f;
    private static final float TABLE_WIDTH =
            TABLE_RIGHT - TABLE_LEFT;

    private DB db;
    private ArrayAdapter<String> productAdapter;

    private EditText customer;
    private EditText phone;
    private EditText notes;

    private TextView totalView;
    private TextView issueDateView;

    private LinearLayout root;
    private LinearLayout itemContainer;

    private Spinner documentTypeSpinner;

    private Calendar issueDate = Calendar.getInstance();

    private final ArrayList<ItemRow> itemRows =
            new ArrayList<>();

    private final int backgroundColor = 0xFFF4F6F8;
    private final int primaryColor = 0xFF176B87;

    // اطلاعات هر ردیف کالا یا خدمات
    private class ItemRow {
        AutoCompleteTextView description;
        EditText qty;
        EditText price;
        EditText discount;
        EditText taxPercent;

        LinearLayout card;
    }

    // اطلاعات ذخیره‌شده هر ردیف برای ساخت PDF
    private static class ItemInfo {

        String description;

        long qty;
        long price;
        long discount;
        long taxPercent;
        long taxAmount;
        long rowTotal;

        ItemInfo(
                String description,
                long qty,
                long price,
                long discount,
                long taxPercent,
                long taxAmount,
                long rowTotal
        ) {
            this.description = description;
            this.qty = qty;
            this.price = price;
            this.discount = discount;
            this.taxPercent = taxPercent;
            this.taxAmount = taxAmount;
            this.rowTotal = rowTotal;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(0xFF124D63);
        getWindow().setNavigationBarColor(0xFF124D63);

        db = new DB(this);

        showForm();
    }

    // تبدیل واحد dp به پیکسل
    private int dp(float value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    // ساخت متن
    private TextView text(
            String value,
            int size,
            int color
    ) {

        TextView view = new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setPadding(
                dp(4),
                dp(7),
                dp(4),
                dp(7)
        );

        view.setGravity(Gravity.RIGHT);

        return view;
    }

    // ساخت فیلد ورودی
    private EditText field(String hint) {

        EditText editText = new EditText(this);

        editText.setHint(hint);
        editText.setTextSize(15);
        editText.setSingleLine(true);

        editText.setGravity(
                Gravity.RIGHT | Gravity.CENTER_VERTICAL
        );

        editText.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        editText.setBackgroundColor(Color.WHITE);

        editText.setLayoutDirection(
                View.LAYOUT_DIRECTION_RTL
        );

        return editText;
    }

    // ساخت دکمه
    private Button button(
            String label,
            Runnable action
    ) {

        Button button = new Button(this);

        button.setText(label);

        button.setOnClickListener(
                view -> action.run()
        );

        return button;
    }

    // ساخت صفحه اصلی
    private void base(String title) {

        root = new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(16),
                dp(12),
                dp(16),
                dp(16)
        );

        root.setBackgroundColor(backgroundColor);

        root.setLayoutDirection(
                View.LAYOUT_DIRECTION_RTL
        );

        ScrollView scrollView = new ScrollView(this);

        scrollView.addView(root);

        setContentView(scrollView);

        TextView heading = text(
                title,
                22,
                0xFF124D63
        );

        heading.setGravity(Gravity.CENTER);

        root.addView(heading);
    }

    // افزودن فیلد به فرم
    private void addField(
            String label,
            EditText editText
    ) {

        root.addView(
                text(label, 14, 0xFF34495E)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );

        params.bottomMargin = dp(5);

        root.addView(editText, params);
    }

    // نمایش فرم ثبت فاکتور
    private void showForm() {

        base("فاکتور ساز شخصی");

        root.addView(
                text("نوع سند", 14, 0xFF34495E)
        );

        documentTypeSpinner = new Spinner(this);

        String[] documentTypes = {
                TYPE_INVOICE,
                TYPE_PREINVOICE
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        documentTypes
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        documentTypeSpinner.setAdapter(adapter);

        root.addView(documentTypeSpinner);

        root.addView(
                text("تاریخ صدور", 14, 0xFF34495E)
        );

        issueDateView = text(
                "",
                16,
                primaryColor
        );

        issueDateView.setGravity(Gravity.CENTER);

        root.addView(issueDateView);

        updateIssueDateLabel();

        root.addView(
                button(
                        "انتخاب تاریخ صدور",
                        this::showDatePicker
                )
        );

        customer = field("نام مشتری");

        phone = field(
                "شماره تماس مشتری (اختیاری)"
        );

        notes = field("توضیحات فاکتور");

        addField("نام مشتری", customer);

        addField("شماره تماس مشتری", phone);

        root.addView(
                text(
                        "کالاها و خدمات",
                        18,
                        primaryColor
                )
        );

        itemContainer = new LinearLayout(this);

        itemContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        root.addView(itemContainer);

        // دریافت نام کالاها از بانک اطلاعاتی
        productAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                db.getProductNames()
        );

        itemRows.clear();

        addItemRow();

        root.addView(
                button(
                        "＋ افزودن ردیف کالا یا خدمات",
                        this::addItemRow
                )
        );

        totalView = text(
                "مبلغ نهایی: ۰ تومان",
                19,
                primaryColor
        );

        totalView.setGravity(Gravity.CENTER);

        root.addView(totalView);

        addField("توضیحات فاکتور", notes);

        root.addView(
                button(
                        "محاسبه مبلغ",
                        this::calculate
                )
        );

        root.addView(
                button(
                        "ذخیره فاکتور",
                        this::save
                )
        );

        root.addView(
                button(
                        "فاکتورها و پیش‌فاکتورهای ذخیره‌شده",
                        this::showList
                )
        );

        root.addView(
                text(
                        "اطلاعات در همین گوشی ذخیره می‌شود. برای نگهداری بلندمدت PDF بگیر.",
                        12,
                        0xFF666666
                )
        );
    }

    // انتخاب تاریخ صدور
    private void showDatePicker() {

        int year = issueDate.get(Calendar.YEAR);
        int month = issueDate.get(Calendar.MONTH);
        int day = issueDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (picker, selectedYear,
                         selectedMonth, selectedDay) -> {

                            issueDate.set(
                                    Calendar.YEAR,
                                    selectedYear
                            );

                            issueDate.set(
                                    Calendar.MONTH,
                                    selectedMonth
                            );

                            issueDate.set(
                                    Calendar.DAY_OF_MONTH,
                                    selectedDay
                            );

                            issueDate.set(
                                    Calendar.HOUR_OF_DAY,
                                    12
                            );

                            issueDate.set(
                                    Calendar.MINUTE,
                                    0
                            );

                            issueDate.set(
                                    Calendar.SECOND,
                                    0
                            );

                            issueDate.set(
                                    Calendar.MILLISECOND,
                                    0
                            );

                            updateIssueDateLabel();
                        },
                        year,
                        month,
                        day
                );

        dialog.show();
    }

    private void updateIssueDateLabel() {

        if (issueDateView != null) {

            issueDateView.setText(
                    "تاریخ انتخاب‌شده: "
                            + displayDate(issueDate)
            );
        }
    }

    private String displayDate(Calendar date) {
    return JalaliDate.format(date, "/");
}

    private String databaseDate(Calendar date) {
    return JalaliDate.format(date, "-");
}

    private String dateFromTimestamp(long timestamp) {
    Calendar date = Calendar.getInstance();
    date.setTimeInMillis(timestamp);
    return JalaliDate.format(date, "/");
}

    private String selectedDocumentType() {

        if (documentTypeSpinner == null
                || documentTypeSpinner.getSelectedItem() == null) {

            return TYPE_INVOICE;
        }

        return documentTypeSpinner
                .getSelectedItem()
                .toString();
    }

    // افزودن ردیف جدید
    private void addItemRow() {

        ItemRow row = new ItemRow();

        row.card = new LinearLayout(this);

        row.card.setOrientation(
                LinearLayout.VERTICAL
        );

        row.card.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        row.card.setBackgroundColor(Color.WHITE);

        // فیلد نام کالا با قابلیت تکمیل خودکار
        row.description = new AutoCompleteTextView(this);

        row.description.setHint(
                "نام کالا یا خدمات را تایپ کن"
        );

        row.description.setTextSize(15);

        row.description.setSingleLine(true);

        row.description.setGravity(
                Gravity.RIGHT | Gravity.CENTER_VERTICAL
        );

        row.description.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        row.description.setBackgroundColor(Color.WHITE);

        row.description.setLayoutDirection(
                View.LAYOUT_DIRECTION_RTL
        );

        row.description.setAdapter(productAdapter);

        // پیشنهادها از اولین حرف نمایش داده می‌شوند
        row.description.setThreshold(1);

        // با لمس فیلد، فهرست کالاهای ذخیره‌شده باز می‌شود
        row.description.setOnClickListener(v -> {

            if (productAdapter != null
                    && !productAdapter.isEmpty()) {

                row.description.showDropDown();
            }
        });

        row.qty = field("تعداد");

        row.qty.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        row.qty.setText("1");

        row.price = field(
                "قیمت واحد (تومان)"
        );

        row.price.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        row.discount = field(
                "تخفیف (تومان)"
        );

        row.discount.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        row.discount.setText("0");

        row.taxPercent = field(
                "درصد مالیات"
        );

        row.taxPercent.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        row.taxPercent.setText("0");

        row.card.addView(
                text(
                        "ردیف " + (itemRows.size() + 1),
                        16,
                        primaryColor
                )
        );

        row.card.addView(row.description);
        row.card.addView(row.qty);
        row.card.addView(row.price);
        row.card.addView(row.discount);
        row.card.addView(row.taxPercent);

        row.card.addView(
                button(
                        "حذف این ردیف",
                        () -> {

                            if (itemRows.size() <= 1) {

                                toast(
                                        "فاکتور باید حداقل یک ردیف داشته باشد"
                                );

                                return;
                            }

                            itemContainer.removeView(
                                    row.card
                            );

                            itemRows.remove(row);

                            updateRowNumbers();

                            updateTotal();
                        }
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = dp(8);

        itemContainer.addView(
                row.card,
                params
        );

        itemRows.add(row);
    }

    // شماره‌گذاری مجدد ردیف‌ها
    private void updateRowNumbers() {

        for (int i = 0; i < itemRows.size(); i++) {

            ItemRow row = itemRows.get(i);

            TextView title =
                    (TextView) row.card.getChildAt(0);

            title.setText(
                    "ردیف " + (i + 1)
            );
        }
    }

    // تبدیل اعداد فارسی و عربی به انگلیسی
    private long number(EditText editText) {

        String value = editText.getText()
                .toString()
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

            return value.isEmpty()
                    ? 0
                    : Long.parseLong(value);

        } catch (Exception ignored) {

            return 0;
        }
    }

    // نمایش مبلغ با جداکننده سه‌رقمی
    private String money(long value) {

        return String.format(
                Locale.US,
                "%,d",
                value
        );
    }

    // مبلغ ردیف قبل از مالیات
    private long rowBeforeTax(ItemRow row) {

        long quantity = number(row.qty);
        long price = number(row.price);
        long discount = number(row.discount);

        return Math.max(
                0,
                quantity * price - discount
        );
    }

    // محاسبه مالیات ردیف
    private long rowTaxAmount(ItemRow row) {

        long subtotal = rowBeforeTax(row);

        long taxPercent = number(
                row.taxPercent
        );

        if (taxPercent <= 0) {
            return 0;
        }

        return Math.round(
                subtotal * taxPercent / 100.0
        );
    }

    // مبلغ نهایی هر ردیف
    private long rowTotalWithTax(ItemRow row) {

        return rowBeforeTax(row)
                + rowTaxAmount(row);
    }

    // مبلغ کل فاکتور
    private long invoiceAmount() {

        long total = 0;

        for (ItemRow row : itemRows) {

            total += rowTotalWithTax(row);
        }

        return total;
    }

    // به‌روزرسانی مبلغ نهایی
    private void updateTotal() {

        if (totalView != null) {

            totalView.setText(
                    "مبلغ نهایی: "
                            + money(invoiceAmount())
                            + " تومان"
            );
        }
    }

    // اعتبارسنجی اطلاعات ردیف‌ها
    private boolean validateItems() {

        if (itemRows.isEmpty()) {

            toast(
                    "حداقل یک ردیف کالا یا خدمات اضافه کن"
            );

            return false;
        }

        for (int i = 0; i < itemRows.size(); i++) {

            ItemRow row = itemRows.get(i);

            if (row.description.getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                toast(
                        "شرح ردیف " + (i + 1)
                                + " را وارد کن"
                );

                return false;
            }

            if (number(row.qty) < 1) {

                toast(
                        "تعداد ردیف " + (i + 1)
                                + " باید حداقل ۱ باشد"
                );

                return false;
            }
        }

        return true;
    }

    // محاسبه فاکتور
    private void calculate() {

        if (!validateItems()) {
            return;
        }

        updateTotal();

        toast("محاسبه انجام شد");
    }

    // ذخیره فاکتور در پایگاه داده
    private void save() {

        String customerName = customer.getText()
                .toString()
                .trim();

        if (customerName.isEmpty()) {

            toast("نام مشتری را وارد کن");

            return;
        }

        if (!validateItems()) {
            return;
        }

        SQLiteDatabase sql =
                db.getWritableDatabase();

        ContentValues invoice =
                new ContentValues();

        invoice.put(
                "customer",
                customerName
        );

        invoice.put(
                "phone",
                phone.getText().toString().trim()
        );

        invoice.put(
                "notes",
                notes.getText().toString().trim()
        );

        invoice.put(
                "total",
                invoiceAmount()
        );

        invoice.put(
                "created",
                System.currentTimeMillis()
        );

        invoice.put(
                "document_type",
                selectedDocumentType()
        );

        invoice.put(
                "issue_date",
                databaseDate(issueDate)
        );

        long invoiceId = -1;

        boolean saved = false;

        sql.beginTransaction();

        try {

            invoiceId = sql.insertOrThrow(
                    "invoices",
                    null,
                    invoice
            );

            for (ItemRow row : itemRows) {

                long taxAmount =
                        rowTaxAmount(row);

                long rowTotal =
                        rowBeforeTax(row) + taxAmount;

                String productName =
                        row.description.getText()
                                .toString()
                                .trim();

                ContentValues item =
                        new ContentValues();

                item.put(
                        "invoice_id",
                        invoiceId
                );

                item.put(
                        "description",
                        productName
                );

                item.put(
                        "qty",
                        number(row.qty)
                );

                item.put(
                        "price",
                        number(row.price)
                );

                item.put(
                        "discount",
                        number(row.discount)
                );

                item.put(
                        "tax_percent",
                        number(row.taxPercent)
                );

                item.put(
                        "tax_amount",
                        taxAmount
                );

                item.put(
                        "total",
                        rowTotal
                );

                sql.insertOrThrow(
                        "invoice_items",
                        null,
                        item
                );

                // ذخیره نام کالا در بانک کالاها
                db.saveProductName(
                        sql,
                        productName
                );
            }

            sql.setTransactionSuccessful();

            saved = true;

        } catch (Exception e) {

            toast(
                    "ذخیره سند انجام نشد: "
                            + e.getMessage()
            );

        } finally {

            sql.endTransaction();
        }

        if (saved && invoiceId > 0) {

            // تازه‌سازی پیشنهادهای کالا پس از ذخیره موفق
            productAdapter.clear();

            productAdapter.addAll(
                    db.getProductNames()
            );

            productAdapter.notifyDataSetChanged();

            updateTotal();

            hideKeyboard();

            long savedId = invoiceId;

            new AlertDialog.Builder(this)
                    .setMessage(
                            "سند ذخیره شد. می‌خواهی PDF آن را بسازی؟"
                    )
                    .setPositiveButton(
                            "ساخت PDF",
                            (dialog, which) ->
                                    makePdf(savedId)
                    )
                    .setNegativeButton(
                            "بستن",
                            null
                    )
                    .show();
        }
    }

    // نمایش فهرست اسناد ذخیره‌شده
    private void showList() {

        base(
                "فاکتورها و پیش‌فاکتورهای ذخیره‌شده"
        );

        root.addView(
                button(
                        "بازگشت به فرم ثبت",
                        this::showForm
                )
        );

        Cursor cursor =
                db.getReadableDatabase().rawQuery(
                        "SELECT id, customer, total, created, " +
                                "document_type, issue_date " +
                                "FROM invoices ORDER BY id DESC",
                        null
                );

        if (cursor.getCount() == 0) {

            root.addView(
                    text(
                            "هنوز سندی ذخیره نشده است.",
                            16,
                            0xFF555555
                    )
            );
        }

        while (cursor.moveToNext()) {

            long id = cursor.getLong(0);

            String customerName =
                    cursor.getString(1);

            long total = cursor.getLong(2);

            long created = cursor.getLong(3);

            String typeText =
                    cursor.getString(4);

            String issueDateText =
                    cursor.getString(5);

            if (typeText == null
                    || typeText.trim().isEmpty()) {

                typeText = TYPE_INVOICE;
            }

            if (issueDateText == null
                    || issueDateText.trim().isEmpty()) {

                issueDateText =
                        dateFromTimestamp(created);

            } else {

                issueDateText =
                        issueDateText.replace('-', '/');
            }

            LinearLayout card =
                    new LinearLayout(this);

            card.setOrientation(
                    LinearLayout.VERTICAL
            );

            card.setPadding(
                    dp(10),
                    dp(8),
                    dp(10),
                    dp(8)
            );

            card.setBackgroundColor(Color.WHITE);

            card.addView(
                    text(
                            typeText,
                            16,
                            primaryColor
                    )
            );

            card.addView(
                    text(
                            "مشتری: " + customerName,
                            15,
                            0xFF222222
                    )
            );

            card.addView(
                    text(
                            "تاریخ صدور: " + issueDateText,
                            13,
                            0xFF555555
                    )
            );

            card.addView(
                    text(
                            "مبلغ: " + money(total)
                                    + " تومان",
                            14,
                            primaryColor
                    )
            );

            card.addView(
                    button(
                            "ساخت و اشتراک PDF",
                            () -> makePdf(id)
                    )
            );

            card.addView(
                    button(
                            "حذف سند",
                            () ->

                                    new AlertDialog.Builder(this)
                                            .setMessage(
                                                    "این سند حذف شود؟"
                                            )
                                            .setPositiveButton(
                                                    "حذف",
                                                    (dialog, which) -> {

                                                        db.delete(id);

                                                        showList();
                                                    }
                                            )
                                            .setNegativeButton(
                                                    "انصراف",
                                                    null
                                            )
                                            .show()
                    )
            );

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

    private String dateFromTimestamp(long timestamp) {

        return new SimpleDateFormat(
                "yyyy/MM/dd",
                Locale.US
        ).format(new Date(timestamp));
    }

    // ساخت PDF
    private void makePdf(long id) {

        Cursor invoiceCursor =
                db.getReadableDatabase().rawQuery(
                        "SELECT customer, phone, notes, total, created, " +
                                "document_type, issue_date " +
                                "FROM invoices WHERE id=?",
                        new String[]{
                                String.valueOf(id)
                        }
                );

        if (!invoiceCursor.moveToFirst()) {

            invoiceCursor.close();

            toast("سند پیدا نشد");

            return;
        }

        String customerName =
                invoiceCursor.getString(0);

        String phoneNumber =
                invoiceCursor.getString(1);

        String invoiceNotes =
                invoiceCursor.getString(2);

        long total =
                invoiceCursor.getLong(3);

        long created =
                invoiceCursor.getLong(4);

        String docType =
                invoiceCursor.getString(5);

        String issueDateText =
                invoiceCursor.getString(6);

        invoiceCursor.close();

        if (docType == null
                || docType.trim().isEmpty()) {

            docType = TYPE_INVOICE;
        }

        if (issueDateText == null
                || issueDateText.trim().isEmpty()) {

            issueDateText =
                    dateFromTimestamp(created);

        } else {

            issueDateText =
                    issueDateText.replace('-', '/');
        }

        Cursor itemsCursor =
                db.getReadableDatabase().rawQuery(
                        "SELECT description, qty, price, discount, " +
                                "tax_percent, tax_amount, total " +
                                "FROM invoice_items " +
                                "WHERE invoice_id=? ORDER BY id",
                        new String[]{
                                String.valueOf(id)
                        }
                );

        ArrayList<ItemInfo> rows =
                new ArrayList<>();

        long subtotal = 0;
        long discountTotal = 0;
        long taxTotal = 0;

        while (itemsCursor.moveToNext()) {

            String description =
                    itemsCursor.getString(0);

            long qty =
                    itemsCursor.getLong(1);

            long price =
                    itemsCursor.getLong(2);

            long discount =
                    itemsCursor.getLong(3);

            long taxPercent =
                    itemsCursor.getLong(4);

            long taxAmount =
                    itemsCursor.getLong(5);

            long rowTotal =
                    itemsCursor.getLong(6);

            subtotal += qty * price;

            discountTotal += discount;

            taxTotal += taxAmount;

            rows.add(
                    new ItemInfo(
                            description,
                            qty,
                            price,
                            discount,
                            taxPercent,
                            taxAmount,
                            rowTotal
                    )
            );
        }

        itemsCursor.close();

        PdfDocument pdf = new PdfDocument();

        try {

            PdfPageWriter writer =
                    new PdfPageWriter(
                            pdf,
                            customerName,
                            phoneNumber,
                            docType,
                            issueDateText,
                            created
                    );

            writer.drawTableHeader();

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
            }

            writer.drawSummary(
                    subtotal,
                    discountTotal,
                    taxTotal,
                    total,
                    invoiceNotes
            );

            writer.finish();

            File dir = new File(
                    getExternalFilesDir(null),
                    "Invoices"
            );

            if (!dir.exists() && !dir.mkdirs()) {

                throw new Exception(
                        "ساخت پوشه PDF ناموفق بود"
                );
            }

            File file = new File(
                    dir,
                    "invoice-" + id + ".pdf"
            );

            try (
                    FileOutputStream out =
                            new FileOutputStream(file)
            ) {

                pdf.writeTo(out);
            }

            Uri uri =
                    FileProvider.getUriForFile(
                            this,
                            "ir.personal.smartinvoice.fileprovider",
                            file
                    );

            Intent send =
                    new Intent(Intent.ACTION_SEND);

            send.setType("application/pdf");

            send.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            send.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            send,
                            "اشتراک‌گذاری PDF"
                    )
            );

        } catch (Exception e) {

            toast(
                    "ساخت PDF انجام نشد: "
                            + e.getMessage()
            );

        } finally {

            pdf.close();
        }
    }

    // مدیریت صفحات PDF
    private class PdfPageWriter {

        private final PdfDocument pdf;

        private final String customerName;
        private final String customerPhone;
        private final String documentType;
        private final String issueDateText;

        private final long created;

        private final float[] columnWidths = {
                40f,
                110f,
                45f,
                65f,
                58f,
                50f,
                62f,
                75f
        };

        private final String[] headers = {
                "ردیف",
                "شرح کالا یا خدمات",
                "تعداد",
                "قیمت واحد",
                "تخفیف",
                "مالیات ٪",
                "مبلغ مالیات",
                "جمع ردیف"
        };

        private PdfDocument.Page page;

        private Canvas canvas;

        private int pageNumber = 0;

        private float y;

        PdfPageWriter(
                PdfDocument pdf,
                String customerName,
                String customerPhone,
                String documentType,
                String issueDateText,
                long created
        ) {

            this.pdf = pdf;

            this.customerName =
                    customerName == null
                            ? ""
                            : customerName;

            this.customerPhone =
                    customerPhone == null
                            ? ""
                            : customerPhone;

            this.documentType =
                    documentType == null
                            ? ""
                            : documentType;

            this.issueDateText =
                    issueDateText == null
                            ? ""
                            : issueDateText;

            this.created = created;

            newPage();
        }

        // ساخت قلم فارسی
        private TextPaint makeTextPaint(
                float size,
                boolean bold
        ) {

            TextPaint textPaint =
                    new TextPaint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            textPaint.setColor(Color.BLACK);

            textPaint.setTextSize(size);

            textPaint.setTypeface(
                    Typeface.create(
                            "sans-serif",
                            bold
                                    ? Typeface.BOLD
                                    : Typeface.NORMAL
                    )
            );

            return textPaint;
        }

        // ساخت چیدمان متن راست‌چین و وسط‌چین
        private StaticLayout makeRtlLayout(
                String value,
                float width,
                float textSize,
                boolean bold
        ) {

            String safeValue =
                    value == null ? "" : value;

            int layoutWidth =
                    Math.max(1, (int) width);

            return StaticLayout.Builder
                    .obtain(
                            safeValue,
                            0,
                            safeValue.length(),
                            makeTextPaint(
                                    textSize,
                                    bold
                            ),
                            layoutWidth
                    )
                    .setAlignment(
                            Layout.Alignment.ALIGN_CENTER
                    )
                    .setTextDirection(
                            TextDirectionHeuristics.RTL
                    )
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1f)
                    .build();
        }

        // ساخت چیدمان متن داخل سلول جدول
        private StaticLayout makeRtlCellLayout(
                String value,
                float width,
                float textSize,
                boolean bold
        ) {

            String safeValue =
                    value == null ? "" : value;

            int layoutWidth =
                    Math.max(1, (int) width);

            return StaticLayout.Builder
                    .obtain(
                            safeValue,
                            0,
                            safeValue.length(),
                            makeTextPaint(
                                    textSize,
                                    bold
                            ),
                            layoutWidth
                    )
                    .setAlignment(
                            Layout.Alignment.ALIGN_NORMAL
                    )
                    .setTextDirection(
                            TextDirectionHeuristics.RTL
                    )
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1f)
                    .build();
        }

        // رسم متن فارسی در محدوده مشخص
        private void drawRtlText(
                String value,
                float left,
                float top,
                float width,
                float height,
                float textSize,
                boolean bold
        ) {

            StaticLayout layout =
                    makeRtlLayout(
                            value,
                            Math.max(1f, width - 8f),
                            textSize,
                            bold
                    );

            float textTop =
                    top + Math.max(
                            0f,
                            (height - layout.getHeight()) / 2f
                    );

            canvas.save();

            canvas.clipRect(
                    left + 2f,
                    top + 2f,
                    left + width - 2f,
                    top + height - 2f
            );

            canvas.translate(
                    left + 4f,
                    textTop
            );

            layout.draw(canvas);

            canvas.restore();
        }

        // ایجاد صفحه جدید PDF
        private void newPage() {

            if (page != null) {

                pdf.finishPage(page);
            }

            pageNumber++;

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(
                            595,
                            842,
                            pageNumber
                    ).create();

            page = pdf.startPage(info);

            canvas = page.getCanvas();

            Paint borderPaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            borderPaint.setColor(Color.BLACK);

            borderPaint.setStyle(
                    Paint.Style.STROKE
            );

            borderPaint.setStrokeWidth(1.5f);

            canvas.drawRect(
                    28f,
                    28f,
                    567f,
                    814f,
                    borderPaint
            );

            drawRtlText(
                    COMPANY_NAME,
                    TABLE_LEFT,
                    38f,
                    TABLE_WIDTH,
                    32f,
                    18f,
                    true
            );

            drawRtlText(
                    "آدرس: " + COMPANY_ADDRESS,
                    TABLE_LEFT,
                    72f,
                    TABLE_WIDTH,
                    22f,
                    11f,
                    false
            );

            String phoneLtr =
                    "\u2066" + COMPANY_PHONE + "\u2069";

            drawRtlText(
                    "تلفن شرکت: " + phoneLtr,
                    TABLE_LEFT,
                    94f,
                    TABLE_WIDTH,
                    22f,
                    11f,
                    false
            );

            drawRtlText(
                    documentType,
                    TABLE_LEFT,
                    122f,
                    TABLE_WIDTH,
                    30f,
                    16f,
                    true
            );

            Paint linePaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            linePaint.setColor(Color.BLACK);

            linePaint.setStrokeWidth(1f);

            canvas.drawLine(
                    45f,
                    157f,
                    550f,
                    157f,
                    linePaint
            );

            drawRtlText(
                    "مشتری: " + customerName,
                    TABLE_LEFT,
                    165f,
                    TABLE_WIDTH,
                    22f,
                    11f,
                    false
            );

            drawRtlText(
                    "تلفن مشتری: " + customerPhone,
                    TABLE_LEFT,
                    188f,
                    TABLE_WIDTH,
                    22f,
                    11f,
                    false
            );

            drawRtlText(
                    "تاریخ صدور: " + issueDateText,
                    TABLE_LEFT,
                    211f,
                    TABLE_WIDTH,
                    22f,
                    11f,
                    false
            );

            String createdText =
                    new SimpleDateFormat(
                            "yyyy/MM/dd - HH:mm",
                            Locale.US
                    ).format(new Date(created));

            drawRtlText(
                    "زمان ثبت: " + createdText,
                    TABLE_LEFT,
                    234f,
                    TABLE_WIDTH,
                    22f,
                    10f,
                    false
            );

            canvas.drawLine(
                    45f,
                    265f,
                    550f,
                    265f,
                    linePaint
            );

            y = 282f;
        }

        float getCurrentY() {

            return y;
        }

        // رسم عنوان جدول
        void drawTableHeader() {

            drawTableRow(
                    headers,
                    true
            );
        }

        // رسم ردیف کالا یا خدمات
        void drawItemRow(
                int index,
                String description,
                long qty,
                long price,
                long discount,
                long taxPercent,
                long taxAmount,
                long rowTotal
        ) {

            String[] values = {

                    String.valueOf(index),

                    description == null
                            ? ""
                            : description,

                    String.valueOf(qty),

                    money(price),

                    money(discount),

                    String.valueOf(taxPercent),

                    money(taxAmount),

                    money(rowTotal)
            };

            drawTableRow(
                    values,
                    false
            );
        }

        // رسم جدول و سلول‌ها
        private void drawTableRow(
                String[] values,
                boolean isHeader
        ) {

            StaticLayout[] layouts =
                    new StaticLayout[values.length];

            float rowHeight =
                    isHeader ? 42f : 34f;

            for (int i = 0; i < values.length; i++) {

                layouts[i] =
                        makeRtlCellLayout(
                                values[i],
                                columnWidths[i] - 8f,
                                isHeader ? 8f : 9f,
                                isHeader
                        );

                rowHeight = Math.max(
                        rowHeight,
                        layouts[i].getHeight() + 10f
                );
            }

            if (!isHeader && y + rowHeight > 785f) {

                newPage();

                drawTableHeader();
            }

            Paint cellPaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            cellPaint.setColor(Color.BLACK);

            cellPaint.setStyle(
                    Paint.Style.STROKE
            );

            cellPaint.setStrokeWidth(
                    isHeader ? 1.1f : 0.8f
            );

            float cellRight = TABLE_RIGHT;

            for (int i = 0; i < values.length; i++) {

                float cellLeft =
                        cellRight - columnWidths[i];

                canvas.drawRect(
                        cellLeft,
                        y,
                        cellRight,
                        y + rowHeight,
                        cellPaint
                );

                StaticLayout layout =
                        layouts[i];

                float textTop =
                        y + Math.max(
                                0f,
                                (rowHeight - layout.getHeight()) / 2f
                        );

                canvas.save();

                canvas.clipRect(
                        cellLeft + 2f,
                        y + 2f,
                        cellRight - 2f,
                        y + rowHeight - 2f
                );

                canvas.translate(
                        cellLeft + 4f,
                        textTop
                );

                layout.draw(canvas);

                canvas.restore();

                cellRight = cellLeft;
            }

            y += rowHeight;
        }

        // جمع‌بندی فاکتور
        void drawSummary(
                long subtotal,
                long discountTotal,
                long taxTotal,
                long grandTotal,
                String noteText
        ) {

            String[] summaryLines = {

                    "جمع مبلغ کالاها پیش از تخفیف: "
                            + money(subtotal) + " تومان",

                    "جمع تخفیف: "
                            + money(discountTotal) + " تومان",

                    "جمع مالیات: "
                            + money(taxTotal) + " تومان",

                    "جمع نهایی: "
                            + money(grandTotal) + " تومان"
            };

            float lineHeight = 24f;

            float summaryHeight =
                    lineHeight * summaryLines.length;

            StaticLayout noteLayout = null;

            float noteHeight = 0f;

            if (noteText != null
                    && !noteText.trim().isEmpty()) {

                noteLayout =
                        makeRtlLayout(
                                "توضیحات: "
                                        + noteText.trim(),
                                TABLE_WIDTH - 16f,
                                10f,
                                false
                        );

                noteHeight = Math.max(
                        34f,
                        noteLayout.getHeight() + 14f
                );
            }

            float requiredHeight =
                    summaryHeight
                            + (noteHeight > 0f
                            ? noteHeight + 8f
                            : 0f);

            if (y + requiredHeight > 785f) {

                newPage();
            }

            Paint boxPaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);

            boxPaint.setColor(Color.BLACK);

            boxPaint.setStyle(
                    Paint.Style.STROKE
            );

            boxPaint.setStrokeWidth(1f);

            float top = y;

            canvas.drawRect(
                    TABLE_LEFT,
                    top,
                    TABLE_RIGHT,
                    top + summaryHeight,
                    boxPaint
            );

            for (int i = 1;
                 i < summaryLines.length;
                 i++) {

                float lineY =
                        top + i * lineHeight;

                canvas.drawLine(
                        TABLE_LEFT,
                        lineY,
                        TABLE_RIGHT,
                        lineY,
                        boxPaint
                );
            }

            for (int i = 0;
                 i < summaryLines.length;
                 i++) {

                drawRtlText(
                        summaryLines[i],
                        TABLE_LEFT + 4f,
                        top + i * lineHeight + 2f,
                        TABLE_WIDTH - 8f,
                        lineHeight - 4f,
                        i == summaryLines.length - 1
                                ? 10f
                                : 9f,
                        i == summaryLines.length - 1
                );
            }

            y = top + summaryHeight + 8f;

            if (noteLayout != null) {

                canvas.drawRect(
                        TABLE_LEFT,
                        y,
                        TABLE_RIGHT,
                        y + noteHeight,
                        boxPaint
                );

                float noteTop =
                        y + Math.max(
                                5f,
                                (noteHeight - noteLayout.getHeight()) / 2f
                        );

                canvas.save();

                canvas.clipRect(
                        TABLE_LEFT + 3f,
                        y + 3f,
                        TABLE_RIGHT - 3f,
                        y + noteHeight - 3f
                );

                canvas.translate(
                        TABLE_LEFT + 8f,
                        noteTop
                );

                noteLayout.draw(canvas);

                canvas.restore();

                y += noteHeight;
            }
        }

        private String money(long value) {

            return String.format(
                    Locale.US,
                    "%,d",
                    value
            );
        }

        // پایان ساخت PDF
        void finish() {

            if (page != null) {

                pdf.finishPage(page);

                page = null;
            }
        }
    }

    // بستن صفحه‌کلید
    private void hideKeyboard() {

        try {

            InputMethodManager manager =
                    (InputMethodManager)
                            getSystemService(
                                    INPUT_METHOD_SERVICE
                            );

            manager.hideSoftInputFromWindow(
                    root.getWindowToken(),
                    0
            );

        } catch (Exception ignored) {
        }
    }

    // نمایش پیام
    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // پایگاه داده
    private class DB extends SQLiteOpenHelper {

        DB(Context context) {

            super(
                    context,
                    "invoice_personal.db",
                    null,
                    5
            );
        }

        @Override
        public void onCreate(
                SQLiteDatabase database
        ) {

            database.execSQL(
                    "CREATE TABLE invoices(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "customer TEXT," +
                            "phone TEXT," +
                            "notes TEXT," +
                            "total INTEGER," +
                            "created INTEGER," +
                            "document_type TEXT NOT NULL DEFAULT " +
                            "'فاکتور فروش کالا/خدمات'," +
                            "issue_date TEXT)"
            );

            createItemsTable(database);

            createProductTable(database);
        }

        // ایجاد جدول ردیف‌های فاکتور
        private void createItemsTable(
                SQLiteDatabase database
        ) {

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

        // ایجاد جدول قدیمی برای مهاجرت نسخه‌های قبلی
        private void createLegacyItemsTable(
                SQLiteDatabase database
        ) {

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

        // ایجاد بانک کالاها و خدمات
        private void createProductTable(
                SQLiteDatabase database
        ) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS product_catalog(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT NOT NULL COLLATE NOCASE UNIQUE)"
            );
        }

        // دریافت فهرست نام کالاها
        ArrayList<String> getProductNames() {

            ArrayList<String> products =
                    new ArrayList<>();

            SQLiteDatabase database =
                    getReadableDatabase();

            Cursor cursor = database.rawQuery(
                    "SELECT name FROM product_catalog " +
                            "ORDER BY name COLLATE NOCASE",
                    null
            );

            try {

                while (cursor.moveToNext()) {

                    products.add(
                            cursor.getString(0)
                    );
                }

            } finally {

                cursor.close();
            }

            return products;
        }

        // ذخیره نام کالا بدون ثبت موارد تکراری
        void saveProductName(
                SQLiteDatabase database,
                String name
        ) {

            if (name == null
                    || name.trim().isEmpty()) {

                return;
            }

            ContentValues values =
                    new ContentValues();

            values.put(
                    "name",
                    name.trim()
            );

            database.insertWithOnConflict(
                    "product_catalog",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
        }

        // بررسی وجود یک ستون در جدول
        private boolean columnExists(
                SQLiteDatabase database,
                String table,
                String column
        ) {

            Cursor cursor = database.rawQuery(
                    "PRAGMA table_info(" + table + ")",
                    null
            );

            try {

                while (cursor.moveToNext()) {

                    String name =
                            cursor.getString(1);

                    if (column.equals(name)) {

                        return true;
                    }
                }

            } finally {

                cursor.close();
            }

            return false;
        }

        // اضافه‌کردن ستون فقط در صورت نبودن آن
        private void addColumnIfMissing(
                SQLiteDatabase database,
                String table,
                String column,
                String definition
        ) {

            if (!columnExists(
                    database,
                    table,
                    column
            )) {

                database.execSQL(
                        "ALTER TABLE "
                                + table
                                + " ADD COLUMN "
                                + column
                                + " "
                                + definition
                );
            }
        }

        @Override
        public void onUpgrade(
                SQLiteDatabase database,
                int oldVersion,
                int newVersion
        ) {

            if (oldVersion < 2) {

                createLegacyItemsTable(database);

                database.execSQL(
                        "INSERT INTO invoice_items " +
                                "(invoice_id, description, qty, price, discount) " +
                                "SELECT id, description, qty, price, discount " +
                                "FROM invoices"
                );

            } else {

                createLegacyItemsTable(database);
            }

            if (oldVersion < 3) {

                addColumnIfMissing(
                        database,
                        "invoices",
                        "document_type",
                        "TEXT NOT NULL DEFAULT 'فاکتور فروش کالا/خدمات'"
                );

                addColumnIfMissing(
                        database,
                        "invoices",
                        "issue_date",
                        "TEXT"
                );

                database.execSQL(
                        "UPDATE invoices SET issue_date = " +
                                "strftime('%Y-%m-%d', created / 1000, 'unixepoch') " +
                                "WHERE issue_date IS NULL OR issue_date=''"
                );
            }

            if (oldVersion < 4) {

                addColumnIfMissing(
                        database,
                        "invoice_items",
                        "tax_percent",
                        "INTEGER NOT NULL DEFAULT 0"
                );

                addColumnIfMissing(
                        database,
                        "invoice_items",
                        "tax_amount",
                        "INTEGER NOT NULL DEFAULT 0"
                );

                addColumnIfMissing(
                        database,
                        "invoice_items",
                        "total",
                        "INTEGER NOT NULL DEFAULT 0"
                );
            }

            // ارتقای بانک کالاها به نسخه ۵
            if (oldVersion < 5) {

                createProductTable(database);
            }
        }

        // حذف فاکتور و ردیف‌های وابسته به آن
        void delete(long id) {

            SQLiteDatabase database =
                    getWritableDatabase();

            database.delete(
                    "invoice_items",
                    "invoice_id=?",
                    new String[]{
                            String.valueOf(id)
                    }
            );

            database.delete(
                    "invoices",
                    "id=?",
                    new String[]{
                            String.valueOf(id)
                    }
            );
        }
    }
}
