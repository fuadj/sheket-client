package com.mukera.sheket.client.controller.importer;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by fuad on 6/9/16.
 *
 * Parses a CSV file. Parsers the first line of the file to get the "headers" columns. These should then
 * exist in every row after. If a row doesn't have a column for a given "header" column, then that row
 * is ignored.
 *
 * The parser is case insensitive and ignores duplicate entities with different "case"s. When a duplicate
 * entity is found, its first occurrence is used.
 */
public class SimpleCSVReader {
    private File mFile;
    private Vector<String> mHeaders;
    private Vector<Vector<String>> mData;
    private String mErrorMsg = null;

    private int mNumLinesSkipped;
    private int mNumLinesFewerColumns;
    private int mNumLinesMoreColumns;

    public SimpleCSVReader(File file) {
        mFile = file;
        mHeaders = new Vector<>();
        mData = new Vector<>();
        mNumLinesSkipped = 0;
        mNumLinesFewerColumns = 0;
        mNumLinesMoreColumns = 0;
    }

    private Vector<String> splitAndTrim(String data) {
        String[] columns = data.split(",");
        Vector<String> result = new Vector<>();
        for (String s : columns) {
            result.add(s.trim());
        }
        return result;
    }

    public String getErrorMessage() {
        return mErrorMsg;
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

            /**
             * For each column, we keep a which names have been used. The names
             * are converted to lower case and any extra space around AND
             * within(e.g: 2 space('  ') instead of 1(' ') space) are removed.
             * When we find a name already used, we replace it with the
             * first occurrence of it.
             */
            Map<Integer, Map<String, String>> duplicateColumns = new HashMap<>();

            // read in the data
            while ((line = reader.readLine()) != null) {
                Vector<String> cols = splitAndTrim(line);

                if (cols.isEmpty())
                    continue;

                // the row and headers don't have equal columns
                if (cols.size() != headers.size()) {
                    mNumLinesSkipped++;
                    if (cols.size() < headers.size()) {
                        mNumLinesFewerColumns++;
                    } else {
                        mNumLinesMoreColumns++;
                    }
                    continue;
                }

                Vector<String> row = new Vector<>();
                for (int i = 0; i < cols.size(); i++) {
                    if (emptyColumns.get(i) == Boolean.TRUE) {
                        // ignore columns if the header isn't defined for it.
                        continue;
                    }

                    String name = removeInnerSpaces(cols.get(i));
                    String name_keyed = name.toLowerCase();

                    if (!duplicateColumns.containsKey(i)) {
                        duplicateColumns.put(i, new HashMap<String, String>());
                    }

                    Map<String, String> columnNames = duplicateColumns.get(i);

                    if (columnNames.containsKey(name_keyed)) {
                        // use the previously used name
                        row.add(columnNames.get(name_keyed));
                    } else {
                        row.add(name);
                        columnNames.put(name_keyed, name);
                    }
                }
                mData.add(row);
            }
            reader.close();
            mErrorMsg = "";
        } catch (IOException | CSVReaderException e) {
            // clear the data
            mHeaders = new Vector<>();
            mData = new Vector<>();
            mErrorMsg = e.getMessage();
        }
    }

    String removeInnerSpaces(String s) {
        String[] space_removed = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < space_removed.length; j++) {
            if (j > 0) sb.append(" ");
            sb.append(space_removed[j]);
        }

        return sb.toString();
    }

    public boolean parsingSuccess() {
        return !mHeaders.isEmpty() &&
                !mData.isEmpty();
    }

    public Vector<String> getHeaders() {
        return mHeaders;
    }

    public int getNumSkippedLines() { return mNumLinesSkipped; }
    public int getNumLinesWithFewerColumnsThanHeader() { return mNumLinesFewerColumns; }
    public int getNumLinesWithMoreColumnsThanHeader() { return mNumLinesMoreColumns; }

    public int getNumRows() {
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
