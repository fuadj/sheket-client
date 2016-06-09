package com.mukera.sheket.client.importer;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by fuad on 6/9/16.
 */
public class SimpleCSVReader {
    private File mFile;
    private Vector<String> mHeaders;
    private Vector<Vector<String>> mData;

    public SimpleCSVReader(File file) {
        mFile = file;
        mHeaders = new Vector<>();
        mData = new Vector<>();
    }

    private Vector<String> splitAndTrim(String data) {
        String[] columns = data.split(",");
        Vector<String> result = new Vector<>();
        for (String s : columns) {
            result.add(s.trim());
        }
        return result;
    }

    public void parseCSV() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(mFile));
            String line;

            if ((line = reader.readLine()) == null) {
                throw new CSVReaderException("No Headers found");
            }

            Vector<String> headers = splitAndTrim(line);
            if (headers.isEmpty()) {
                throw new CSVReaderException("Empty Header Line");
            }

            Map<Integer, Boolean> emptyColumns = new HashMap<>();

            for (int i = 0; i < headers.size(); i++) {
                String col = headers.get(i);
                if (TextUtils.isEmpty(col)) {
                    emptyColumns.put(i, Boolean.TRUE);
                } else {
                    mHeaders.add(col);
                    emptyColumns.put(i, Boolean.FALSE);
                }
            }

            // read in the data
            while ((line = reader.readLine()) != null) {
                Vector<String> cols = splitAndTrim(line);
                // this line isn't full
                if (cols.size() != headers.size()) continue;

                Vector<String> row = new Vector<>();
                for (int i = 0; i < cols.size(); i++) {
                    if (emptyColumns.get(i) == Boolean.FALSE) {
                        // only add the non empty columns
                        row.add(cols.get(i));
                    }
                }
                mData.add(row);
            }
            reader.close();
        } catch (IOException | CSVReaderException e) {
            // clear the data
            mHeaders = new Vector<>();
            mData = new Vector<>();
        }
    }

    public Vector<String> getHeaders() {
        return mHeaders;
    }

    public int getNumLines() {
        return mData.size();
    }

    public Vector<String> getRowAt(int i) {
        return mData.get(i);
    }

    private class CSVReaderException extends Exception {
        public CSVReaderException() { super(); }

        public CSVReaderException(String detailMessage) { super(detailMessage); }

        public CSVReaderException(String detailMessage, Throwable throwable) { super(detailMessage, throwable); }

        public CSVReaderException(Throwable throwable) { super(throwable); }
    }
}
