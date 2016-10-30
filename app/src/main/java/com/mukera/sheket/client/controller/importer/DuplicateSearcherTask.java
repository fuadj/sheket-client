package com.mukera.sheket.client.controller.importer;

import android.os.AsyncTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.mukera.sheket.client.utils.DuplicateFinder;

/**
 * Created by fuad on 6/26/16.
 */
public class DuplicateSearcherTask extends AsyncTask<Void, Void, DuplicateEntities> {
    private SimpleCSVReader mReader;
    private Map<Integer, Integer> mDataMapping;
    private SearchFinishedListener mListener;

    public interface SearchFinishedListener {
        void duplicateSearchFinished(DuplicateEntities duplicateEntities);
    }

    public DuplicateSearcherTask(SimpleCSVReader reader, Map<Integer, Integer> mapping, SearchFinishedListener listener) {
        mReader = reader;
        mDataMapping = mapping;
        mListener = listener;
    }

    @Override
    protected DuplicateEntities doInBackground(Void... params) {
        DuplicateEntities duplicateEntities = new DuplicateEntities();

        if (mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY) !=
                ColumnMappingDialog.NO_DATA_FOUND) {
            findDuplicateCategories(duplicateEntities);
        }

        if (mDataMapping.get(ColumnMappingDialog.DATA_BRANCH) !=
                ColumnMappingDialog.NO_DATA_FOUND) {
            findDuplicateBranches(duplicateEntities);
        }

        return duplicateEntities;
    }

    void findDuplicateCategories(DuplicateEntities duplicateEntities) {
        Set<String> categories = new HashSet<>();
        int category_col = mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY);

        for (int i = 0; i < mReader.getNumRows(); i++) {
            categories.add(mReader.getRowAt(i).get(category_col));
        }

        duplicateEntities.categoryDuplicates = DuplicateFinder.findDuplicates(categories, DuplicateFinder.DISTANCE_2_COMPARATOR);
        removeNonDuplicateWords(duplicateEntities.categoryDuplicates);
    }

    void findDuplicateBranches(DuplicateEntities duplicateEntities) {
        Set<String> branches = new HashSet<>();
        int branch_col = mDataMapping.get(ColumnMappingDialog.DATA_BRANCH);

        for (int i = 0; i < mReader.getNumRows(); i++) {
            branches.add(mReader.getRowAt(i).get(branch_col));
        }

        duplicateEntities.branchDuplicates = DuplicateFinder.findDuplicates(branches, DuplicateFinder.DISTANCE_2_COMPARATOR);
        removeNonDuplicateWords(duplicateEntities.branchDuplicates);
    }

    /**
     * Remove "rows" from the vector which are single words.(i.e: they don't have any
     * duplicates and are by themselves.) Only consider the "rows" that have at-least 2 words.
     * @param duplicateLists
     */
    void removeNonDuplicateWords(Vector<Vector<String>> duplicateLists) {
        for (int i = 0; i < duplicateLists.size(); i++) {
            if (duplicateLists.get(i).size() < 2) {
                duplicateLists.remove(i);

                // because we removed an element, reset it so next iteration doesn't jump stuff
                i--;
            }
        }
    }

    @Override
    protected void onPostExecute(DuplicateEntities duplicateEntities) {
        mListener.duplicateSearchFinished(duplicateEntities);
    }
}
