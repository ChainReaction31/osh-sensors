/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Generic driver implementation for RTP/RTSP cameras.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class RTPCameraDriver extends AbstractSensorModule<RTPCameraConfig>
{
    static final Logger log = LoggerFactory.getLogger(RTPCameraDriver.class);
    RTPVideoOutput dataInterface;
    
    
    public RTPCameraDriver()
    {        
    }
    
    
    @Override
    public void init(RTPCameraConfig config) throws SensorHubException
    {
        super.init(config);        
        this.dataInterface = new RTPVideoOutput(this);
        this.dataInterface.init();
        addOutput(dataInterface, false);
    }
    
    
    @Override
    public synchronized void start() throws SensorException
    {
        dataInterface.start();
    }
    
    
    @Override
    public synchronized void stop()
    {
        dataInterface.stop();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            if (AbstractSensorModule.DEFAULT_ID.equals(sensorDescription.getId()))
                sensorDescription.setId("RTP_CAMERA");
            
            if (config.cameraID != null)
                sensorDescription.setUniqueIdentifier("urn:osh:sensor:rtpcam:" + config.cameraID);
        }
    }


    @Override
    public boolean isConnected()
    {
        if (dataInterface != null)
            return dataInterface.firstFrameReceived;
        
        return false;
    }
    

    @Override
    public void cleanup()
    {        
    }
    
    
    @Override
    public void finalize()
    {
        stop();
    }
}