package com.codex.lemoneytracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TYPE_EXPENSE = "Expense";
    private static final String TYPE_INCOME = "Income";

    private static final String[] EXPENSE_CATEGORIES = {
            "Food", "Transport", "Bills", "Rent", "Shopping", "Health", "Education",
            "Entertainment", "Family", "Savings", "Other"
    };

    private static final String[] INCOME_CATEGORIES = {
            "Salary", "Freelance", "Business", "Gift", "Investment", "Refund", "Other"
    };

    private static final String[] PAYMENTS = {
            "Cash", "Card", "Vodafone Cash", "InstaPay", "Bank transfer", "Other wallet"
    };

    private static final String[] ACCOUNTS = {
            "Main wallet", "Cash pocket", "Bank account", "Emergency fund", "Savings"
    };

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private Db db;
    private String selectedMonth;

    private TextView incomeValue;
    private TextView expenseValue;
    private TextView balanceValue;
    private TextView budgetValue;
    private TextView budgetStatus;
    private ProgressBar budgetProgress;
    private EditText budgetInput;
    private EditText amountInput;
    private EditText dateInput;
    private EditText noteInput;
    private Spinner monthSpinner;
    private Spinner typeSpinner;
    private Spinner categorySpinner;
    private Spinner paymentSpinner;
    private Spinner accountSpinner;
    private LinearLayout transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new Db(this);
        selectedMonth = new SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().getTime());
        setContentView(buildUi());
        refreshAll();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color("#F7F5EE"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("LE Money", 30, color("#17211B"), true);
        root.addView(title);

        TextView subtitle = text("Track every pound you spend, earn, and budget each month.", 14, color("#526158"), false);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.setMargins(0, dp(4), 0, dp(14));
        root.addView(subtitle, subtitleParams);

        monthSpinner = spinner(monthChoices());
        root.addView(label("Month"));
        root.addView(monthSpinner, matchWrap());
        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = parent.getItemAtPosition(position).toString();
                refreshAll();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        LinearLayout summary = section();
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        summary.addView(row1, matchWrap());
        incomeValue = stat(row1, "Income", color("#1C7C54"));
        expenseValue = stat(row1, "Spent", color("#B93B3B"));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Params = matchWrap();
        row2Params.setMargins(0, dp(10), 0, 0);
        summary.addView(row2, row2Params);
        balanceValue = stat(row2, "Balance", color("#3267A8"));
        budgetValue = stat(row2, "Budget", color("#C89327"));

        budgetStatus = text("", 14, color("#526158"), false);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.setMargins(0, dp(12), 0, dp(8));
        summary.addView(budgetStatus, statusParams);

        budgetProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        budgetProgress.setMax(100);
        summary.addView(budgetProgress, matchWrap());
        root.addView(summary);

        LinearLayout budgetBox = section();
        budgetBox.addView(sectionTitle("Monthly budget"));
        budgetInput = input("Budget in LE", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        budgetBox.addView(budgetInput, matchWrap());
        Button saveBudget = button("Save budget", color("#1C7C54"));
        LinearLayout.LayoutParams buttonParams = matchWrap();
        buttonParams.setMargins(0, dp(10), 0, 0);
        budgetBox.addView(saveBudget, buttonParams);
        saveBudget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBudget();
            }
        });
        root.addView(budgetBox);

        LinearLayout form = section();
        form.addView(sectionTitle("Add transaction"));
        amountInput = input("Amount in LE", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amountInput, matchWrap());

        typeSpinner = spinner(new String[]{TYPE_EXPENSE, TYPE_INCOME});
        form.addView(label("Type"));
        form.addView(typeSpinner, matchWrap());

        categorySpinner = spinner(EXPENSE_CATEGORIES);
        form.addView(label("Category"));
        form.addView(categorySpinner, matchWrap());

        paymentSpinner = spinner(PAYMENTS);
        form.addView(label("Payment"));
        form.addView(paymentSpinner, matchWrap());

        accountSpinner = spinner(ACCOUNTS);
        form.addView(label("Account"));
        form.addView(accountSpinner, matchWrap());

        dateInput = input("Date", InputType.TYPE_NULL);
        dateInput.setFocusable(false);
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime()));
        form.addView(dateInput, matchWrap());
        dateInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseDate();
            }
        });

        noteInput = input("Note", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(noteInput, matchWrap());

        Button add = button("Add transaction", color("#3267A8"));
        LinearLayout.LayoutParams addParams = matchWrap();
        addParams.setMargins(0, dp(12), 0, 0);
        form.addView(add, addParams);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTransaction();
            }
        });

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean income = TYPE_INCOME.equals(parent.getItemAtPosition(position).toString());
                setSpinnerItems(categorySpinner, income ? INCOME_CATEGORIES : EXPENSE_CATEGORIES);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        root.addView(form);

        LinearLayout listBox = section();
        listBox.addView(sectionTitle("Transactions"));
        transactionList = new LinearLayout(this);
        transactionList.setOrientation(LinearLayout.VERTICAL);
        listBox.addView(transactionList, matchWrap());
        root.addView(listBox);

        return scroll;
    }

    private void refreshAll() {
        if (incomeValue == null) {
            return;
        }

        double income = db.sum(selectedMonth, TYPE_INCOME);
        double expense = db.sum(selectedMonth, TYPE_EXPENSE);
        double budget = db.budget(selectedMonth);
        double balance = income - expense;
        double budgetLeft = budget - expense;

        incomeValue.setText(le(income));
        expenseValue.setText(le(expense));
        balanceValue.setText(le(balance));
        budgetValue.setText(budget > 0 ? le(budget) : "Not set");
        budgetInput.setText(budget > 0 ? moneyFormat.format(budget).replace(",", "") : "");

        if (budget > 0) {
            int progress = (int) Math.min(100, Math.round((expense / budget) * 100));
            budgetProgress.setProgress(progress);
            if (budgetLeft >= 0) {
                budgetStatus.setText(le(budgetLeft) + " left from this month's budget");
                budgetStatus.setTextColor(color("#1C7C54"));
            } else {
                budgetStatus.setText(le(Math.abs(budgetLeft)) + " over this month's budget");
                budgetStatus.setTextColor(color("#B93B3B"));
            }
        } else {
            budgetProgress.setProgress(0);
            budgetStatus.setText("Set a budget to watch monthly spending.");
            budgetStatus.setTextColor(color("#526158"));
        }

        renderTransactions();
    }

    private void renderTransactions() {
        transactionList.removeAllViews();
        List<Tx> rows = db.transactions(selectedMonth);
        if (rows.isEmpty()) {
            TextView empty = text("No transactions for this month yet.", 15, color("#526158"), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(18), 0, dp(12));
            transactionList.addView(empty, matchWrap());
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            final Tx tx = rows.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            item.setBackground(cardBackground("#FFFFFF"));

            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setOrientation(LinearLayout.HORIZONTAL);
            item.addView(top, matchWrap());

            TextView main = text(tx.category + " · " + tx.type, 16, color("#17211B"), true);
            LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            top.addView(main, mainParams);

            int amountColor = TYPE_INCOME.equals(tx.type) ? color("#1C7C54") : color("#B93B3B");
            String prefix = TYPE_INCOME.equals(tx.type) ? "+ " : "- ";
            TextView amount = text(prefix + le(tx.amount), 16, amountColor, true);
            top.addView(amount);

            String details = tx.date + " · " + tx.payment + " · " + tx.account;
            if (tx.note.length() > 0) {
                details = details + "\n" + tx.note;
            }
            TextView sub = text(details, 13, color("#526158"), false);
            LinearLayout.LayoutParams subParams = matchWrap();
            subParams.setMargins(0, dp(6), 0, 0);
            item.addView(sub, subParams);

            Button delete = button("Delete", color("#B93B3B"));
            LinearLayout.LayoutParams deleteParams = matchWrap();
            deleteParams.setMargins(0, dp(10), 0, 0);
            item.addView(delete, deleteParams);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete(tx.id);
                }
            });

            LinearLayout.LayoutParams itemParams = matchWrap();
            itemParams.setMargins(0, i == 0 ? dp(8) : dp(10), 0, 0);
            transactionList.addView(item, itemParams);
        }
    }

    private void saveBudget() {
        double amount = parseAmount(budgetInput);
        if (amount < 0) {
            toast("Enter a valid budget.");
            return;
        }
        db.saveBudget(selectedMonth, amount);
        hideKeyboard();
        toast("Budget saved.");
        refreshAll();
    }

    private void addTransaction() {
        double amount = parseAmount(amountInput);
        if (amount <= 0) {
            toast("Enter an amount greater than 0.");
            return;
        }

        String date = dateInput.getText().toString().trim();
        if (date.length() != 10) {
            toast("Choose a valid date.");
            return;
        }

        String month = date.substring(0, 7);
        String type = typeSpinner.getSelectedItem().toString();
        String category = categorySpinner.getSelectedItem().toString();
        String payment = paymentSpinner.getSelectedItem().toString();
        String account = accountSpinner.getSelectedItem().toString();
        String note = noteInput.getText().toString().trim();

        db.add(amount, type, category, payment, account, note, date, month);
        amountInput.setText("");
        noteInput.setText("");
        hideKeyboard();

        selectedMonth = month;
        selectMonth(month);
        refreshAll();
        maybeWarnBudget();
        toast("Transaction added.");
    }

    private void maybeWarnBudget() {
        double budget = db.budget(selectedMonth);
        if (budget <= 0) {
            return;
        }
        double expense = db.sum(selectedMonth, TYPE_EXPENSE);
        if (expense > budget) {
            new AlertDialog.Builder(this)
                    .setTitle("Budget limit passed")
                    .setMessage("You are " + le(expense - budget) + " over the budget for " + selectedMonth + ".")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void confirmDelete(final long id) {
        new AlertDialog.Builder(this)
                .setTitle("Delete transaction?")
                .setMessage("This removes it from your monthly totals.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.delete(id);
                        refreshAll();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void chooseDate() {
        Calendar now = Calendar.getInstance();
        String current = dateInput.getText().toString();
        if (current.length() == 10) {
            try {
                String[] parts = current.split("-");
                now.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                now.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                now.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, dayOfMonth);
                        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.getTime()));
                    }
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private List<String> monthChoices() {
        ArrayList<String> months = new ArrayList<String>();
        Calendar cursor = Calendar.getInstance();
        cursor.add(Calendar.MONTH, -11);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM", Locale.US);
        for (int i = 0; i < 14; i++) {
            months.add(fmt.format(cursor.getTime()));
            cursor.add(Calendar.MONTH, 1);
        }
        return months;
    }

    private void selectMonth(String month) {
        for (int i = 0; i < monthSpinner.getCount(); i++) {
            if (month.equals(monthSpinner.getItemAtPosition(i).toString())) {
                monthSpinner.setSelection(i);
                return;
            }
        }
    }

    private TextView stat(LinearLayout row, String label, int valueColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(cardBackground("#F7F5EE"));

        TextView labelView = text(label, 12, color("#526158"), false);
        box.addView(labelView);

        TextView value = text("LE 0.00", 18, valueColor, true);
        LinearLayout.LayoutParams valueParams = matchWrap();
        valueParams.setMargins(0, dp(4), 0, 0);
        box.addView(value, valueParams);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        boxParams.setMargins(row.getChildCount() == 0 ? 0 : dp(8), 0, 0, 0);
        row.addView(box, boxParams);
        return value;
    }

    private LinearLayout section() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(cardBackground("#FFFFFF"));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(14), 0, 0);
        box.setLayoutParams(params);
        return box;
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 19, color("#17211B"), true);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(10));
        title.setLayoutParams(params);
        return title;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, color("#526158"), true);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(10), 0, dp(4));
        label.setLayoutParams(params);
        return label;
    }

    private EditText input(String hint, int type) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setInputType(type);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setTextColor(color("#17211B"));
        input.setHintTextColor(color("#7C8A82"));
        input.setBackground(cardBackground("#F7F5EE"));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, 0);
        input.setLayoutParams(params);
        return input;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        setSpinnerItems(spinner, values);
        return spinner;
    }

    private Spinner spinner(List<String> values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int current = values.indexOf(selectedMonth);
        if (current >= 0) {
            spinner.setSelection(current);
        }
        return spinner;
    }

    private void setSpinnerItems(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private Button button(String label, int bg) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(bg);
        shape.setCornerRadius(dp(8));
        button.setBackground(shape);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }
        text.setIncludeFontPadding(true);
        return text;
    }

    private GradientDrawable cardBackground(String fill) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color(fill));
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), color("#E2DED2"));
        return shape;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int color(String value) {
        return Color.parseColor(value);
    }

    private String le(double value) {
        return "LE " + moneyFormat.format(value);
    }

    private double parseAmount(EditText input) {
        try {
            String text = input.getText().toString().replace(",", "").trim();
            if (text.length() == 0) {
                return -1;
            }
            return Double.parseDouble(text);
        } catch (Exception ex) {
            return -1;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View focused = getCurrentFocus();
            if (imm != null && focused != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    private static class Tx {
        long id;
        double amount;
        String type;
        String category;
        String payment;
        String account;
        String note;
        String date;
    }

    private static class Db extends SQLiteOpenHelper {
        Db(Context context) {
            super(context, "le_money.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "amount REAL NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "payment TEXT NOT NULL, " +
                    "account TEXT NOT NULL, " +
                    "note TEXT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "month TEXT NOT NULL" +
                    ")");
            database.execSQL("CREATE TABLE budgets (" +
                    "month TEXT PRIMARY KEY, " +
                    "amount REAL NOT NULL" +
                    ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            database.execSQL("DROP TABLE IF EXISTS transactions");
            database.execSQL("DROP TABLE IF EXISTS budgets");
            onCreate(database);
        }

        void add(double amount, String type, String category, String payment, String account, String note, String date, String month) {
            ContentValues values = new ContentValues();
            values.put("amount", amount);
            values.put("type", type);
            values.put("category", category);
            values.put("payment", payment);
            values.put("account", account);
            values.put("note", note);
            values.put("date", date);
            values.put("month", month);
            getWritableDatabase().insert("transactions", null, values);
        }

        void delete(long id) {
            getWritableDatabase().delete("transactions", "id = ?", new String[]{String.valueOf(id)});
        }

        void saveBudget(String month, double amount) {
            ContentValues values = new ContentValues();
            values.put("month", month);
            values.put("amount", amount);
            getWritableDatabase().replace("budgets", null, values);
        }

        double budget(String month) {
            Cursor cursor = getReadableDatabase().query(
                    "budgets",
                    new String[]{"amount"},
                    "month = ?",
                    new String[]{month},
                    null,
                    null,
                    null
            );
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getDouble(0);
                }
                return 0;
            } finally {
                cursor.close();
            }
        }

        double sum(String month, String type) {
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE month = ? AND type = ?",
                    new String[]{month, type}
            );
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getDouble(0);
                }
                return 0;
            } finally {
                cursor.close();
            }
        }

        List<Tx> transactions(String month) {
            ArrayList<Tx> rows = new ArrayList<Tx>();
            Cursor cursor = getReadableDatabase().query(
                    "transactions",
                    new String[]{"id", "amount", "type", "category", "payment", "account", "note", "date"},
                    "month = ?",
                    new String[]{month},
                    null,
                    null,
                    "date DESC, id DESC"
            );
            try {
                while (cursor.moveToNext()) {
                    Tx tx = new Tx();
                    tx.id = cursor.getLong(0);
                    tx.amount = cursor.getDouble(1);
                    tx.type = cursor.getString(2);
                    tx.category = cursor.getString(3);
                    tx.payment = cursor.getString(4);
                    tx.account = cursor.getString(5);
                    tx.note = cursor.getString(6);
                    tx.date = cursor.getString(7);
                    rows.add(tx);
                }
            } finally {
                cursor.close();
            }
            return rows;
        }
    }
}
