package pl.skanerufgvoice;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int SPEECH = 101, FILE_PICK = 102, AUDIO_PERMISSION = 103;
    private WebView web;
    private TextToSpeech tts;
    private ValueCallback<Uri[]> fileCallback;
    private LinearLayout historyBox;
    private EditText plateInput;
    private String lastPlate = "";
    private boolean captchaAnnounced = false;
    private final String UFG_URL = "https://www.ufg.pl/";
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("ufg_history", MODE_PRIVATE);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("pl", "PL"));
        });

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(0xFFF4F6F4);

        TextView heading = new TextView(this);
        heading.setText("SKANER UFG VOICE");
        heading.setTextSize(20);
        heading.setTextColor(0xFFFFFFFF);
        heading.setTypeface(null, 1);
        heading.setPadding(16, 16, 16, 16);
        heading.setBackgroundColor(0xFF176B45);
        main.addView(heading);

        LinearLayout controls = new LinearLayout(this);
        controls.setPadding(8, 6, 8, 6);
        controls.setOrientation(LinearLayout.VERTICAL);
        main.addView(controls);

        LinearLayout row1 = new LinearLayout(this);
        Button photo = button("TRYB ZDJĘCIE");
        Button voice = button("TRYB GŁOSOWY");
        row1.addView(photo, new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(voice, new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        plateInput = new EditText(this);
        plateInput.setSingleLine(true);
        plateInput.setHint("Numer rejestracyjny (opcjonalnie)");
        plateInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        row2.addView(plateInput, new LinearLayout.LayoutParams(0, -2, 1));
        Button insert = button("WSTAW");
        row2.addView(insert);
        controls.addView(row2);

        LinearLayout row3 = new LinearLayout(this);
        Button next = button("NASTĘPNY");
        Button repeat = button("POWTÓRZ");
        Button history = button("HISTORIA");
        row3.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        row3.addView(repeat, new LinearLayout.LayoutParams(0, -2, 1));
        row3.addView(history, new LinearLayout.LayoutParams(0, -2, 1));
        controls.addView(row3);

        TextView note = new TextView(this);
        note.setText("Po wyniku wybierz jego status poniżej strony UFG. CAPTCHA potwierdzasz samodzielnie.");
        note.setTextSize(12);
        note.setPadding(8, 4, 8, 4);
        controls.addView(note);

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                inspectPage();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_PICK); }
                catch (Exception e) { fileCallback = null; Toast.makeText(MainActivity.this, "Nie można otworzyć wyboru zdjęcia", Toast.LENGTH_LONG).show(); return false; }
                return true;
            }
        });
        main.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout results = new LinearLayout(this);
        results.setPadding(6, 4, 6, 8);
        Button yes = button("OC POTWIERDZONE");
        Button uncertain = button("BRAK POTWIERDZENIA");
        Button error = button("BŁĄD");
        results.addView(yes, new LinearLayout.LayoutParams(0, -2, 1));
        results.addView(uncertain, new LinearLayout.LayoutParams(0, -2, 1));
        results.addView(error, new LinearLayout.LayoutParams(0, -2, 1));
        main.addView(results);
        setContentView(main);

        photo.setOnClickListener(v -> { speak("Otwieram stronę UFG. Wybierz zdjęcie tablicy w formularzu."); web.loadUrl(UFG_URL); });
        voice.setOnClickListener(v -> startSpeech());
        insert.setOnClickListener(v -> injectPlate(plateInput.getText().toString()));
        next.setOnClickListener(v -> { plateInput.setText(""); lastPlate=""; speak("Możesz sprawdzić następny samochód."); web.loadUrl(UFG_URL); });
        repeat.setOnClickListener(v -> speak("Podaj numer rejestracyjny albo wybierz tryb zdjęcie."));
        history.setOnClickListener(v -> showHistory());
        yes.setOnClickListener(v -> saveResult("OC POTWIERDZONE"));
        uncertain.setOnClickListener(v -> saveResult("BRAK POTWIERDZENIA WAŻNEGO OC"));
        error.setOnClickListener(v -> saveResult("NIE UDAŁO SIĘ SPRAWDZIĆ"));
        web.loadUrl(UFG_URL);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        return b;
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ufg_voice");
    }

    private void startSpeech() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Powiedz numer rejestracyjny lub komendę");
        try { startActivityForResult(i, SPEECH); }
        catch (Exception e) { speak("Rozpoznawanie mowy nie jest dostępne na tym telefonie."); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == AUDIO_PERMISSION && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) startSpeech();
        else if (requestCode == AUDIO_PERMISSION) speak("Nie przyznano dostępu do mikrofonu.");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) handleSpoken(matches.get(0));
        } else if (requestCode == FILE_PICK && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    private void handleSpoken(String raw) {
        String phrase = raw.trim();
        String lower = phrase.toLowerCase(new Locale("pl", "PL"));
        if (lower.contains("tryb zdjęcie")) { speak("Wybierz zdjęcie tablicy w formularzu UFG."); return; }
        if (lower.contains("tryb głosowy")) { speak("Powiedz numer rejestracyjny."); startSpeech(); return; }
        if (lower.equals("tak")) { injectPlate(lastPlate); return; }
        if (lower.equals("nie")) { speak("Powiedz numer jeszcze raz."); startSpeech(); return; }
        if (lower.contains("następny samochód")) { plateInput.setText(""); lastPlate=""; speak("Gotowy na następny samochód."); return; }
        if (lower.equals("powtórz")) { speak("Ostatni rozpoznany numer: " + lastPlate); return; }
        if (lower.equals("zakończ")) { speak("Kończę pracę aplikacji."); finish(); return; }

        lastPlate = phrase.toUpperCase(new Locale("pl", "PL")).replaceAll("[^A-Z0-9]", "");
        plateInput.setText(lastPlate);
        speak("Rozpoznany numer: " + lastPlate + ". Powiedz tak, aby wstawić, albo nie, aby poprawić.");
    }

    private void injectPlate(String plate) {
        String p = plate.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (p.isEmpty()) { speak("Nie podano numeru rejestracyjnego."); return; }
        lastPlate = p;
        String js = "(function(){var e=document.querySelector('input[type=text],input:not([type]),input'); if(e){e.focus();e.value='" +
                p + "';e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true})); return true;} return false;})();";
        web.evaluateJavascript(js, value -> {
            if ("true".equals(value)) speak("Numer wstawiony do pierwszego pola formularza. Sprawdź poprawność.");
            else speak("Nie znalazłem pola numeru na stronie. Wpisz numer ręcznie.");
        });
    }

    private void inspectPage() {
        web.evaluateJavascript("(document.body ? document.body.innerText : '').toLowerCase()", value -> {
            if (value == null) return;
            if (!captchaAnnounced && (value.contains("captcha") || value.contains("nie jestem robotem") || value.contains("robotem"))) {
                captchaAnnounced = true;
                speak("Potwierdź sprawdzenie");
            }
            if (!value.contains("captcha") && !value.contains("nie jestem robotem")) captchaAnnounced = false;
        });
    }

    private void saveResult(String status) {
        String plate = lastPlate.isEmpty() ? plateInput.getText().toString().trim().toUpperCase(Locale.ROOT) : lastPlate;
        if (plate.isEmpty()) plate = "NIE PODANO";
        try {
            JSONArray arr = new JSONArray(prefs.getString("items", "[]"));
            JSONObject o = new JSONObject();
            o.put("plate", plate);
            o.put("status", status);
            o.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            o.put("method", "Aplikacja");
            arr.put(0, o);
            prefs.edit().putString("items", arr.toString()).apply();
        } catch (Exception ignored) {}
        if (status.equals("OC POTWIERDZONE")) speak("Pojazd posiada potwierdzone ważne OC.");
        else if (status.startsWith("BRAK")) speak("Brak potwierdzenia ważnego OC.");
        else speak("Nie udało się sprawdzić pojazdu.");
        Toast.makeText(this, "Zapisano wynik: " + status, Toast.LENGTH_SHORT).show();
    }

    private void showHistory() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("items", "[]"));
            StringBuilder s = new StringBuilder("HISTORIA SPRAWDZEŃ\n\n");
            for (int i=0; i<arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                s.append(o.optString("date")).append("  ").append(o.optString("plate"))
                 .append("\n").append(o.optString("status")).append("\n\n");
            }
            if (arr.length()==0) s.append("Brak zapisanych sprawdzeń.");
            new android.app.AlertDialog.Builder(this).setTitle("Historia").setMessage(s.toString())
                .setPositiveButton("ZAMKNIJ", null)
                .setNeutralButton("USUŃ HISTORIĘ", (d,w) -> { prefs.edit().remove("items").apply(); Toast.makeText(this,"Historia usunięta",Toast.LENGTH_SHORT).show(); })
                .show();
        } catch (Exception e) { Toast.makeText(this,"Nie można odczytać historii",Toast.LENGTH_SHORT).show(); }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
