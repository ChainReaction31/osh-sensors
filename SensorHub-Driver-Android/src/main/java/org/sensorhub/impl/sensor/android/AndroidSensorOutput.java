/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import java.util.List;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * <p><b>Title:</b>
 * AndroidSensorOutput
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Implementation of data interface for Android sensors
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
public class AndroidSensorOutput implements ISensorDataInterface, SensorEventListener
{
    SensorManager aSensorManager;
    Sensor aSensor;
    
    
    protected AndroidSensorOutput(SensorManager aSensorManager, Sensor aSensor)
    {
        this.aSensorManager = aSensorManager;
        this.aSensor = aSensor;
    }
    
    
    protected void init()
    {
        aSensorManager.registerListener(this, aSensor, 10);
    }
    
    
    @Override
    public boolean isStorageSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isPushSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public double getAverageSamplingRate()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int arg1)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void onSensorChanged(SensorEvent e)
    {
        // TODO Auto-generated method stub        
    }
}