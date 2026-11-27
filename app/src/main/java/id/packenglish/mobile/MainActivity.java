package id.packenglish.mobile;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK_FILES = 1001;
    private static final int REQUEST_CREATE_OUTPUT = 1002;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<FileRecord> records = new ArrayList<>();

    private LinearLayout resultsContainer;
    private TextView statusText;
    private FileRecord pendingExportRecord;
    private NcsdInspector.Partition pendingExportPartition;

    private static final int COLOR_BACKGROUND = Color.rgb(12, 18, 31);
    private static final int COLOR_CARD = Color.rgb(24, 34, 53);
    private static final int COLOR_ACCENT = Color.rgb(90, 170, 255);
    private static final int COLOR_TEXT = Color.rgb(241, 245, 250);
    private static final int COLOR_MUTED = Color.rgb(174, 187, 205);

    private static class FileRecord {
        Uri uri;
        String name;
        long size;
        NcsdInspector.Result result;
        String error;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("PackEnglish Mobile");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("3DS file inspector • Android edition");
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setTextSize(14);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(5);
        subtitleParams.bottomMargin = dp(20);
        root.addView(subtitle, subtitleParams);

        Button chooseButton = new Button(this);
        chooseButton.setText("CHOOSE 3DS / CIA / CXI FILES");
        chooseButton.setTextColor(Color.WHITE);
        chooseButton.setAllCaps(false);
        chooseButton.setTextSize(15);
        chooseButton.setBackground(roundRect(COLOR_ACCENT, 14));
        chooseButton.setOnClickListener(v -> openFilePicker());
        root.addView(chooseButton, new LinearLayout.LayoutParams(-1, dp(54)));

        LinearLayout help = new LinearLayout(this);
        help.setOrientation(LinearLayout.VERTICAL);
        help.setPadding(dp(15), dp(13), dp(15), dp(13));
        help.setBackground(roundRect(COLOR_CARD, 14));
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.topMargin = dp(14);
        root.addView(help, helpParams);

        TextView helpTitle = new TextView(this);
        helpTitle.setText("How to use");
        helpTitle.setTextColor(COLOR_TEXT);
        helpTitle.setTextSize(15);
        helpTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        help.addView(helpTitle);

        TextView helpBody = new TextView(this);
        helpBody.setText("1. Tap the button above and select one or more .3ds, .cia, or .cxi files.\n"
                + "2. Review the detected file type and inspection results below.\n"
                + "3. For a 3DS / NCSD file, tap an Export partition button and choose where to save the .ncch file.\n"
                + "4. Exported partition data is copied unchanged and may still be encrypted.\n\n"
                + "Note: This version inspects CIA and CXI files but only exports valid NCCH partitions found in 3DS / NCSD files.");
        helpBody.setTextColor(COLOR_MUTED);
        helpBody.setTextSize(12);
        helpBody.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams helpBodyParams = new LinearLayout.LayoutParams(-1, -2);
        helpBodyParams.topMargin = dp(7);
        help.addView(helpBody, helpBodyParams);

        statusText = new TextView(this);
        statusText.setText("No files selected yet.");
        statusText.setTextColor(COLOR_MUTED);
        statusText.setTextSize(13);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(14);
        statusParams.bottomMargin = dp(12);
        root.addView(statusText, statusParams);

        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(15), dp(14), dp(15), dp(14));
        note.setBackground(roundRect(COLOR_CARD, 14));
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(22);
        root.addView(note, noteParams);

        TextView noteTitle = new TextView(this);
        noteTitle.setText("Limitations");
        noteTitle.setTextColor(COLOR_TEXT);
        noteTitle.setTextSize(15);
        noteTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        note.addView(noteTitle);

        TextView noteBody = new TextView(this);
        noteBody.setText("This app performs read-only NCSD/NCCH/CIA header inspection and can copy raw NCCH partitions from 3DS files. Exported data is copied unchanged and may still be encrypted. This version does not decrypt content, retrieve or use keys, extract ROMFS/banner data, or rebuild files. Only inspect files you own or homebrew files you are authorized to use.");
        noteBody.setTextColor(COLOR_MUTED);
        noteBody.setTextSize(12);
        noteBody.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.topMargin = dp(7);
        note.addView(noteBody, bodyParams);

        TextView footer = new TextView(this);
        footer.setText("Offline • Files are never uploaded to the internet");
        footer.setTextColor(COLOR_MUTED);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(18);
        root.addView(footer, footerParams);

        setContentView(scroll);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_FILES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_PICK_FILES) {
            ArrayList<Uri> selected = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) selected.add(uri);
                }
            } else if (data.getData() != null) {
                selected.add(data.getData());
            }
            if (selected.isEmpty()) return;

            for (Uri uri : selected) {
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Some document providers grant temporary access only; the current scan can still proceed.
                }
            }
            scanFiles(selected);
        } else if (requestCode == REQUEST_CREATE_OUTPUT && pendingExportRecord != null
                && pendingExportPartition != null && data.getData() != null) {
            Uri destination = data.getData();
            FileRecord record = pendingExportRecord;
            NcsdInspector.Partition partition = pendingExportPartition;
            pendingExportRecord = null;
            pendingExportPartition = null;
            exportPartition(record, partition, destination);
        }
    }

    private void scanFiles(List<Uri> selected) {
        records.clear();
        resultsContainer.removeAllViews();
        statusText.setText("Scanning " + selected.size() + (selected.size() == 1 ? " file..." : " files..."));
        executor.execute(() -> {
            ArrayList<FileRecord> scanned = new ArrayList<>();
            for (Uri uri : selected) {
                FileRecord record = new FileRecord();
                record.uri = uri;
                try {
                    record.name = queryName(uri);
                    record.size = querySize(uri);
                    try (InputStream input = getContentResolver().openInputStream(uri)) {
                        if (input == null) throw new IOException("Unable to open file.");
                        record.result = NcsdInspector.inspect(input, record.size, record.name);
                    }
                } catch (Exception e) {
                    record.error = e.getMessage() == null ? "Failed to read file." : e.getMessage();
                }
                scanned.add(record);
            }
            mainHandler.post(() -> {
                records.clear();
                records.addAll(scanned);
                showResults();
            });
        });
    }

    private void showResults() {
        resultsContainer.removeAllViews();
        int recognized = 0;
        for (FileRecord record : records) {
            if (record.result != null && !"Unknown".equals(record.result.kind)) recognized++;
            resultsContainer.addView(makeRecordCard(record));
        }
        statusText.setText("Scan complete: " + records.size() + (records.size() == 1 ? " file" : " files")
                + " scanned, " + recognized + (recognized == 1 ? " format" : " formats") + " recognized.");
    }

    private View makeRecordCard(FileRecord record) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(roundRect(COLOR_CARD, 14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        TextView name = new TextView(this);
        name.setText(record.name == null ? "Unnamed file" : record.name);
        name.setTextColor(COLOR_TEXT);
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(name);

        TextView meta = new TextView(this);
        meta.setText(record.size >= 0 ? NcsdInspector.humanBytes(record.size) : "Size unavailable");
        meta.setTextColor(COLOR_MUTED);
        meta.setTextSize(12);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(4);
        card.addView(meta, metaParams);

        TextView detail = new TextView(this);
        detail.setTextColor(COLOR_TEXT);
        detail.setTextSize(12);
        detail.setLineSpacing(dp(3), 1.0f);
        String detailText;
        if (record.error != null) {
            detailText = "Could not inspect: " + record.error;
        } else if (record.result != null) {
            detailText = record.result.kind + "\n" + record.result.description;
        } else {
            detailText = "No inspection results.";
        }
        detail.setText(detailText);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(10);
        card.addView(detail, detailParams);

        if (record.result != null && !record.result.partitions.isEmpty()) {
            TextView partitionTitle = new TextView(this);
            partitionTitle.setText("Detected NCCH partitions");
            partitionTitle.setTextColor(COLOR_TEXT);
            partitionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            partitionTitle.setTextSize(12);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
            titleParams.topMargin = dp(12);
            card.addView(partitionTitle, titleParams);

            for (NcsdInspector.Partition partition : record.result.partitions) {
                Button exportButton = new Button(this);
                exportButton.setAllCaps(false);
                exportButton.setTextColor(Color.WHITE);
                exportButton.setTextSize(12);
                exportButton.setText("Export partition " + partition.index + " • " + NcsdInspector.humanBytes(partition.length));
                exportButton.setBackground(roundRect(Color.rgb(49, 78, 112), 10));
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(46));
                buttonParams.topMargin = dp(7);
                card.addView(exportButton, buttonParams);
                exportButton.setOnClickListener(v -> createExportFile(record, partition));
            }
        }
        return card;
    }

    private void createExportFile(FileRecord record, NcsdInspector.Partition partition) {
        pendingExportRecord = record;
        pendingExportPartition = partition;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, safeStem(record.name) + "_partition_" + partition.index + ".ncch");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CREATE_OUTPUT);
    }

    private void exportPartition(FileRecord record, NcsdInspector.Partition partition, Uri destination) {
        statusText.setText("Exporting partition " + partition.index + "...");
        executor.execute(() -> {
            String error = null;
            try (InputStream input = getContentResolver().openInputStream(record.uri);
                 OutputStream output = getContentResolver().openOutputStream(destination, "w")) {
                if (input == null || output == null) throw new IOException("Unable to open the source or destination.");
                NcsdInspector.copyRange(input, output, partition.offset, partition.length);
                output.flush();
            } catch (Exception e) {
                error = e.getMessage() == null ? "Export failed." : e.getMessage();
            }
            final String resultMessage = error == null
                    ? "Partition exported unchanged."
                    : "Export failed: " + error;
            mainHandler.post(() -> {
                statusText.setText(resultMessage);
                Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show();
            });
        });
    }

    private String queryName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = cursor.getString(index);
            }
        } catch (Exception ignored) { }
        if (name == null || name.trim().isEmpty()) name = uri.getLastPathSegment();
        return name == null ? "unknown.bin" : name;
    }

    private long querySize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index >= 0 && !cursor.isNull(index)) return cursor.getLong(index);
            }
        } catch (Exception ignored) { }
        return -1;
    }

    private String safeStem(String filename) {
        if (filename == null || filename.trim().isEmpty()) return "3ds_file";
        int dot = filename.lastIndexOf('.');
        String stem = dot > 0 ? filename.substring(0, dot) : filename;
        return stem.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
