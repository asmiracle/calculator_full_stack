
package com.example.javacalculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import java.util.ArrayList;
import org.json.JSONObject;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView display;
    private TextView status;
    private Button equalsButton;

    private String firstNumber = "";
    private final ArrayList<String> historyList = new ArrayList<>();
    private SharedPreferences prefs;
    private static final int MAX_HISTORY = 20;
    private String operator = "";
    private boolean startNewNumber = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);
        status = findViewById(R.id.status);
        equalsButton = findViewById(R.id.equals);
        prefs = getSharedPreferences("calculator", MODE_PRIVATE);
        status.setMovementMethod(new ScrollingMovementMethod());
        loadSavedHistory();

        int[] numberIds = {
                R.id.n0, R.id.n1, R.id.n2, R.id.n3,
                R.id.n4, R.id.n5, R.id.n6, R.id.n7,
                R.id.n8, R.id.n9
        };

        for (int id : numberIds) {
            Button button = findViewById(id);

            button.setOnClickListener(v -> {
                String digit = button.getText().toString();
                String current = display.getText().toString();

                if (startNewNumber || current.equals("0")
                        || current.equals("Error")) {
                    display.setText(digit);
                    startNewNumber = false;
                } else if (current.length() < 15) {
                    display.append(digit);
                }
            });
        }

        findViewById(R.id.dot).setOnClickListener(v -> {
            String current = display.getText().toString();

            if (startNewNumber || current.equals("Error")) {
                display.setText("0.");
                startNewNumber = false;
            } else if (!current.contains(".")) {
                display.append(".");
            }
        });

        setOperator(R.id.plus, "+");
        setOperator(R.id.minus, "-");
        setOperator(R.id.multiply, "*");
        setOperator(R.id.divide, "/");

        findViewById(R.id.clear).setOnClickListener(v -> clear());

        findViewById(R.id.delete).setOnClickListener(v -> {
            String current = display.getText().toString();

            if (startNewNumber || current.equals("Error")
                    || current.length() <= 1) {
                display.setText("0");
                startNewNumber = false;
            } else {
                display.setText(current.substring(
                        0, current.length() - 1));
            }
        });

        equalsButton.setOnClickListener(v -> calculate());

        findViewById(R.id.history).setOnClickListener(v ->
                loadHistory());
    }

    private void setOperator(int id, String symbol) {
        findViewById(id).setOnClickListener(v -> {
            try {
                firstNumber = display.getText().toString();
                Double.parseDouble(firstNumber);

                operator = symbol;
                startNewNumber = true;
                status.setText("");
            } catch (NumberFormatException e) {
                status.setText("Enter a valid number first.");
            }
        });
    }

    private void calculate() {
        if (firstNumber.isEmpty() || operator.isEmpty()
                || !equalsButton.isEnabled()) {
            return;
        }

        final double a;
        final double b;

        try {
            a = Double.parseDouble(firstNumber);
            b = Double.parseDouble(display.getText().toString());
        } catch (NumberFormatException e) {
            status.setText("Enter valid numbers.");
            return;
        }

        if (operator.equals("/") && b == 0) {
            status.setText("Cannot divide by zero.");
            return;
        }

        final String op = operator;

        // 1. Calculate on the phone and show the answer right away
        double localResult;
        switch (op) {
            case "+": localResult = a + b; break;
            case "-": localResult = a - b; break;
            case "*": localResult = a * b; break;
            case "/": localResult = a / b; break;
            default:
                status.setText("Unknown operator.");
                return;
        }
        display.setText(formatResult(localResult));
        addToHistory(formatResult(a) + " " + toSymbol(op) + " "
                + formatResult(b) + " = " + formatResult(localResult));

        display.setText(formatResult(localResult));
        firstNumber = "";
        operator = "";
        startNewNumber = true;

        // 2. Send to the server to save in PostgreSQL
        equalsButton.setEnabled(false);
        status.setText("Saving to server...");

        ApiClient.calculate(a, op, b, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                status.setText("Saved to PostgreSQL.");
                equalsButton.setEnabled(true);
            }

            @Override
            public void onError(String message) {
                status.setText("Not saved: " + message);
                equalsButton.setEnabled(true);
            }
        });
    }

    private String formatResult(double result) {
        if (result == Math.rint(result)
                && Math.abs(result) < 1e15) {
            return String.format(Locale.US, "%.0f", result);
        }

        return Double.toString(result);
    }

    private void clear() {
        display.setText("0");
        firstNumber = "";
        operator = "";
        startNewNumber = true;
        status.setText("");
    }

    private void loadHistory() {
        String message;

        if (historyList.isEmpty()) {
            message = "No saved calculations yet.";
        } else {
            StringBuilder text = new StringBuilder();
            for (int i = historyList.size() - 1; i >= 0; i--) {
                text.append(historyList.get(i)).append("\n");
            }
            message = text.toString();
        }

        new AlertDialog.Builder(this)
                .setTitle("Calculation History")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .setNegativeButton("Clear", (dialog, which) -> {
                    historyList.clear();
                    prefs.edit().remove("history").apply();
                    status.setText("History cleared.");
                })
                .show();
    }

    private void addToHistory(String entry) {
        historyList.add(entry);
        while (historyList.size() > MAX_HISTORY) {
            historyList.remove(0);
        }
        prefs.edit()
                .putString("history", String.join("\n", historyList))
                .apply();
    }

    private void loadSavedHistory() {
        String saved = prefs.getString("history", "");
        historyList.clear();
        if (!saved.isEmpty()) {
            for (String line : saved.split("\n")) {
                historyList.add(line);
            }
        }
    }

    private String toSymbol(String op) {
        switch (op) {
            case "*": return "×";
            case "/": return "÷";
            default:  return op;
        }
    }
    }
