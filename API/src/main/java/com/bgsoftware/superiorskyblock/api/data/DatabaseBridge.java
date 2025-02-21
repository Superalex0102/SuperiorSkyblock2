package com.bgsoftware.superiorskyblock.api.data;

import com.bgsoftware.superiorskyblock.api.objects.Pair;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

public interface DatabaseBridge {

    /**
     * Load all the objects from the database.
     *
     * @param table:         The table to load the objects from.
     * @param resultConsumer Consumer that receives each object from the database.
     */
    void loadAllObjects(String table, Consumer<Map<String, Object>> resultConsumer);

    /**
     * Start saving data for the object.
     * If this method is not called, data should not be saved when saveRow is called.
     */
    void startSavingData();

    /**
     * Set whether to execute operations in batches or not.
     */
    void batchOperations(boolean batchOperations);

    /**
     * Update the object in the database.
     *
     * @param table   The name of the table in the database.
     * @param filter  The filter of the column.
     * @param columns All columns to be saved with their values.
     */
    void updateObject(String table, @Nullable DatabaseFilter filter, Pair<String, Object>... columns);

    /**
     * Insert the object in the database.
     *
     * @param table   The name of the table in the database.
     * @param columns All columns to be saved with their values.
     */
    void insertObject(String table, Pair<String, Object>... columns);

    /**
     * Delete the object from the database.
     *
     * @param table  The name of the table in the database.
     * @param filter The filter of the column.
     */
    void deleteObject(String table, @Nullable DatabaseFilter filter);

    /**
     * Load data from the database for this object.
     *
     * @param table          The table to get the data from.
     * @param filter         The filter of the column.
     * @param resultConsumer Consumer that receives data for each row.
     */
    void loadObject(String table, @Nullable DatabaseFilter filter, Consumer<Map<String, Object>> resultConsumer);

}
