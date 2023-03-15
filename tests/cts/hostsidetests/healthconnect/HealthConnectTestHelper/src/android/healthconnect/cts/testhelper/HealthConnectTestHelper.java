/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.healthconnect.cts.testhelper;

import static android.healthconnect.cts.lib.TestUtils.DELETE_RECORDS_QUERY;
import static android.healthconnect.cts.lib.TestUtils.INSERT_RECORD_QUERY;
import static android.healthconnect.cts.lib.TestUtils.INTENT_EXCEPTION;
import static android.healthconnect.cts.lib.TestUtils.QUERY_TYPE;
import static android.healthconnect.cts.lib.TestUtils.RECORD_IDS;
import static android.healthconnect.cts.lib.TestUtils.SUCCESS;
import static android.healthconnect.cts.lib.TestUtils.UPDATE_RECORDS_QUERY;
import static android.healthconnect.cts.lib.TestUtils.getTestRecords;
import static android.healthconnect.cts.lib.TestUtils.insertRecordsAndGetIds;
import static android.healthconnect.cts.lib.TestUtils.readRecords;
import static android.healthconnect.cts.lib.TestUtils.updateRecords;
import static android.healthconnect.cts.lib.TestUtils.verifyDeleteRecords;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.datatypes.Record;
import android.healthconnect.cts.lib.TestUtils;
import android.os.Bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HealthConnectTestHelper extends Activity {
    private static final String TAG = "HealthConnectTestHelper";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        Bundle bundle = getIntent().getExtras();
        String queryType = bundle.getString(QUERY_TYPE);
        Intent returnIntent;
        try {
            switch (queryType) {
                case INSERT_RECORD_QUERY:
                    returnIntent = insertRecord(queryType, context);
                    break;
                case DELETE_RECORDS_QUERY:
                    returnIntent =
                            deleteRecords(
                                    queryType,
                                    (List<TestUtils.RecordTypeAndRecordIds>)
                                            bundle.getSerializable(RECORD_IDS),
                                    context);
                    break;
                case UPDATE_RECORDS_QUERY:
                    returnIntent =
                            updateRecordsAs(
                                    queryType,
                                    (List<TestUtils.RecordTypeAndRecordIds>)
                                            bundle.getSerializable(RECORD_IDS),
                                    context);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unknown query received from launcher app: " + queryType);
            }
        } catch (Exception e) {
            returnIntent = new Intent(queryType);
            returnIntent.putExtra(INTENT_EXCEPTION, e);
        }

        sendBroadcast(returnIntent);
        this.finish();
    }

    /**
     * Method to get test records, insert them, and put the list of recordId and recordClass in the
     * intent
     *
     * @param queryType - specifies the action, here it should be INSERT_RECORDS_QUERY
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private Intent insertRecord(String queryType, Context context) {
        List<Record> records = getTestRecords(context.getPackageName());
        final Intent intent = new Intent(queryType);
        try {
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                    insertRecordsAndGetIds(records, context);
            intent.putExtra(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);
            intent.putExtra(SUCCESS, true);
        } catch (InterruptedException e) {
            intent.putExtra(SUCCESS, false);
        }

        return intent;
    }

    /**
     * Method to delete the records and put the Exception in the intent if deleting records throws
     * an exception
     *
     * @param queryType - specifies the action, here it should be DELETE_RECORDS_QUERY
     * @param listOfRecordIdsAndClassName - list of recordId and recordClass of records to be
     *     deleted
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     * @throws ClassNotFoundException if a record category class is not found for any class name
     *     present in the list @listOfRecordIdsAndClassName
     */
    private Intent deleteRecords(
            String queryType,
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClassName,
            Context context)
            throws ClassNotFoundException {
        final Intent intent = new Intent(queryType);

        List<RecordIdFilter> recordIdFilters = new ArrayList<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds :
                listOfRecordIdsAndClassName) {
            for (String recordId : recordTypeAndRecordIds.getRecordIds()) {
                recordIdFilters.add(
                        RecordIdFilter.fromId(
                                (Class<? extends Record>)
                                        Class.forName(recordTypeAndRecordIds.getRecordType()),
                                recordId));
            }
        }
        try {
            verifyDeleteRecords(recordIdFilters, context);
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
        }
        return intent;
    }

    /**
     * Method to update the records and put the exception in the intent if updating the records
     * throws an exception
     *
     * @param queryType - specifies the action, here it should be UPDATE_RECORDS_QUERY
     * @param listOfRecordIdsAndClassName - list of recordId and recordClass of records to be
     *     updated
     * @param context - application context
     * @return Intent to send back to the main app which is running the tests
     */
    private Intent updateRecordsAs(
            String queryType,
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClassName,
            Context context) {
        final Intent intent = new Intent(queryType);

        try {
            for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds :
                    listOfRecordIdsAndClassName) {
                List<? extends Record> recordsToBeUpdated =
                        readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(
                                                (Class<? extends Record>)
                                                        Class.forName(
                                                                recordTypeAndRecordIds
                                                                        .getRecordType()))
                                        .build(),
                                context);
                updateRecords((List<Record>) recordsToBeUpdated, context);
            }
        } catch (Exception e) {
            intent.putExtra(INTENT_EXCEPTION, e);
        }

        return intent;
    }
}
