/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.Map.Entry;
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;


public class StorageHelper
{
    
    
    public static SensorStorageHelper configureStorageForSensor(ISensorModule<?> sensor, IBasicStorage<?> storage, boolean createListener) throws SensorHubException
    {
        if (storage.getDataStores().size() > 0)
            throw new RuntimeException("Storage " + MsgUtils.moduleString(storage) + " is already in use");
        
        // copy sensor description history
        if (sensor.isSensorDescriptionHistorySupported())
        {
            for (AbstractProcess sensorDesc: sensor.getSensorDescriptionHistory())
                storage.storeDataSourceDescription(sensorDesc);
        }
        else
        {
            storage.storeDataSourceDescription(sensor.getCurrentSensorDescription());
        }
        
        // create one data store for each sensor output
        for (Entry<String, ? extends ISensorDataInterface> item: sensor.getAllOutputs().entrySet())
        {
            String name = item.getKey();
            ISensorDataInterface output = item.getValue();
            storage.addNewDataStore(name, output.getRecordDescription(), output.getRecommendedEncoding());
        }
        
        if (createListener)
        {
            // start storage helper for adding data on data events
            SensorStorageHelperConfig helperConfig = new SensorStorageHelperConfig();
            helperConfig.name = "Storage Listener for " + sensor.getName();
            helperConfig.enabled = true;
            helperConfig.sensorID = sensor.getLocalID();
            helperConfig.storageID = storage.getLocalID();
            IModule<?> m = SensorHub.getInstance().getModuleRegistry().loadModule(helperConfig);
            return (SensorStorageHelper)m;
        }
        
        return null;
    }
}