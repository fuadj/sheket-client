package com.mukera.sheket.client.controller.importer;

import android.os.AsyncTask;

import java.io.File;

/**
 * Created by fuad on 6/9/16.
 *
 * This task goes through the main-ui and a worker thread twice.
 * The lifecycle looks like:
 *
 *          Main Thread                     Worker Thread
 *
 *          MainActivity starts this --->
 *
 *                                          parseFile on worker         -----(1st-stage)
 *
 *          Display ImportDataMappingDialog  <----
 *              to user
 *                                  ---->   add data to ContentProvider -----(2nd-stage)
 *
 *          notify main thread on   <----
 *              task finish
 */
public class ParseFileTask extends AsyncTask<Void, Void, SimpleCSVReader> {
    private SimpleCSVReader mReader;

    private File mFile;

    private ImportListener mListener;
    public void setListener(ImportListener listener) { mListener = listener; }

    public ParseFileTask(File file) {
        mFile = file;
    }

    @Override
    protected SimpleCSVReader doInBackground(Void... params) {
        mReader = new SimpleCSVReader(mFile);
        mReader.parseCSV();
        return mReader;
    }

    @Override
    protected void onPostExecute(SimpleCSVReader reader) {
        if (!reader.parsingSuccess()) {
            mListener.importError("Import Error: " + reader.getErrorMessage());
        } else {
            mListener.displayDataMappingDialog(reader);
        }
    }

}
