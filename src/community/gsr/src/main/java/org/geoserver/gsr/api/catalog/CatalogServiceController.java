/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.catalog;

import static org.geoserver.gsr.GSRConfig.CURRENT_VERSION;
import static org.geoserver.gsr.GSRConfig.PRODUCT_NAME;
import static org.geoserver.gsr.GSRConfig.SPEC_VERSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.GSRServiceInfo;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.model.AbstractGSRModel.Link;
import org.geoserver.gsr.model.service.AbstractService;
import org.geoserver.gsr.model.service.CatalogService;
import org.geoserver.gsr.model.service.FeatureService;
import org.geoserver.gsr.model.service.GeometryService;
import org.geoserver.gsr.model.service.MapService;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.wfs.json.JSONType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the root Catalog service endpoint. */
@APIService(
        service = "GSR",
        version = "10.51",
        landingPage = "gsr/rest/services",
        core = true,
        serviceClass = GSRServiceInfo.class)
@RestController
@RequestMapping(
        path = "/gsr/rest/services",
        produces = {MediaType.APPLICATION_JSON_VALUE, JSONType.jsonp})
public class CatalogServiceController extends AbstractGSRController {

    @Autowired
    public CatalogServiceController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(
            path = {""},
            name = "GetWorkspaceFolders")
    @HTMLResponseBody(templateName = "catalog.ftl", fileName = "catalog.html")
    public CatalogService catalogGet() {
        List<AbstractService> services = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        for (WorkspaceInfo ws : catalog.getWorkspaces()) {
            folders.add(ws.getName());
        }
        CatalogService catalog =
                new CatalogService(
                        "/", SPEC_VERSION, PRODUCT_NAME, CURRENT_VERSION, folders, services);
        catalog.getInterfaces().add(new Link("?f=json&pretty=true", "REST"));
        return catalog;
    }

    @GetMapping(
            path = {"/{workspaceName:.*}"},
            name = "GetLayerFolders")
    @HTMLResponseBody(templateName = "catalog.ftl", fileName = "catalog.html")
    public CatalogService catalogGet(@PathVariable(required = true) String workspaceName) {
        List<String> folders = new ArrayList<>();
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new APIException(
                    "InvalidWorkspaceName",
                    workspaceName + " does not correspond to any workspaces.",
                    HttpStatus.NOT_FOUND);
        }
        folders =
                catalog.getLayers()
                        .parallelStream()
                        .filter(
                                l ->
                                        workspaceName.equals(
                                                l.getResource()
                                                        .getStore()
                                                        .getWorkspace()
                                                        .getName()))
                        .map(l -> workspaceName + "/" + l.getName())
                        .collect(Collectors.toList());

        CatalogService catalog =
                new CatalogService(
                        workspaceName,
                        SPEC_VERSION,
                        PRODUCT_NAME,
                        CURRENT_VERSION,
                        folders,
                        Collections.emptyList());
        catalog.getPath().add(new Link(workspaceName, workspaceName));
        catalog.getInterfaces().add(new Link(workspaceName + "?f=json&pretty=true", "REST"));
        return catalog;
    }

    @GetMapping(
            path = {"{workspaceName:.*}/{layerName}"},
            name = "GetServices")
    @HTMLResponseBody(templateName = "catalog.ftl", fileName = "catalog.html")
    public CatalogService catalogServiceGet(
            @PathVariable String workspaceName, @PathVariable String layerName) {
        List<AbstractService> services = new ArrayList<>();
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new APIException(
                    "InvalidWorkspaceName",
                    workspaceName + " does not correspond to any workspaces.",
                    HttpStatus.NOT_FOUND);
        }
        LayerInfo li = catalog.getLayerByName(layerName);
        if (li != null && li.getResource().getStore().getWorkspace().equals(ws)) {
            fillServices(services, li, workspaceName);
        } else {
            throw new APIException(
                    "InvalidLayerName",
                    layerName + " does not correspond to a layer in the workspace.",
                    HttpStatus.NOT_FOUND);
        }

        if (!GeometryService.isGeometryServiceDisabled()) {
            services.add(new GeometryService("Geometry"));
        }
        CatalogService catalog =
                new CatalogService(
                        layerName,
                        SPEC_VERSION,
                        PRODUCT_NAME,
                        CURRENT_VERSION,
                        Collections.emptyList(),
                        services);
        catalog.getPath().add(new Link(workspaceName, workspaceName));
        catalog.getPath().add(new Link(workspaceName + "/" + layerName, layerName));
        catalog.getInterfaces()
                .add(new Link(workspaceName + "/" + layerName + "?f=json&pretty=true", "REST"));
        return catalog;
    }

    private void fillServices(List<AbstractService> services, LayerInfo li, String workspaceName) {
        MapService ms = new MapService(workspaceName + "/" + li.getName());
        FeatureService fs = new FeatureService(workspaceName + "/" + li.getName());
        services.add(ms);
        services.add(fs);
    }
}
