package com.mukera.sheket.client.controller.importer;

import java.util.Map;

/**
 * Created by fuad on 6/26/16.
 */
public interface ImportListener {
    void importSuccessful();
    void importError(String msg);

    void displayDataMappingDialog(SimpleCSVReader reader);

    void displayReplacementDialog(SimpleCSVReader reader,
                                  Map<Integer, Integer> mapping,
                                  DuplicateEntities duplicateEntities);
}
