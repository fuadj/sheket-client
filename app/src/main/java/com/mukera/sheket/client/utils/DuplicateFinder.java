package com.mukera.sheket.client.utils;

import android.util.Log;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Created by fuad on 6/24/16.
 */
public class DuplicateFinder {
    public static int computeDistance(String a, String b) {
        int len0 = a.length() + 1;
        int len1 = b.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {
                // matching current letters in both strings
                int match = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    public static int computeDistanceIgnoringSpaces(String a, String b) {
        a = a.toLowerCase().trim();
        b = b.toLowerCase().trim();

        // we don't want any spaces
        String[] a_split = a.split("\\s+");
        String[] b_split = b.split("\\s+");

        if (a_split.length == 0 ||
                a_split.length != b_split.length)
            return computeDistance(a, b);

        int sum = 0;
        for (int i = 0; i < a_split.length; i++) {
            sum += computeDistance(a_split[i], b_split[i]);
        }
        return sum;
    }

    public interface WordComparator {
        /**
         * Compares the two words to determine if they are duplicates.
         * @param distance The distance between the left and right words.
         *                 This is computed on a trimmed and lowered version of
         *                 the words. It also ignores any white spaces within the words.                 the .
         * @param lower_left    The lowered and trimmed version of the left word
         * @param lower_right   The lowered and trimmed version fo the right word
         * @param left  The original left word
         * @param right The original right word
         *
         * @return  True if the words are duplicates.
         */
        boolean areDuplicates(int distance, String lower_left, String lower_right,
                              String left, String right);
    }

    public static WordComparator DISTANCE_1_COMPARATOR = new WordComparator() {
        @Override
        public boolean areDuplicates(int distance, String lower_left, String lower_right, String left, String right) {
            return distance <= 1;
        }
    };

    public static WordComparator DISTANCE_2_COMPARATOR = new WordComparator() {
        @Override
        public boolean areDuplicates(int distance, String lower_left, String lower_right, String left, String right) {
            return distance <= 2;
        }
    };

    public static WordComparator DISTANCE_3_COMPARATOR = new WordComparator() {
        @Override
        public boolean areDuplicates(int distance, String lower_left, String lower_right, String left, String right) {
            return distance <= 3;
        }
    };

    /**
     * Given a set of words, finds possible duplicates of words and groups them.
     * Each word only appears once in the result, either by itself or in a group
     * of possible duplicates.
     * @param words
     * @return
     */
    public static Vector<Vector<String>> findDuplicates(Set<String> words,
                                                        WordComparator wc) {
        Vector<Vector<String>> result = new Vector<>();
        if (words.isEmpty()) return result;
        // this is an edge case, better here than complicate the rest of the code
        if (words.size() == 1) {
            result.add(new Vector<>(words));
            return result;
        }

        // We need to convert all the words to the same case to compare them.
        Vector<Pair<String, String>> lower_trimmed = new Vector<>(words.size());
        for (String word : words) {
            lower_trimmed.add(new Pair<>(word, word.trim().toLowerCase()));
        }

        /**
         * We want a word to appear in the result list only once. This creates
         * a problem where a word has multiple relations with other words in the
         * given word list. To prevent this, a word is only considered for comparison
         * if it hasn't already got a duplicate. This insures a word exists in the
         * result list only once.
         */
        Set<String> hasDuplicates = new HashSet<>();

        // we don't check the n-1'th word in the loop because
        // there isn't anything after it
        for (int i = 0; i < (words.size() - 1); i++) {
            String left = lower_trimmed.get(i).second;

            if (hasDuplicates.contains(left)) continue;

            String left_original = lower_trimmed.get(i).first;
            Vector<String> word_duplicates = new Vector<>();


            for (int j = i+1; j < words.size(); j++) {
                String right = lower_trimmed.get(j).second;
                String right_original = lower_trimmed.get(j).first;

                if (hasDuplicates.contains(right)) continue;

                int distance = computeDistanceIgnoringSpaces(left, right);
                if (wc.areDuplicates(distance, left, right,
                        left_original, right_original)) {
                    // we are adding the original word, not the trimmed and lower'ed version
                    word_duplicates.add(right_original);

                    hasDuplicates.add(left);
                    hasDuplicates.add(right);
                }
            }

            word_duplicates.add(left_original);
            result.add(word_duplicates);
        }
        return result;
    }

}
