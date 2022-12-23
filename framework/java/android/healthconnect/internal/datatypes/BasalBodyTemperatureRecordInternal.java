/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.healthconnect.internal.datatypes;

import android.annotation.NonNull;
import android.healthconnect.datatypes.BasalBodyTemperatureRecord;
import android.healthconnect.datatypes.BodyTemperatureMeasurementLocation;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.units.Temperature;
import android.os.Parcel;

/**
 * @see BasalBodyTemperatureRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE)
public final class BasalBodyTemperatureRecordInternal
        extends InstantRecordInternal<BasalBodyTemperatureRecord> {
    private int mMeasurementLocation;
    private double mTemperature;

    @BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }

    /** returns this object with the specified measurementLocation */
    @NonNull
    public BasalBodyTemperatureRecordInternal setMeasurementLocation(int measurementLocation) {
        this.mMeasurementLocation = measurementLocation;
        return this;
    }

    public double getTemperature() {
        return mTemperature;
    }

    /** returns this object with the specified temperature */
    @NonNull
    public BasalBodyTemperatureRecordInternal setTemperature(double temperature) {
        this.mTemperature = temperature;
        return this;
    }

    @NonNull
    @Override
    public BasalBodyTemperatureRecord toExternalRecord() {
        return new BasalBodyTemperatureRecord.Builder(
                        buildMetaData(),
                        getTime(),
                        getMeasurementLocation(),
                        Temperature.fromCelsius(getTemperature()))
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mMeasurementLocation = parcel.readInt();
        mTemperature = parcel.readDouble();
    }

    @Override
    void populateInstantRecordFrom(@NonNull BasalBodyTemperatureRecord basalBodyTemperatureRecord) {
        mMeasurementLocation = basalBodyTemperatureRecord.getMeasurementLocation();
        mTemperature = basalBodyTemperatureRecord.getTemperature().getInCelsius();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mMeasurementLocation);
        parcel.writeDouble(mTemperature);
    }
}
