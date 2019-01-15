package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "finalpower-mobilehub-509467640-FinalPowerTable")

public class FinalPowerTableDO {
    private String _userId;
    private String _time;
    private String _power;
    private String _temperature;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "time")
    @DynamoDBAttribute(attributeName = "time")
    public String getTime() {
        return _time;
    }

    public void setTime(final String _time) {
        this._time = _time;
    }
    @DynamoDBAttribute(attributeName = "power")
    public String getPower() {
        return _power;
    }

    public void setPower(final String _power) {
        this._power = _power;
    }
    @DynamoDBAttribute(attributeName = "temperature")
    public String getTemperature() {
        return _temperature;
    }

    public void setTemperature(final String _temperature) {
        this._temperature = _temperature;
    }

}
