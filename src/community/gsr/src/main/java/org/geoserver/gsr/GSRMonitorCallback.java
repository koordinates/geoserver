/** */
package org.geoserver.gsr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/** */
public class GSRMonitorCallback implements DispatcherCallback, ExtensionPriority {

    static final Logger LOGGER = Logging.getLogger(GSRMonitorCallback.class);

    Monitor monitor;
    Catalog catalog;

    public GSRMonitorCallback(Monitor monitor, Catalog catalog) {
        this.monitor = monitor;
        this.catalog = catalog;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        // TODO Auto-generated method stub
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        RequestData data = monitor.current();
        if (data == null) {
            // will happen in cases where the filter is not active
            return operation;
        }
        Service service = operation.getService();
        if (service != null && service.getId().equals("GSR")) {
            // tweak some GSR-specific things
            data.setOwsVersion(Double.toString(GSRConfig.CURRENT_VERSION));

            List<String> params =
                    Arrays.stream(operation.getParameters())
                            .map(o -> o != null ? o.toString() : null)
                            .collect(Collectors.toList());

            // TODO: implement different logic based on the operation
            //            switch(operation.getId()) {
            //            case "GetServices":
            //            case "FeatureServerGetService":
            //            case "FeatureServerGetFeature":
            //            case "FeatureServerGetLayers":
            //            case "FeatureServerGetLegend":
            //            case "FeatureServerQuery":
            //            case "FeatureServerAddFeatures":
            //            case "FeatureServerApplyEdits":
            //            case "FeatureServerDeleteFeatures":
            //            case "FeatureServerUpdateFeatures":
            //            case "MapServerGetService":
            //            case "MapServerGetLayers":
            //            case "MapServerGetLayer":
            //            case "MapServerExportLayerMap":
            //            case "MapServerExportMap":
            //            case "MapServerExportMapImage":
            //            case "MapServerFind":
            //            case "MapServerGetLegend":
            //            case "MapServerIdentify":
            //            case "MapServerQuery":
            //            }
            try {
                // TODO: WFS/etc does this pre-query like we do here, couldn't we do it during
                // the query when we figure out what's actually being asked for?
                // big assumption we can resolve /{index}/ later...
                data.setResources(Collections.singletonList(String.join(":", params)));
            } catch (Exception e) {
                LOGGER.warning("Error getting resources for auditlog from GSR request");
            }

            if (monitor.getConfig().getBboxMode() == MonitorConfig.BboxMode.FULL) {
                // TODO: which operations take a bounding box or spatial filter?
            }

            monitor.update();
        }
        return operation;
    }

    @Override
    public Request init(Request request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void finished(Request request) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getPriority() {
        return LOWEST;
    }
}
