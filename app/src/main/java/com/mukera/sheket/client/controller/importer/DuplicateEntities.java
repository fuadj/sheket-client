package com.mukera.sheket.client.controller.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Fuad on 6/26/16.
 *
 * It holds the possible duplicate values for entities during
 * importing AND ALSO the replacement the user has selected for them.
 */
public class DuplicateEntities {
    /**
     * These are the possible duplicates found by parsing the data.
     */
    public Vector<Vector<String>> categoryDuplicates;
    public Vector<Vector<String>> branchDuplicates;

    /**
     * These are filled in after user selects which are duplicates and which are correct.
     * It maps a duplicate word to the correct word. It is this final value that
     * will be used while importing to figure out the correct name for an entity.
     */
    public Map<String, String> categoryReplacement;
    public Map<String, String> branchReplacement;

    public DuplicateEntities() {
        branchDuplicates = new Vector<>();
        categoryDuplicates = new Vector<>();

        branchReplacement = new HashMap<>();
        categoryReplacement = new HashMap<>();
    }
}
