package com.mishkat.livethemes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

public class MainActivity extends Activity {

    static final String[] WALLS = {
            "01-mishkat-home.jpg", "02-mishkat-lock.jpg", "03-mishkat-amoled.jpg", "04-mishkat-light.jpg",
            "05-mishkat-star.jpg", "06-forest-mist.jpg", "07-forest-autumn-rays.jpg", "08-forest-green.jpg",
            "09-forest-sunbeams.jpg", "10-kuang-si-falls.jpg", "11-tropical-stream.jpg",
            "12-amazon-lake-dawn.jpg", "13-lake-reflection.jpg", "14-user-1.jpg", "15-user-2.jpg", "16-user-3.jpg", "17-user-4.jpg", "18-user-5.jpg", "19-user-6.jpg", "20-user-7.jpg", "21-user-8.jpg", "22-user-9.jpg", "23-user-10.jpg", "24-user-11.jpg", "25-user-12.jpg", "26-user-13.jpg", "27-user-14.jpg", "28-user-15.jpg", "29-user-16.jpg", "30-user-17.jpg", "31-user-18.jpg", "32-user-19.jpg"};
    static final String[] WALL_NAMES = {
            "مشكاة · الرئيسية", "مشكاة · القفل", "مشكاة · أسود", "مشكاة · فاتح", "مشكاة · النجمة",
            "غابة الضباب", "أشعة الخريف", "الغابة الخضراء", "أشعة الصباح",
            "شلالات كوانغ سي", "جدول استوائي", "فجر الأمازون", "بحيرة الانعكاس", "غابة الثلوج", "خليج الجزر", "البحر الفيروزي", "شاطئ النخيل", "الساحل الصخري", "سماء خيالية", "شمس الغروب", "أشرعة الغروب", "بحيرة الغابة", "ضوء الصباح", "أشجار الماء", "حديقة النهر", "الغابة الاستوائية", "الكعبة المشرفة", "الحرم ليلاً", "الطواف", "المسجد الحرام", "صفوف المصلين", "الحرم عند الغروب"};

    static final String[] ICONS = {"phone", "messages", "contacts", "camera", "gallery", "settings", "clock",
            "calendar", "calculator", "browser", "files", "music", "mail", "maps", "store", "notes",
            "weather", "quran", "azkar", "qibla", "flashlight", "recorder"};
    static final String[] ICON_NAMES = {"الهاتف", "الرسائل", "جهات الاتصال", "الكاميرا", "المعرض",
            "الإعدادات", "الساعة", "التقويم", "الحاسبة", "المتصفح", "الملفات", "الصوتيات", "البريد",
            "الخرائط", "المتجر", "الملاحظات", "الطقس", "المصحف", "الأذكار", "القبلة", "الكشاف", "المسجل"};

    // ---- navigation: one place that defines the app's menus
    static final int TAB_DESIGN = 0, TAB_WALLS = 1, TAB_ICONS = 2, TAB_ABOUT = 3;
    static final String[] NAV_ICONS = {"◷", "▦", "✦", "ⓘ"};
    static final String[] NAV_NAMES = {"الثيم الحي", "الخلفيات", "الأيقونات", "حول"};
    static final String[] DESIGN_SECTIONS = {"المظهر", "التخطيط", "العناصر", "الخلفية", "ثيماتي"};
    static final String[] WALL_CATEGORIES = {"الكل", "الطبيعة", "إسلامية"};

    // ---- palette
    static final int MUTED = 0xFFB9AD8C;
    static final int LINE = 0x33D9B15C;
    static final int CARD = ClockRenderer.NAVY;

