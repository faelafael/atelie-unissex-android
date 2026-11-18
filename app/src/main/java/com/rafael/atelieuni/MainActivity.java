package com.rafael.atelieuni;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String GENERATOR_URL = "https://perchance.org/atelieuni";
    private static final String PRIVACY_URL = "https://faelafael.github.io/atelie-unissex-android/privacy.html";
    private static final String SUPPORT_URL = "https://github.com/faelafael/atelie-unissex-android/issues/new";
    private static final String PREFS = "atelie_unissex";
    private static final String DOWNLOAD_IDS = "download_ids";
    private static final String SAVED_URIS = "saved_uris";
    private static final int FILE_CHOOSER_REQUEST = 4102;

    private FrameLayout content;
    private WebView webView;
    private ScrollView galleryScroll;
    private TextView statusText;
    private ValueCallback<Uri[]> filePathCallback;
    private DownloadManager downloadManager;
    private boolean galleryVisible = false;
    private OnBackInvokedCallback backCallback;

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(53, 33, 63));
        getWindow().setNavigationBarColor(Color.rgb(36, 24, 43));
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (getPreferences(MODE_PRIVATE).getBoolean("age_confirmed", false)) {
            buildApp();
        } else {
            showAgeGate();
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable rounded(int color, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        if (strokeColor != Color.TRANSPARENT) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(63, 48, 70));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        b.setMinHeight(dp(44));
        b.setBackground(rounded(primary ? Color.rgb(104, 67, 125) : Color.WHITE,
                primary ? Color.TRANSPARENT : Color.rgb(222, 213, 228)));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showAgeGate() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(28), dp(30), dp(28), dp(30));
        page.setBackgroundColor(Color.rgb(53, 33, 63));

        TextView logo = text("ATELIÊ\nUNISSEX", 30, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        page.addView(logo, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = text("Seu espaço criativo", 16, Color.rgb(224, 208, 234), false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(10);
        page.addView(subtitle, sp);

        TextView notice = text(
                "O acesso ao gerador é destinado a pessoas com 18 anos ou mais, conforme os termos do Perchance.\n\n" +
                "O gerador é carregado diretamente do Perchance. O aplicativo não é oficial nem afiliado à plataforma.",
                15, Color.WHITE, false);
        notice.setGravity(Gravity.CENTER);
        notice.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(36);
        np.bottomMargin = dp(28);
        page.addView(notice, np);

        Button enter = button("Tenho 18 anos ou mais — continuar", true, () -> {
            getPreferences(MODE_PRIVATE).edit().putBoolean("age_confirmed", true).apply();
            buildApp();
        });
        page.addView(enter, new LinearLayout.LayoutParams(-1, -2));

        Button leave = button("Sair", false, this::finish);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        page.addView(leave, lp);
        setContentView(page);
    }

    private void buildApp() {
        LinearLayout base = new LinearLayout(this);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setBackgroundColor(Color.rgb(248, 245, 250));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(12), dp(18), dp(10));
        header.setBackgroundColor(Color.rgb(53, 33, 63));
        header.addView(text("Ateliê Unissex", 21, Color.WHITE, true));
        TextView subtitle = text("Gerador criativo", 12, Color.rgb(222, 208, 232), false);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(-1, -2);
        subp.topMargin = dp(2);
        header.addView(subtitle, subp);
        base.addView(header);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(12), dp(8), dp(12), dp(8));
        tabs.setGravity(Gravity.CENTER);
        tabs.setBackgroundColor(Color.rgb(242, 237, 246));
        Button generatorTab = button("Gerador", true, this::showGenerator);
        Button galleryTab = button("Minhas criações", false, this::showGallery);
        LinearLayout.LayoutParams tabp1 = new LinearLayout.LayoutParams(0, -2, 1f);
        tabp1.setMargins(dp(4), 0, dp(4), 0);
        LinearLayout.LayoutParams tabp2 = new LinearLayout.LayoutParams(0, -2, 1f);
        tabp2.setMargins(dp(4), 0, dp(4), 0);
        tabs.addView(generatorTab, tabp1);
        tabs.addView(galleryTab, tabp2);
        base.addView(tabs);

        content = new FrameLayout(this);
        base.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        webView = new WebView(this);
        configureWebView();
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        galleryScroll = new ScrollView(this);
        galleryScroll.setFillViewport(true);
        galleryScroll.setVisibility(View.GONE);
        content.addView(galleryScroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(6), dp(7), dp(6), dp(7));
        actions.setGravity(Gravity.CENTER);
        actions.setBackgroundColor(Color.WHITE);
        Button back = button("Voltar", false, () -> {
            if (webView.canGoBack()) webView.goBack();
        });
        Button reload = button("Atualizar", false, () -> {
            if (!galleryVisible) webView.reload();
        });
        Button share = button("Compartilhar", false, this::shareGenerator);
        Button open = button("Navegador", false, () -> openExternal(GENERATOR_URL));
        actions.addView(back, actionParams());
        actions.addView(reload, actionParams());
        actions.addView(share, actionParams());
        actions.addView(open, actionParams());
        base.addView(actions);

        statusText = text("Conectando ao Perchance…", 11, Color.rgb(103, 89, 111), false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(8), dp(5), dp(8), dp(7));
        statusText.setOnClickListener(v -> showPrivacyInfo());
        base.addView(statusText);

        setContentView(base);
        registerBackCallback();
        if (webView.getUrl() == null) webView.loadUrl(GENERATOR_URL);
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                if ("perchance.org".equals(host) || host.endsWith(".perchance.org")) return false;
                openExternal(uri.toString());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                statusText.setText("Carregando gerador…");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                statusText.setText("Gerador hospedado no Perchance · toque para privacidade");
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) statusText.setText("Falha de conexão. Verifique a internet e atualize.");
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                statusText.setText("A conexão segura não pôde ser validada.");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                              FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*"});
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    startActivityForResult(Intent.createChooser(pick, "Selecionar imagem"), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Não foi possível abrir os arquivos.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                downloadFile(url, userAgent, contentDisposition, mimeType));
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

    private void downloadFile(String url, String userAgent, String contentDisposition, String mimeType) {
        if (url == null) {
            Toast.makeText(this, "Link de download inválido.", Toast.LENGTH_LONG).show();
            return;
        }
        if (url.startsWith("data:")) {
            saveDataUrl(url);
            return;
        }
        if (url.startsWith("blob:")) {
            downloadBlob(url);
            return;
        }
        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, "Este formato de download não é compatível.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
            int dot = filename.lastIndexOf('.');
            String extension = dot > 0 ? filename.substring(dot) : "";
            String base = dot > 0 ? filename.substring(0, dot) : filename;
            filename = base + "_" + System.currentTimeMillis() + extension;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(filename);
            request.setDescription("Salvando em Downloads/AtelieUnissex");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "AtelieUnissex/" + filename);
            request.allowScanningByMediaScanner();
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            if (mimeType != null) request.setMimeType(mimeType);

            long id = downloadManager.enqueue(request);
            rememberDownload(id);
            Toast.makeText(this, "Download iniciado. Veja em Minhas criações.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível iniciar o download: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadBlob(String blobUrl) {
        String quotedUrl = JSONObject.quote(blobUrl);
        String script = "(async()=>{try{const r=await fetch(" + quotedUrl + ");" +
                "if(!r.ok)throw new Error('Não foi possível ler a imagem');" +
                "const b=await r.blob();if(!b.type.startsWith('image/'))throw new Error('O arquivo não é uma imagem');" +
                "return await new Promise((resolve,reject)=>{const f=new FileReader();" +
                "f.onload=()=>resolve(f.result);f.onerror=()=>reject(new Error('Falha ao ler a imagem'));" +
                "f.readAsDataURL(b);});}catch(e){return 'ERROR:'+String(e);}})()";
        webView.evaluateJavascript(script, result -> {
            try {
                if (result == null || "null".equals(result)) throw new IllegalStateException("Resultado vazio");
                String dataUrl = new JSONArray("[" + result + "]").getString(0);
                if (!dataUrl.startsWith("data:image/")) {
                    throw new IllegalStateException(dataUrl.startsWith("ERROR:")
                            ? dataUrl.substring(6) : "Formato de imagem não reconhecido");
                }
                saveDataUrl(dataUrl);
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível salvar essa imagem: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveDataUrl(String dataUrl) {
        Uri outputUri = null;
        try {
            int comma = dataUrl.indexOf(',');
            if (!dataUrl.startsWith("data:") || comma < 0) {
                throw new IllegalArgumentException("Imagem em formato inválido");
            }

            String metadata = dataUrl.substring(5, comma);
            String mime = metadata.split(";", 2)[0];
            if (!mime.startsWith("image/")) throw new IllegalArgumentException("O arquivo não é uma imagem");

            String payload = dataUrl.substring(comma + 1);
            byte[] bytes = metadata.contains(";base64")
                    ? Base64.decode(payload, Base64.DEFAULT)
                    : URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0 || bytes.length > 25 * 1024 * 1024) {
                throw new IllegalArgumentException("Imagem vazia ou maior que 25 MB");
            }

            String extension;
            switch (mime.toLowerCase(Locale.ROOT)) {
                case "image/jpeg":
                case "image/jpg": extension = ".jpg"; break;
                case "image/webp": extension = ".webp"; break;
                case "image/gif": extension = ".gif"; break;
                case "image/svg+xml": extension = ".svg"; break;
                default: extension = ".png"; break;
            }
            String filename = "atelie-" + System.currentTimeMillis() + extension;
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/AtelieUnissex");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            outputUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (outputUri == null) throw new IllegalStateException("Não foi possível criar o arquivo");
            try (OutputStream out = getContentResolver().openOutputStream(outputUri)) {
                if (out == null) throw new IllegalStateException("Não foi possível abrir o arquivo");
                out.write(bytes);
                out.flush();
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(outputUri, published, null, null);
            rememberSavedUri(outputUri);
            Toast.makeText(this, "Imagem salva em Downloads/AtelieUnissex.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            if (outputUri != null) {
                try { getContentResolver().delete(outputUri, null, null); } catch (Exception ignored) {}
            }
            Toast.makeText(this, "Não foi possível salvar a imagem: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void rememberDownload(long id) {
        String old = getSharedPreferences(PREFS, MODE_PRIVATE).getString(DOWNLOAD_IDS, "");
        List<String> ids = new ArrayList<>();
        if (!old.isEmpty()) Collections.addAll(ids, old.split(","));
        ids.add(Long.toString(id));
        if (ids.size() > 100) ids = new ArrayList<>(ids.subList(ids.size() - 100, ids.size()));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(DOWNLOAD_IDS, String.join(",", ids)).apply();
    }

    private void rememberSavedUri(Uri uri) {
        String old = getSharedPreferences(PREFS, MODE_PRIVATE).getString(SAVED_URIS, "");
        List<String> uris = new ArrayList<>();
        if (!old.isEmpty()) Collections.addAll(uris, old.split(","));
        uris.add(uri.toString());
        if (uris.size() > 100) uris = new ArrayList<>(uris.subList(uris.size() - 100, uris.size()));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(SAVED_URIS, String.join(",", uris)).apply();
    }

    private void showGenerator() {
        galleryVisible = false;
        galleryScroll.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        statusText.setText("Gerador hospedado no Perchance · toque para privacidade");
    }

    private void showGallery() {
        galleryVisible = true;
        webView.setVisibility(View.GONE);
        galleryScroll.setVisibility(View.VISIBLE);
        renderGallery();
    }

    private void renderGallery() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(12), dp(12), dp(20));
        page.addView(text("Minhas criações", 20, Color.rgb(53, 33, 63), true));

        TextView hint = text("Imagens salvas pelo aplicativo ficam em Downloads/AtelieUnissex.",
                12, Color.rgb(100, 88, 108), false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(4);
        hp.bottomMargin = dp(12);
        page.addView(hint, hp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        page.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(DOWNLOAD_IDS, "");
        List<String> ids = new ArrayList<>();
        if (!raw.isEmpty()) Collections.addAll(ids, raw.split(","));
        Collections.reverse(ids);

        int count = 0;
        for (String value : ids) {
            try {
                long id = Long.parseLong(value);
                DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
                try (Cursor c = downloadManager.query(query)) {
                    if (c == null || !c.moveToFirst()) continue;
                    int state = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (state != DownloadManager.STATUS_SUCCESSFUL) continue;
                    String mime = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE));
                    if (mime == null || !mime.startsWith("image/")) continue;
                }
                Uri uri = downloadManager.getUriForDownloadedFile(id);
                if (uri != null && addImageTile(grid, uri)) count++;
            } catch (Exception ignored) {
                // Um download removido ou arquivo inválido não impede a galeria de abrir.
            }
        }

        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(SAVED_URIS, "");
        List<String> savedUris = new ArrayList<>();
        if (!saved.isEmpty()) Collections.addAll(savedUris, saved.split(","));
        Collections.reverse(savedUris);
        for (String value : savedUris) {
            try {
                if (addImageTile(grid, Uri.parse(value))) count++;
            } catch (Exception ignored) {
                // Uma imagem apagada pelo usuário é ignorada.
            }
        }

        if (count == 0) {
            TextView empty = text(
                    "Ainda não há imagens salvas pelo aplicativo.\n\nVolte ao gerador e baixe uma criação para ela aparecer aqui.",
                    15, Color.rgb(105, 91, 112), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(45), dp(20), dp(45));
            page.addView(empty);
        }

        galleryScroll.removeAllViews();
        galleryScroll.addView(page);
        statusText.setText(count + (count == 1 ? " imagem salva" : " imagens salvas"));
    }

    private boolean addImageTile(GridLayout grid, Uri uri) {
        try {
            Bitmap bitmap;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return false;
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 2;
                bitmap = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bitmap == null) return false;

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(5), dp(5), dp(5), dp(10));
            tile.setBackground(rounded(Color.WHITE, Color.rgb(229, 221, 233)));
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(image, new LinearLayout.LayoutParams(-1, dp(170)));
            TextView label = text("Imagem salva", 12, Color.rgb(76, 61, 84), true);
            label.setPadding(dp(4), dp(7), dp(4), dp(2));
            tile.addView(label);
            tile.setOnClickListener(v -> showImageActions(uri));

            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = -2;
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            gp.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(tile, gp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showImageActions(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("Imagem salva")
                .setItems(new String[]{"Abrir imagem", "Compartilhar imagem"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        view.setDataAndType(uri, "image/*");
                        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try { startActivity(view); }
                        catch (ActivityNotFoundException e) {
                            Toast.makeText(this, "Nenhum visualizador de imagens disponível.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType("image/*");
                        send.putExtra(Intent.EXTRA_STREAM, uri);
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(send, "Compartilhar criação"));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void shareGenerator() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "Ateliê Unissex — " + GENERATOR_URL);
        startActivity(Intent.createChooser(send, "Compartilhar gerador"));
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum navegador disponível.", Toast.LENGTH_LONG).show();
        }
    }

    private void showPrivacyInfo() {
        new AlertDialog.Builder(this)
                .setTitle("Política de privacidade e sobre")
                .setMessage(
                        "Este é um aplicativo independente, não oficial e sem afiliação com o Perchance. " +
                        "O gerador é carregado diretamente de perchance.org.\n\n" +
                        "O app não possui servidor próprio, conta, publicidade ou analytics. A confirmação de idade, " +
                        "cookies/armazenamento da WebView e a lista de downloads ficam no dispositivo. " +
                        "Arquivos baixados são mantidos em Downloads/AtelieUnissex até serem apagados pelo usuário.\n\n" +
                        "Ao usar o gerador, o Perchance e os serviços de terceiros carregados pela página podem receber " +
                        "IP, dados técnicos do dispositivo/navegador, cookies, prompts, imagens enviadas e resultados, " +
                        "conforme as políticas desses serviços.\n\n" +
                        "Política completa: " + PRIVACY_URL + "\n" +
                        "Suporte e privacidade: " + SUPPORT_URL)
                .setNeutralButton("Política completa", (d, w) -> openExternal(PRIVACY_URL))
                .setPositiveButton("Termos do Perchance", (d, w) -> openExternal("https://perchance.org/terms-of-service"))
                .setNegativeButton("Fechar", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    results = new Uri[n];
                    for (int i = 0; i < n; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    private void registerBackCallback() {
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    private void handleBack() {
        if (galleryVisible) showGenerator();
        else if (webView != null && webView.canGoBack()) webView.goBack();
        else finish();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        handleBack();
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
