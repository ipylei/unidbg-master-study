package com.androidsdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;

public class SharedPreferences {
    private JSONObject jsonObject;
    private final File mFile;

    SharedPreferences(File file) {
        mFile = file;
        jsonObject = null;
        loadFromDisk();
    }

    private void loadFromDisk() {
        if (mFile.exists() && !mFile.canRead()) {
            System.out.println("Attempt to read preferences file " + mFile + " without permission");
        }
        BufferedInputStream str;
        try {
            str = new BufferedInputStream(new FileInputStream(mFile), 16 * 1024);
            readSP(str);
        } catch (Exception e) {
            System.out.println("getSharedPreferences:" + e);
        }
    }

    public String getString(String key, String defValue) {
        String v = (String) jsonObject.get(key);
        return v != null ? v : defValue;
    }

    public void putString(String key, String value) {
        jsonObject.put(key, value);
    }

    private void readSP(InputStream in) {
        char[] buf = new char[16 * 1024];
        InputStreamReader input;
        try {
            input = new InputStreamReader(in);
            int len = input.read(buf);
            String text = new String(buf, 0, len);
            jsonObject = JSON.parseObject(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