    private static final int REQ_STORAGE = 7;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };

    private Typeface font;
    private FrameLayout content;
    private final LinearLayout[] navItems = new LinearLayout[NAV_NAMES.length];
    private SharedPreferences prefs;
    private float dp;
    private int currentTab = TAB_DESIGN;

    // design tab state
    private ImageView preview;
    private LinearLayout panel;
    private boolean lockPreview = true;
    private int designSection = 0;
    private int previewGeneration = 0;
    private LinearLayout bgRow;
    private final java.util.List<Object[]> choiceRows = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        font = ClockRenderer.font(this);
        prefs = getSharedPreferences(ClockRenderer.PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(ClockRenderer.DEEP);

        // ---- app bar
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(px(16), px(12), px(16), px(10));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        head.addView(logo, new LinearLayout.LayoutParams(px(40), px(40)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(px(12), 0, px(12), 0);
        titles.addView(label("مشكاة – ثيمات الهاتف", 21, ClockRenderer.GOLD));
        titles.addView(label("مشكاة · برمجيات نافعة للهاتف والحاسب", 12.5f, MUTED));
        head.addView(titles);
        root.addView(head);
        root.addView(divider(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1))));

        // ---- page content
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // ---- bottom navigation
        root.addView(divider(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1))));
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(0xFF0B1A40);
        nav.setPadding(px(6), px(6), px(6), px(8));
        for (int i = 0; i < NAV_NAMES.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(0, px(2), 0, px(2));
            TextView icon = new TextView(this);
            icon.setText(NAV_ICONS[i]);
            icon.setTypeface(Typeface.DEFAULT);
            icon.setTextSize(19);
            icon.setGravity(Gravity.CENTER);
            icon.setPadding(px(18), px(2), px(18), px(2));
            item.addView(icon);
            TextView name = label(NAV_NAMES[i], 13.5f, ClockRenderer.CREAM);
            name.setGravity(Gravity.CENTER);
            item.addView(name);
            item.setBackground(ripple(null));
            item.setOnClickListener(v -> showTab(idx));
            nav.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            navItems[i] = item;
        }
        root.addView(nav);

        setContentView(root);
        showTab(TAB_DESIGN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pool.shutdownNow();
    }

    @Override
    public void onBackPressed() {
        if (currentTab != TAB_DESIGN) showTab(TAB_DESIGN);
        else super.onBackPressed();
    }

    // ------------------------------------------------------------------ navigation

    private void showTab(int i) {
        currentTab = i;
        for (int k = 0; k < navItems.length; k++) {
            boolean on = k == i;
            TextView icon = (TextView) navItems[k].getChildAt(0);
            TextView name = (TextView) navItems[k].getChildAt(1);
            icon.setBackground(on ? round(0x33D9B15C, 16, 0) : null);
            icon.setTextColor(on ? ClockRenderer.GOLD : MUTED);
            name.setTextColor(on ? ClockRenderer.GOLD : MUTED);
        }
        content.removeAllViews();
        preview = null;
        panel = null;
        switch (i) {
            case TAB_DESIGN: content.addView(designTab()); break;
            case TAB_WALLS: content.addView(wallpapersTab()); break;
            case TAB_ICONS: content.addView(iconsTab()); break;
            default: content.addView(aboutTab()); break;
        }
    }

    // ------------------------------------------------------------------ wallpapers

    private int category = 0;

    private static boolean inCategory(int cat, int i) {
        return cat == 0 || (cat == 1 && i >= 5 && i < 26) || (cat == 2 && (i < 5 || i >= 26));
    }

    private View wallpapersTab() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(px(16), 0, px(16), 0);
        pageHeader(outer, "مكتبة الخلفيات", WALLS.length + " خلفية · اضغط على أي خلفية لمعاينتها وتعيينها");

        String[] cats = new String[WALL_CATEGORIES.length];
        for (int k = 0; k < cats.length; k++) {
            int n = 0;
            for (int i = 0; i < WALLS.length; i++) if (inCategory(k, i)) n++;
            cats[k] = WALL_CATEGORIES[k] + "  " + n;
        }
        outer.addView(segmented(cats, category, 15.5f, n -> { category = n; showTab(TAB_WALLS); }), matchWrap(0));

        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (int i = 0; i < WALLS.length; i++) if (inCategory(category, i)) ids.add(i);
        GridView g = new GridView(this);
        g.setNumColumns(2);
        g.setPadding(0, px(12), 0, px(16));
        g.setClipToPadding(false);
        g.setVerticalSpacing(px(12));
        g.setHorizontalSpacing(px(10));
        g.setSelector(new android.graphics.drawable.ColorDrawable(0));
        g.setAdapter(new BaseAdapter() {
            public int getCount() { return ids.size(); }
            public Object getItem(int p) { return WALLS[ids.get(p)]; }
            public long getItemId(int p) { return p; }
            public View getView(int p, View v, ViewGroup parent) {
                int w = ids.get(p);
                FrameLayout cell = new FrameLayout(MainActivity.this);
                cell.setBackground(round(ClockRenderer.BLUE, 14, 0));
                cell.setClipToOutline(true);
                AspectImage img = new AspectImage(MainActivity.this, 2400f / 1080f);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                loadThumb("thumbs/" + WALLS[w], img);
                cell.addView(img);
                TextView t = label(WALL_NAMES[w], 13.5f, 0xFFFFFFFF);
                t.setGravity(Gravity.CENTER);
                t.setSingleLine(true);
                t.setPadding(px(6), px(22), px(6), px(8));
                t.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                        new int[]{0xD9000000, 0x00000000}));
                cell.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
                return cell;
            }
        });
        g.setOnItemClickListener((parent, view, pos, id) -> previewWall(ids.get(pos)));
        outer.addView(g, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return outer;
    }

    private void previewWall(int pos) {
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageBitmap(ClockRenderer.loadBg(this, WALLS[pos], 540, 1000));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(img, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(340)));
        new AlertDialog.Builder(this).setTitle(WALL_NAMES[pos]).setView(box)
                .setPositiveButton("استخدامها في الثيم الحي", (d, w) -> {
                    prefs.edit().putString(ClockRenderer.KEY_BG, WALLS[pos]).apply();
                    designSection = 3;
                    showTab(TAB_DESIGN);
                })
                .setNeutralButton("خلفية ثابتة", (d, w) -> chooseTarget(pos))
                .setNegativeButton("إغلاق", null).show();
    }

    private void chooseTarget(int pos) {
        String[] opts = {"الشاشة الرئيسية", "شاشة القفل", "الشاشتان معًا"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("تعيين «" + WALL_NAMES[pos] + "» على")
                .setItems(opts, (d, which) -> {
                    int flags = which == 0 ? WallpaperManager.FLAG_SYSTEM
                            : which == 1 ? WallpaperManager.FLAG_LOCK
                            : WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                    applyWallpaper(WALLS[pos], flags);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void applyWallpaper(String file, int flags) {
        toast("جارٍ تعيين الخلفية…");
        pool.execute(() -> {
            try (InputStream in = getAssets().open("wallpapers/" + file)) {
                WallpaperManager.getInstance(this).setStream(in, null, true, flags);
                runOnUiThread(() -> toast("تم تعيين الخلفية ✓"));
            } catch (Exception e) {
                runOnUiThread(() -> toast("تعذّر تعيين الخلفية"));
            }
        });
    }

    // ------------------------------------------------------------------ live theme designer

    private View designTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(16), 0, px(16), px(28));
        sv.addView(col);

        pageHeader(col, "صمّم ثيمك الحي", "اختر نوع الثيم وخصّصه، ثم فعّله على شاشة القفل");

        // 1) theme type
        col.addView(segmented(new String[]{"◷  ساعة بعقارب", "☾  مواقيت الصلاة"},
                prefs.getInt("theme_mode", 0), 16, i -> {
                    prefs.edit().putInt("theme_mode", i).apply();
                    rebuildPanel();
                    refreshClockUi();
                }), matchWrap(0));

        // 2) live preview
        preview = new ImageView(this);
        preview.setBackground(round(0xFF05070C, 22, 0));
        preview.setClipToOutline(true);
        preview.setElevation(px(6));
        preview.setScaleType(ImageView.ScaleType.FIT_XY);
        int pw = Math.round(getResources().getDisplayMetrics().widthPixels * 0.56f);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(pw, Math.round(pw * 2400f / 1080f));
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        plp.setMargins(0, px(16), 0, px(8));
        col.addView(preview, plp);

        TextView sys = label("", 14, MUTED);
        sys.setGravity(Gravity.CENTER);
        sys.setPadding(px(14), px(6), px(14), px(6));
        sys.setBackground(ripple(round(0x00000000, 16, LINE, 1)));
        Runnable sysText = () -> sys.setText((lockPreview ? "◉" : "○") + "  إظهار ساعة النظام في المعاينة");
        sysText.run();
        sys.setOnClickListener(v -> { lockPreview = !lockPreview; sysText.run(); refreshClockUi(); });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(sys, slp);

        // 3) actions
        col.addView(primaryButton("تفعيل على شاشة القفل", v -> activateLive()), matchWrap(14));
        TextView hint = label("بعد التفعيل تظهر أي تعديلات هنا على شاشة القفل مباشرة.", 13, MUTED);
        hint.setGravity(Gravity.CENTER);
        col.addView(hint, matchWrap(6));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(outlineButton("▦  مكتبة الخلفيات", v -> showTab(TAB_WALLS)), weighted(0, 4));
        actions.addView(outlineButton("ⓘ  طريقة التفعيل", v -> showActivationHelp()), weighted(4, 0));
        col.addView(actions, matchWrap(10));

        // 4) customisation sections
        TextView h = label("خيارات التخصيص", 18, ClockRenderer.GOLD);
        col.addView(h, matchWrap(20));
        col.addView(segmented(DESIGN_SECTIONS, designSection, 14.5f, k -> { designSection = k; rebuildPanel(); refreshClockUi(); }), matchWrap(8));

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        col.addView(panel, matchWrap(0));
        rebuildPanel();
        refreshClockUi();
        return sv;
    }

    /** Rebuilds only the options of the selected section, keeping the preview and scroll position. */
    private void rebuildPanel() {
        if (panel == null) return;
        panel.removeAllViews();
        choiceRows.clear();
        bgRow = new LinearLayout(this);
        boolean prayer = prefs.getInt("theme_mode", 0) == 1;

        switch (designSection) {
            case 0: // appearance
                if (prayer) {
                    LinearLayout c = card(panel, "ألوان اللوحة");
                    choiceRow(c, "لون اللوحة", "prayer_color", new String[]{"كحلي", "أسود", "زمردي", "بنفسجي", "بني", "عاجي"}, 0);
                    choiceRow(c, "لون النصوص", "prayer_text", new String[]{"تلقائي", "ذهبي", "أبيض", "أسود"}, 0);
                    range(c, "عتامة اللوحة", "prayer_opacity", 0, 100, 50, true);
                } else {
                    LinearLayout c = card(panel, "هيئة الساعة");
                    choiceRow(c, "الشكل", ClockRenderer.KEY_SHAPE, ClockRenderer.SHAPE_NAMES, 1);
                    choiceRow(c, "الألوان", ClockRenderer.KEY_PALETTE, ClockRenderer.PALETTE_NAMES, 0);
                    LinearLayout d = card(panel, "تفاصيل الميناء");
                    choiceRow(d, "علامات الساعات", ClockRenderer.KEY_MARKS, ClockRenderer.MARK_NAMES, 1);
                    choiceRow(d, "شكل العقارب", ClockRenderer.KEY_HANDS, ClockRenderer.HAND_NAMES, 0);
                    LinearLayout e = card(panel, "مؤشر البطارية");
                    choiceRow(e, "طريقة العرض", ClockRenderer.KEY_BATTERY, ClockRenderer.BATTERY_NAMES, 0);
                }
                break;

            case 1: { // layout
                LinearLayout c = card(panel, "الموقع والحجم");
                c.addView(muted("اترك مساحة أعلى الشاشة لساعة النظام."));
                range(c, "الموقع الرأسي", prayer ? "prayer_position" : "clock_position",
                        prayer ? 45 : 35, prayer ? 80 : 65, prayer ? 58 : 45, false);
                range(c, "الحجم", prayer ? "prayer_scale" : "clock_scale", 60, 100, 100, false);
                c.addView(outlineButton("↺  استعادة التخطيط الافتراضي", v -> {
                    prefs.edit().remove("prayer_position").remove("prayer_scale")
                            .remove("clock_position").remove("clock_scale").apply();
                    rebuildPanel();
                    refreshClockUi();
                }), matchWrap(12));
                break;
            }

            case 2: // elements
                if (prayer) {
                    LinearLayout c = card(panel, "الموقع وطريقة الحساب");
                    choiceRow(c, "المدينة", "prayer_city", PrayerRenderer.CITIES, 0);
                    c.addView(outlineButton(prefs.getBoolean("prayer_gps", false)
                            ? "📍  تحديث موقعي (مفعّل)" : "📍  تحديد موقعي تلقائيًا", v -> locate()), matchWrap(10));
                    choiceRow(c, "طريقة الحساب", "prayer_method", PrayerRenderer.METHODS, 0);
                    toggle(c, "رمضان: تأخير العشاء لأم القرى إلى 120 دقيقة", "prayer_ramadan", false);
                    LinearLayout d = card(panel, "ما يظهر على الشاشة");
                    toggle(d, "مؤشر البطارية", "prayer_battery", true);
                    toggle(d, "التاريخ الميلادي", "prayer_gregorian", true);
                    toggle(d, "التاريخ الهجري", "prayer_hijri", true);
                    toggle(d, "الآية الكريمة", ClockRenderer.KEY_VERSE, true);
                } else {
                    LinearLayout d = card(panel, "ما يظهر على الشاشة");
                    toggle(d, "علامات الدقائق", ClockRenderer.KEY_TICKS, true);
                    toggle(d, "عقرب الثواني", ClockRenderer.KEY_SECONDS, true);
                    toggle(d, "نافذة التاريخ داخل الساعة", ClockRenderer.KEY_DATEWIN, false);
                    toggle(d, "الآية الكريمة", ClockRenderer.KEY_VERSE, true);
                    d.addView(muted("إخفاء عقرب الثواني يقلّل استهلاك البطارية."));
                }
                break;

            case 3: { // background
                LinearLayout c = card(panel, "خلفية الثيم");
                c.addView(muted("اختر خلفية الساعة، أو تصفّح المكتبة كاملة من «الخلفيات»."));
                HorizontalScrollView hs = new HorizontalScrollView(this);
                hs.setHorizontalScrollBarEnabled(false);
                hs.addView(bgRow);
                addBgChoice("", "تلقائي");
                addBgChoice(ClockRenderer.BG_BLACK, "أسود");
                for (int i = 0; i < WALLS.length; i++) {
                    if (WALLS[i].startsWith("02-") || WALLS[i].startsWith("03-")) continue;
                    addBgChoice(WALLS[i], WALL_NAMES[i]);
                }
                c.addView(hs, matchWrap(8));
                break;
            }

            default:
                savedThemes(panel);
                break;
        }
    }

    private void showActivationHelp() {
        new AlertDialog.Builder(this)
                .setTitle("طريقة التفعيل")
                .setMessage("١. اضغط «تفعيل على شاشة القفل».\n"
                        + "٢. ستفتح شاشة معاينة النظام: اضغط «تعيين خلفية» واختر «شاشة القفل» إن ظهر لك الاختيار.\n"
                        + "٣. أي تغيير تجريه هنا بعد التفعيل يظهر على شاشة القفل مباشرة دون إعادة التفعيل.\n\n"
                        + "ساعة الهاتف الأصلية تظل ظاهرة أعلى الشاشة، ويمكنك تصغيرها من إعدادات شاشة القفل.\n"
                        + "إخفاء عقرب الثواني يقلّل استهلاك البطارية.")
                .setPositiveButton("فهمت", null)
                .show();
    }

    private void range(LinearLayout parent, String title, String key, int min, int max, int def, boolean live) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label(title, 15, ClockRenderer.CREAM), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView val = label(prefs.getInt(key, def) + "%", 14, ClockRenderer.GOLD);
        val.setPadding(px(10), px(2), px(10), px(2));
        val.setBackground(round(0x26D9B15C, 10, 0));
        row.addView(val);
        parent.addView(row, matchWrap(12));

        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(prefs.getInt(key, def) - min);
        bar.setProgressTintList(ColorStateList.valueOf(ClockRenderer.GOLD));
        bar.setThumbTintList(ColorStateList.valueOf(ClockRenderer.GOLD));
        parent.addView(bar, matchWrap(4));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar x, int n, boolean user) {
                val.setText((n + min) + "%");
                if (live && user) { prefs.edit().putInt(key, n + min).apply(); refreshClockUi(); }
            }
            public void onStartTrackingTouch(SeekBar x) { }
            public void onStopTrackingTouch(SeekBar x) {
                prefs.edit().putInt(key, x.getProgress() + min).apply();
                refreshClockUi();
            }
        });
    }

    private void savedThemes(LinearLayout parent) {
        SharedPreferences store = getSharedPreferences("saved_themes", MODE_PRIVATE);
        LinearLayout c = card(parent, "ثيماتي المحفوظة");
        c.addView(muted("احفظ حتى ثلاثة تصاميم، واستعد أيًّا منها بلمسة واحدة."));
        for (int i = 1; i <= 3; i++) {
            final String slot = "theme" + i;
            final int n = i;
            boolean has = store.contains(slot);
            if (i > 1) c.addView(divider(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1))));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, px(10), 0, px(10));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(label("الثيم " + i, 16, ClockRenderer.CREAM));
            info.addView(label(has ? "● محفوظ" : "○ فارغ", 13, has ? ClockRenderer.GOLD : MUTED));
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView save = smallButton("حفظ", false);
            save.setOnClickListener(v -> {
                if (!store.contains(slot)) { saveTheme(store, slot); return; }
                new AlertDialog.Builder(this).setTitle("استبدال الثيم " + n + "؟")
                        .setMessage("سيُحفظ التصميم الحالي مكان التصميم المحفوظ في هذه الخانة.")
                        .setPositiveButton("استبدال", (d, w) -> saveTheme(store, slot))
                        .setNegativeButton("إلغاء", null).show();
            });
            row.addView(save);
            if (has) {
                TextView load = smallButton("استعادة", true);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.setMarginStart(px(8));
                load.setOnClickListener(v -> restoreTheme(store, slot));
                row.addView(load, llp);
            }
            c.addView(row);
        }
    }

    private void saveTheme(SharedPreferences store, String slot) {
        org.json.JSONObject obj = new org.json.JSONObject(prefs.getAll());
        store.edit().putString(slot, obj.toString()).apply();
        toast("تم حفظ التصميم ✓");
        rebuildPanel();
    }

    private void restoreTheme(SharedPreferences store, String slot) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(store.getString(slot, "{}"));
            SharedPreferences.Editor e = prefs.edit().clear();
            java.util.Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object val = obj.get(k);
                if (val instanceof Boolean) e.putBoolean(k, (Boolean) val);
                else if (val instanceof Integer) e.putInt(k, (Integer) val);
                else if (val instanceof Number) e.putFloat(k, ((Number) val).floatValue());
                else if (val instanceof String) e.putString(k, (String) val);
            }
            e.apply();
            toast("تمت استعادة التصميم ✓");
            showTab(TAB_DESIGN);
        } catch (Exception ex) {
            toast("تعذّر استعادة الثيم");
        }
    }

    /** A titled, horizontally scrolling row of choice chips bound to an int preference. */
    private void choiceRow(LinearLayout parent, String title, String key, String[] names, int def) {
        parent.addView(label(title, 15, ClockRenderer.CREAM), matchWrap(12));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < names.length; i++) {
            final int val = i;
            TextView t = label(names[i], 14.5f, ClockRenderer.CREAM);
            t.setGravity(Gravity.CENTER);
            t.setPadding(px(14), px(7), px(14), px(7));
            t.setOnClickListener(v -> {
                SharedPreferences.Editor edit = prefs.edit().putInt(key, val);
                if (key.equals("prayer_city")) edit.putBoolean("prayer_gps", false);
                edit.apply();
                if (key.equals("prayer_city")) rebuildPanel();
                refreshClockUi();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(px(8));
            row.addView(t, lp);
        }
        hs.addView(row);
        parent.addView(hs, matchWrap(6));
        choiceRows.add(new Object[]{key, row, def});
    }

    private void addBgChoice(String file, String name) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setTag(file);
        cell.setPadding(px(3), px(3), px(3), px(3));
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setClipToOutline(true);
        img.setBackground(round(ClockRenderer.BG_BLACK.equals(file) ? 0xFF000000 : ClockRenderer.BLUE, 8, 0));
        if (file.isEmpty()) img.setImageResource(R.drawable.logo);
        else if (!ClockRenderer.BG_BLACK.equals(file)) loadThumb("thumbs/" + file, img);
        cell.addView(img, new LinearLayout.LayoutParams(px(64), px(110)));
        TextView t = label(name, 11, ClockRenderer.CREAM);
        t.setGravity(Gravity.CENTER);
        t.setMaxWidth(px(64));
        t.setSingleLine(true);
        cell.addView(t, new LinearLayout.LayoutParams(px(64), ViewGroup.LayoutParams.WRAP_CONTENT));
        cell.setOnClickListener(v -> {
            prefs.edit().putString(ClockRenderer.KEY_BG, file).apply();
            refreshClockUi();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(px(6));
        bgRow.addView(cell, lp);
    }

    /** An on/off row bound to a boolean preference. */
    private void toggle(LinearLayout parent, String text, String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(text);
        s.setTypeface(font);
        s.setTextSize(15.5f);
        s.setTextColor(ClockRenderer.CREAM);
        s.setPadding(0, px(8), 0, px(8));
        int[][] states = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(states, new int[]{ClockRenderer.GOLD, 0xFF8A8F9C}));
        s.setTrackTintList(new ColorStateList(states, new int[]{0x99D9B15C, 0x55FFFFFF}));
        s.setChecked(prefs.getBoolean(key, def));
        s.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            refreshClockUi();
        });
        parent.addView(s, matchWrap(4));
    }

    private void refreshClockUi() {
        if (preview == null) return;
        final ClockRenderer.Style st = ClockRenderer.Style.from(prefs);
        for (Object[] cr : choiceRows) {
            String key = (String) cr[0];
            LinearLayout row = (LinearLayout) cr[1];
            int sel = prefs.getInt(key, (Integer) cr[2]);
            for (int i = 0; i < row.getChildCount(); i++) {
                TextView t = (TextView) row.getChildAt(i);
                boolean on = i == sel;
                t.setBackground(round(on ? ClockRenderer.GOLD : 0x00000000, 16, on ? 0 : 0x66D9B15C, 1));
                t.setTextColor(on ? ClockRenderer.DEEP : ClockRenderer.CREAM);
            }
        }
        if (bgRow != null) {
            for (int i = 0; i < bgRow.getChildCount(); i++) {
                View c = bgRow.getChildAt(i);
                boolean on = st.bg.equals(c.getTag());
                c.setBackground(on ? round(0x00000000, 10, ClockRenderer.GOLD) : null);
            }
        }
        final ImageView target = preview;
        final int generation = ++previewGeneration;
        final boolean showSystem = lockPreview;
        pool.execute(() -> {
            int w = 540, h = 1200;
            Bitmap bgb = ClockRenderer.loadBg(this, st.bgFile(), w, h);
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            Intent bi = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = bi == null ? 0 : Math.round(100f * bi.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0)
                    / Math.max(1, bi.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)));
            int status = bi == null ? 0 : bi.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, 0);
            ClockRenderer.draw(this, c, w, h, st, bgb, level, status == 2 || status == 5, Calendar.getInstance());
            if (showSystem) {
                android.graphics.Paint pp = new android.graphics.Paint(3);
                pp.setColor(0xffeeeeee);
                pp.setTextAlign(android.graphics.Paint.Align.CENTER);
                pp.setTextSize(w * .12f);
                pp.setShadowLayer(3, 0, 1, 0xff000000);
                c.drawText(new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date()), w / 2f, h * .18f, pp);
                pp.setTextSize(w * .025f);
                c.drawText("معاينة ساعة النظام", w / 2f, h * .095f, pp);
            }
            if (bgb != null) bgb.recycle();
            runOnUiThread(() -> {
                if (target == preview && generation == previewGeneration) target.setImageBitmap(out);
                else out.recycle();
            });
        });
    }

    private void activateLive() {
        ComponentName cn = new ComponentName(this, ClockWallpaperService.class);
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, cn);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
                toast("اختر «مشكاة – خلفيات حية» من القائمة");
            } catch (ActivityNotFoundException e2) {
                toast("هاتفك لا يدعم الخلفيات الحية");
            }
        }
    }

    // ------------------------------------------------------------------ icons

    private View iconsTab() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(16), 0, px(16), 0);
        pageHeader(col, "أيقونات مشكاة", ICONS.length + " أيقونة بتصميم موحّد لتطبيقاتك");

        LinearLayout info = card(col, null);
        ((LinearLayout.LayoutParams) info.getLayoutParams()).topMargin = 0;
        info.addView(label("طريقة الاستخدام", 15.5f, ClockRenderer.GOLD));
        info.addView(label("١. احفظ الأيقونات في معرض الصور.\n"
                + "٢. من مشغّل يدعم تغيير الأيقونات مثل Nova Launcher: اضغط مطوّلًا على التطبيق ← تعديل ← الصورة.", 14, MUTED));
        col.addView(primaryButton("حفظ الأيقونات في المعرض", v -> saveIconsChecked()), matchWrap(12));

        GridView g = new GridView(this);
        g.setNumColumns(4);
        g.setVerticalSpacing(px(14));
        g.setPadding(0, px(16), 0, px(16));
        g.setClipToPadding(false);
        g.setSelector(new android.graphics.drawable.ColorDrawable(0));
        g.setAdapter(new BaseAdapter() {
            public int getCount() { return ICONS.length; }
            public Object getItem(int p) { return ICONS[p]; }
            public long getItemId(int p) { return p; }
            public View getView(int p, View v, ViewGroup parent) {
                LinearLayout cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                ImageView img = new ImageView(MainActivity.this);
                loadThumb("icons/" + ICONS[p] + ".png", img);
                cell.addView(img, new LinearLayout.LayoutParams(px(56), px(56)));
                TextView t = label(ICON_NAMES[p], 12.5f, ClockRenderer.CREAM);
                t.setGravity(Gravity.CENTER);
                t.setSingleLine(true);
                cell.addView(t);
                return cell;
            }
        });
        col.addView(g, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return col;
    }

    private void saveIconsChecked() {
        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        saveIcons();
    }

    private android.location.LocationListener locationListener;
    private final android.os.Handler locationHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private void locate() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 18);
            return;
        }
        android.location.LocationManager lm = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationListener != null) lm.removeUpdates(locationListener);
        locationListener = new android.location.LocationListener() {
            public void onLocationChanged(android.location.Location l) {
                if (locationListener != this) return;
                lm.removeUpdates(this);
                locationListener = null;
                prefs.edit().putFloat("prayer_lat", (float) l.getLatitude()).putFloat("prayer_lon", (float) l.getLongitude())
                        .putBoolean("prayer_gps", true).apply();
                toast("تم تحديد الموقع ✓");
                if (currentTab == TAB_DESIGN) { rebuildPanel(); refreshClockUi(); }
            }
            public void onProviderEnabled(String p) { }
            public void onProviderDisabled(String p) { }
            public void onStatusChanged(String p, int status, Bundle extras) { }
        };
        boolean started = false;
        for (String provider : new String[]{android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER}) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(provider, 1000, 0, locationListener);
                    started = true;
                }
            } catch (Exception ignored) { }
        }
        if (!started) { locationListener = null; toast("فعّل الموقع أو اختر المدينة يدويًا"); return; }
        toast("جارٍ تحديد الموقع…");
        final android.location.LocationListener pending = locationListener;
        locationHandler.postDelayed(() -> {
            if (locationListener == pending) {
                lm.removeUpdates(pending);
                locationListener = null;
                toast("تعذّر تحديد الموقع؛ اختر المدينة يدويًا");
            }
        }, 30000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationListener != null) {
            ((android.location.LocationManager) getSystemService(LOCATION_SERVICE)).removeUpdates(locationListener);
            locationListener = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == 18) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate();
            else toast("يمكن اختيار المدينة يدويًا");
            return;
        }
        if (req == REQ_STORAGE && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) saveIcons();
        else if (req == REQ_STORAGE) toast("لم يُسمح بالحفظ في الذاكرة");
    }

    private void saveIcons() {
        toast("جارٍ الحفظ…");
        pool.execute(() -> {
            int ok = 0;
            for (String n : ICONS) {
                String fileName = "mishkat_" + n + ".png";
                try (InputStream in = getAssets().open("icons/" + n + ".png")) {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                        cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MishkatLiveThemes");
                        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                        if (uri == null) continue;
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            copy(in, out);
                        }
                    } else {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MishkatLiveThemes");
                        if (!dir.exists() && !dir.mkdirs()) continue;
                        File f = new File(dir, fileName);
                        try (OutputStream out = new FileOutputStream(f)) {
                            copy(in, out);
                        }
                        MediaScannerConnection.scanFile(this, new String[]{f.getAbsolutePath()}, null, null);
                    }
                    ok++;
                } catch (Exception ignored) {
                }
            }
            final int saved = ok;
            runOnUiThread(() -> toast(saved > 0
                    ? "حُفظت " + saved + " أيقونة في الصور ← مجلد MishkatLiveThemes"
                    : "تعذّر حفظ الأيقونات"));
        });
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buf = new byte[16384];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
    }

    // ------------------------------------------------------------------ about & credits

    private View aboutTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(16), px(16), px(16), px(28));
        sv.addView(col);

        // app identity card
        LinearLayout id = card(col, null);
        ((LinearLayout.LayoutParams) id.getLayoutParams()).topMargin = 0;
        id.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(px(72), px(72));
        llp.topMargin = px(6);
        id.addView(logo, llp);
        TextView name = label("مشكاة – ثيمات الهاتف", 20, ClockRenderer.GOLD);
        name.setGravity(Gravity.CENTER);
        id.addView(name);
        String version = "";
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
        TextView ver = label("الإصدار " + version, 13, MUTED);
        ver.setGravity(Gravity.CENTER);
        id.addView(ver);
        TextView about = label("ساعة حية ذات عقارب أو مواقيت للصلاة على شاشة القفل، مع خلفيات طبيعية وإسلامية وأيقونات بتصميم موحّد.", 14, ClockRenderer.CREAM);
        about.setGravity(Gravity.CENTER);
        about.setPadding(0, px(8), 0, 0);
        id.addView(about);

        // contact
        LinearLayout contact = card(col, "تواصل معنا");
        TextView m = label("مشكاة · برمجيات نافعة للهاتف والحاسب\nhttps://mishkatapps.github.io\nmishkatapps@gmail.com", 14, ClockRenderer.CREAM);
        Linkify.addLinks(m, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        m.setLinkTextColor(ClockRenderer.GOLD);
        m.setMovementMethod(LinkMovementMethod.getInstance());
        contact.addView(m);

        // design credit
        LinearLayout design = card(col, "التصميم والخط");
        design.addView(label("خلفيات مشكاة والأيقونات والساعات من تصميم «مشكاة».\n"
                + "خط «أميري» – Amiri، بترخيص SIL Open Font License 1.1.", 14, MUTED));

        // photo credits
        LinearLayout photos = card(col, "حقوق الصور");
        TextView lic = label("صور الطبيعة من موقع ويكيميديا كومنز، منشورة بترخيص المشاع الإبداعي "
                + "«نسب المصنَّف – الترخيص بالمثل 4.0» (CC BY-SA 4.0). قُصَّت الصور لمقاس الهاتف وأضيف إليها تظليل خفيف، "
                + "والصور المعدّلة متاحة بالترخيص نفسه.\nhttps://creativecommons.org/licenses/by-sa/4.0/", 13.5f, MUTED);
        Linkify.addLinks(lic, Linkify.WEB_URLS);
        lic.setLinkTextColor(ClockRenderer.GOLD);
        lic.setMovementMethod(LinkMovementMethod.getInstance());
        photos.addView(lic);
        try (InputStream in = getAssets().open("credits.txt")) {
            byte[] all = new byte[in.available()];
            int n = in.read(all);
            String[] lines = new String(all, 0, Math.max(0, n), "UTF-8").split("\n");
            for (String line : lines) {
                String[] p = line.split("\t");
                if (p.length < 4) continue;
                photos.addView(divider(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1))));
                LinearLayout entry = new LinearLayout(this);
                entry.setOrientation(LinearLayout.VERTICAL);
                entry.setPadding(0, px(8), 0, px(8));
                entry.addView(label(p[0], 15, ClockRenderer.CREAM));
                entry.addView(label("المصوّر: " + p[1] + " · " + p[2], 13, MUTED));
                TextView link = label(p[3], 12, ClockRenderer.GOLD);
                link.setTextDirection(View.TEXT_DIRECTION_LTR);
                Linkify.addLinks(link, Linkify.WEB_URLS);
                link.setLinkTextColor(ClockRenderer.GOLD);
                link.setMovementMethod(LinkMovementMethod.getInstance());
                entry.addView(link);
                photos.addView(entry);
            }
        } catch (Exception ignored) {
        }
        return sv;
    }

    // ------------------------------------------------------------------ ui helpers

    private int px(float v) { return Math.round(v * dp); }

    private TextView label(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTypeface(font);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private TextView muted(String s) {
        TextView t = label(s, 13.5f, MUTED);
        t.setPadding(0, px(2), 0, px(2));
        return t;
    }

    /** Page title + one-line description at the top of every page. */
    private void pageHeader(LinearLayout parent, String title, String sub) {
        TextView t = label(title, 22, ClockRenderer.CREAM);
        t.setPadding(0, px(14), 0, 0);
        parent.addView(t);
        TextView s = label(sub, 13.5f, MUTED);
        s.setPadding(0, 0, 0, px(12));
        parent.addView(s);
    }

    /** A rounded panel that groups related settings under one heading. */
    private LinearLayout card(LinearLayout parent, String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(px(16), px(12), px(16), px(14));
        c.setBackground(round(CARD, 18, LINE, 1));
        if (title != null) c.addView(label(title, 17, ClockRenderer.GOLD));
        parent.addView(c, matchWrap(12));
        return c;
    }

    /** Segmented control: equal-width options, the selected one filled in gold. */
    private LinearLayout segmented(String[] names, int sel, float sp, IntConsumer pick) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(px(4), px(4), px(4), px(4));
        bar.setBackground(round(CARD, 16, LINE, 1));
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView t = label(names[i], sp, ClockRenderer.CREAM);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(px(2), px(9), px(2), px(9));
            t.setOnClickListener(v -> {
                styleSegments(bar, idx);
                pick.accept(idx);
            });
            bar.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        styleSegments(bar, sel);
        return bar;
    }

    private void styleSegments(LinearLayout bar, int sel) {
        for (int i = 0; i < bar.getChildCount(); i++) {
            TextView t = (TextView) bar.getChildAt(i);
            boolean on = i == sel;
            t.setBackground(on ? round(ClockRenderer.GOLD, 12, 0) : ripple(null));
            t.setTextColor(on ? ClockRenderer.DEEP : ClockRenderer.CREAM);
        }
    }

    private TextView primaryButton(String text, View.OnClickListener click) {
        TextView b = label(text, 18, ClockRenderer.DEEP);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(10), px(12), px(10), px(12));
        b.setBackground(ripple(round(ClockRenderer.GOLD, 24, 0)));
        b.setElevation(px(2));
        b.setOnClickListener(click);
        return b;
    }

    private TextView outlineButton(String text, View.OnClickListener click) {
        TextView b = label(text, 15, ClockRenderer.GOLD);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setPadding(px(8), px(10), px(8), px(10));
        b.setBackground(ripple(round(0x00000000, 22, 0x99D9B15C, 1)));
        b.setOnClickListener(click);
        return b;
    }

    private TextView smallButton(String text, boolean filled) {
        TextView b = label(text, 14, filled ? ClockRenderer.DEEP : ClockRenderer.GOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(16), px(6), px(16), px(6));
        b.setBackground(ripple(filled ? round(ClockRenderer.GOLD, 16, 0) : round(0x00000000, 16, 0x99D9B15C, 1)));
        return b;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(LINE);
        return v;
    }

    private LinearLayout.LayoutParams matchWrap(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(topDp);
        return lp;
    }

    private LinearLayout.LayoutParams weighted(int startDp, int endDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMarginStart(px(startDp));
        lp.setMarginEnd(px(endDp));
        return lp;
    }

    private Drawable ripple(Drawable content) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(px(16));
        return new RippleDrawable(ColorStateList.valueOf(0x33D9B15C), content, mask);
    }

    private GradientDrawable round(int fill, float radiusDp, int stroke) {
        return round(fill, radiusDp, stroke, 2);
    }

    private GradientDrawable round(int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, px(strokeDp)), stroke);
        return g;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void loadThumb(String asset, ImageView into) {
        Bitmap hit = cache.get(asset);
        into.setTag(asset);
        if (hit != null) { into.setImageBitmap(hit); return; }
        into.setImageDrawable(null);
        pool.execute(() -> {
            try (InputStream in = getAssets().open(asset)) {
                Bitmap b = BitmapFactory.decodeStream(in);
                if (b == null) return;
                cache.put(asset, b);
                runOnUiThread(() -> { if (asset.equals(into.getTag())) into.setImageBitmap(b); });
            } catch (Exception ignored) {
            }
        });
    }

    /** ImageView that keeps a fixed height/width ratio. */
    static class AspectImage extends ImageView {
        private final float ratio;
        AspectImage(android.content.Context c, float ratio) { super(c); this.ratio = ratio; }
        @Override protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            setMeasuredDimension(w, Math.round(w * ratio));
        }
    }
}
