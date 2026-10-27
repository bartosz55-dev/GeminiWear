package com.antigravity.geminiwear;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImagePickerActivity extends Activity {
    public static final String EXTRA_IMAGE_PATH = "image_path";

    public static class ImageItem {
        public String path;
        public String name;
        public long dateAdded;

        public ImageItem(String path, String name, long dateAdded) {
            this.path = path;
            this.name = name;
            this.dateAdded = dateAdded;
        }
    }

    private ListView listView;
    private TextView tvEmpty;
    private final List<ImageItem> imageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);

        listView = findViewById(R.id.lvImages);
        tvEmpty = findViewById(R.id.tvEmpty);

        loadImages();

        if (imageList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(new ImageAdapter());
        }
    }

    private void loadImages() {
        imageList.clear();

        // 1. Query Android MediaStore
        try {
            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE
            };

            Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            );

            if (cursor != null) {
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                while (cursor.moveToNext()) {
                    String path = cursor.getString(dataCol);
                    String name = cursor.getString(nameCol);
                    long date = cursor.getLong(dateCol);
                    long size = cursor.getLong(sizeCol);

                    if (isValidImage(path, name, size)) {
                        imageList.add(new ImageItem(path, formatImageName(name), date * 1000));
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {}

        // 2. Direct scan of Screenshots and Camera directories
        File[] directDirs = new File[]{
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots"),
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        };

        for (File dir : directDirs) {
            scanDir(dir);
        }

        // Sort by date descending (newest first)
        Collections.sort(imageList, new Comparator<ImageItem>() {
            @Override
            public int compare(ImageItem o1, ImageItem o2) {
                return Long.compare(o2.dateAdded, o1.dateAdded);
            }
        });
    }

    private void scanDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isFile()) {
                String path = f.getAbsolutePath();
                String name = f.getName();
                long size = f.length();

                if (isValidImage(path, name, size)) {
                    boolean exists = false;
                    for (ImageItem item : imageList) {
                        if (item.path.equals(path)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        imageList.add(new ImageItem(path, formatImageName(name), f.lastModified()));
                    }
                }
            }
        }
    }

    private boolean isValidImage(String path, String name, long size) {
        if (path == null || name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);

        // Ignore small placeholder icons and junk
        if (size > 0 && size < 5000) return false;

        // Ignore system screenshots dumped for test
        if (lower.startsWith("gemini_")) return false;

        // Ignore demo files
        if (lower.equals("balloons.png") || lower.equals("puppy.jpg") || lower.equals("animation.gif")) return false;

        // Ignore thumbnails
        if (path.contains("/.thumbnails/")) return false;

        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    private String formatImageName(String name) {
        if (name == null) return "Zdjęcie";
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("screenshot")) {
            return "Zrzut ekranu";
        }
        if (lower.contains("messenger")) {
            return "Z Messenger";
        }
        return name;
    }

    private class ImageAdapter extends BaseAdapter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        @Override
        public int getCount() {
            return imageList.size();
        }

        @Override
        public Object getItem(int position) {
            return imageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ImagePickerActivity.this).inflate(R.layout.item_image, parent, false);
                ViewMotionHelper.applyTouchFeedback(convertView);
            }

            final ImageItem item = imageList.get(position);
            ImageView ivThumb = convertView.findViewById(R.id.ivThumb);
            TextView tvName = convertView.findViewById(R.id.tvName);
            TextView tvDate = convertView.findViewById(R.id.tvDate);

            tvName.setText(item.name);
            if (item.dateAdded > 0) {
                tvDate.setText(sdf.format(new Date(item.dateAdded)));
            } else {
                tvDate.setText("");
            }

            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;
                Bitmap bm = BitmapFactory.decodeFile(item.path, opts);
                if (bm != null) {
                    ivThumb.setImageBitmap(bm);
                } else {
                    ivThumb.setImageResource(R.drawable.ic_image);
                }
            } catch (Exception e) {
                ivThumb.setImageResource(R.drawable.ic_image);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_IMAGE_PATH, item.path);
                    setResult(RESULT_OK, data);
                    finish();
                }
            });

            return convertView;
        }
    }
}
