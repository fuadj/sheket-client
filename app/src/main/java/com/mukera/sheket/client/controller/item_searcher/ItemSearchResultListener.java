package com.mukera.sheket.client.controller.item_searcher;

import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/5/16.
 */
public interface ItemSearchResultListener {
    void itemSelected(SItem item);

    void finishTransaction();
    void cancelTransaction();
}
