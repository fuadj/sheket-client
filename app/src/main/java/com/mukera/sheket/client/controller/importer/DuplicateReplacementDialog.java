package com.mukera.sheket.client.controller.importer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created by fuad on 6/26/16.
 */
public class DuplicateReplacementDialog extends DialogFragment {
    private Vector<String> mWordList;
    private String mEntityType;

    public static DuplicateReplacementDialog newInstance(Vector<String> wordList,
                                                         String entityType) {
        DuplicateReplacementDialog dialog = new DuplicateReplacementDialog();
        dialog.mWordList = wordList;
        dialog.mEntityType = entityType;
        return dialog;
    }

    /**
     * Represents duplicate words. When >= 2 words have duplicates,
     * they are trying to represent a single word. So we have a single
     * correct word and a set of words that mean the same thing.
     */
    public static class Replacement {
        public String correctWord;
        public Set<String> duplicates;
    }

    public interface ReplacementListener {
        void noDuplicatesFound();

        /**
         * Callback for the duplicates found.
         * @param nonDuplicates   This are words that were "possible candidates" to have duplicates,
         *                        but the user has identified them as unique and aren't duplicates.
         *
         * @param replacement     The user selected replacement to "group" the duplicates as one
         *                        unique word. See class {@code Replacement}.
         */
        void duplicatesFound(Set<String> nonDuplicates, Replacement replacement);
    }

    private ReplacementListener mListener;
    public void setListener(ReplacementListener listener) { mListener = listener; }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] words = mWordList.toArray(new String[]{});
        final boolean[] checkedWords = new boolean[words.length];
        for (int i = 0; i < words.length; i++) {
            // The DEFAULT is for the words to be duplicate
            checkedWords[i] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(words, checkedWords, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedWords[which] = isChecked;
                int selected = 0;
                for (Boolean check : checkedWords) {
                    if (check) {
                        selected++;
                        // we need at least 2 to have duplicates, a single word can't be a duplicate
                        // this check is for efficiency
                        if (selected == 2) break;
                    }
                }

                ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected >= 2);
            }
        }).
                setCancelable(false).
                setTitle("Select the duplicate " + mEntityType).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If we get here, it means at least 2 words have been selected
                        // now choose which one is the "correct one"

                        Vector<String> duplicates = new Vector<>();
                        for (int i = 0; i < checkedWords.length; i++) {
                            if (checkedWords[i]) {      // this was selected as duplicate
                                duplicates.add(mWordList.get(i));
                            }
                        }

                        final String[] duplicateWords = duplicates.toArray(new String[]{});

                        AlertDialog.Builder chooser = new AlertDialog.Builder(getContext());
                        chooser.setSingleChoiceItems(duplicateWords, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // once a word is selected, we are good to go.
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).
                                        setEnabled(true);
                            }
                        }).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                dialog.dismiss();

                                correctWordChosen(selectedPosition, duplicateWords);
                            }
                        }).setNeutralButton("Back", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).setTitle("Choose The Correct Word");

                        AlertDialog chooserDialog = chooser.create();
                        chooserDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {
                                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            }
                        });
                        chooserDialog.show();
                    }
                }).
                // we don't use the negative b/c its location is closer to the positive btn
                // the neutral is at the opposite end of the dialog
                setNeutralButton("Not Duplicates", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.noDuplicatesFound();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // this is here for convenience, if we want to disable it we can.
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });
        return dialog;
    }

    void correctWordChosen(int position, String[] words) {
        Set<String> duplicates = new HashSet<>(Arrays.asList(words));

        Set<String> non_duplicates = new HashSet<>();
        for (String word : mWordList) {
            if (!duplicates.contains(word)) {
                non_duplicates.add(word);
            }
        }

        Replacement replacement = new Replacement();
        replacement.correctWord = words[position];
        duplicates.remove(replacement.correctWord);
        replacement.duplicates = duplicates;

        mListener.duplicatesFound(non_duplicates, replacement);
    }
}
