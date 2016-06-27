package com.mukera.sheket.client.controller.importer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;

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
         *
         * @param nonDuplicates This are words that were "possible candidates" to have duplicates,
         *                      but the user has identified them as unique and aren't duplicates.
         * @param replacement   The user selected replacement to "group" the duplicates as one
         *                      unique word. See class {@code Replacement}.
         */
        void duplicatesFound(Set<String> nonDuplicates, Replacement replacement);
    }

    private ReplacementListener mListener;

    public void setListener(ReplacementListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] words = new String[mWordList.size()];
        mWordList.toArray(words);

        final boolean[] selectedWords = new boolean[words.length];
        for (int i = 0; i < words.length; i++) {
            // The DEFAULT is for the words to be duplicate
            selectedWords[i] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setCancelable(false);
        builder.setMultiChoiceItems(words, selectedWords, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                selectedWords[which] = isChecked;
                int selected = 0;
                for (Boolean check : selectedWords) {
                    if (check) {
                        selected++;
                        // we need at least 2 to have duplicates, a single word can't be a duplicate
                        // this check is for efficiency
                        if (selected == 2) break;
                    }
                }

                ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected >= 2);
            }
        }).setCancelable(false).
                setTitle("Select the duplicate " + mEntityType).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface outerDialog, int which) {
                        // If we get here, it means at least 2 words have been selected
                        // now choose which one is the "correct one"

                        Vector<String> duplicates = new Vector<>();
                        for (int i = 0; i < selectedWords.length; i++) {
                            if (selectedWords[i]) {      // this was selected as duplicate
                                duplicates.add(mWordList.get(i));
                            }
                        }

                        final String[] duplicateWords = new String[duplicates.size()];
                        duplicates.toArray(duplicateWords);

                        DialogFragment wordChooserDialog = new DialogFragment() {
                            @NonNull
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                final DialogFragment self = this;
                                AlertDialog dialog = new AlertDialog.Builder(getContext()).setCancelable(false).
                                        setSingleChoiceItems(duplicateWords, -1,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // once a word is selected, we are good to go.
                                                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).
                                                                setEnabled(true);
                                                    }
                                                }).setPositiveButton("Ok",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                                self.dismiss();

                                                correctWordChosen(selectedPosition, duplicateWords);
                                            }
                                        }).setNeutralButton("Back",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                self.dismiss();
                                            }
                                        }).setTitle("Choose The Correct Word").create();

                                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialog) {
                                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    }
                                });

                                dialog.setCanceledOnTouchOutside(false);
                                return dialog;
                            }
                        };

                        /**
                         * Because the {@code DuplicateReplacementDialog} is a 2-level dialog(i.e:
                         *      it first asks for the duplicates from a possible list of words, it then
                         *      asks for the "correct" version of the word) we want to add the first
                         *      dialog to the backstack if the user navigates back from the second
                         *      to the first. But the normal {@code show()} method for dialogs doesn't
                         *      add them to the backstack, trying to go back from the second to the first
                         *      will also cause the first to be removed. This creates a UI bug where
                         *      the importing is frozen due to the dialogs disappearing.
                         *
                         * Reference https://books.google.com.et/books?id=I4knCgAAQBAJ&pg=PA51&lpg=PA51&dq=android+show+dialog+on+a+transaction+with+backstack&source=bl&ots=UHd3TOFPNy&sig=oqmALWf6P0W1fLo5Fr65ZNFTQCk&hl=en&sa=X&ved=0ahUKEwj30rz6_MjNAhUJnRoKHZAnA3gQ6AEIQTAG#v=onepage&q=android%20show%20dialog%20on%20a%20transaction%20with%20backstack&f=false
                         *
                         * See also http://stackoverflow.com/questions/20733142/android-multiple-nested-dialogfragment
                         * for more information.
                         */
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

                        // When the transaction is popped when the second dialog is dismissed, the transaction will
                        // be done in reverse which also means the removed dialog will be added back.
                        transaction.remove(DuplicateReplacementDialog.this);

                        transaction.addToBackStack(null);
                        wordChooserDialog.show(transaction, "Choose Correct Word");
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
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // this is here for convenience, if we want to disable it we can.
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d("DuplicateDialog", "dismissed");
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
