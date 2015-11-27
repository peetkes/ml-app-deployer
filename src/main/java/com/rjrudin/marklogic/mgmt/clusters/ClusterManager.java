package com.rjrudin.marklogic.mgmt.clusters;

import com.rjrudin.marklogic.mgmt.AbstractManager;
import com.rjrudin.marklogic.mgmt.ManageClient;

public class ClusterManager extends AbstractManager {

    private ManageClient client;

    public ClusterManager(ManageClient client) {
        this.client = client;
    }

    public void modifyLocalCluster(String payload) {
        putPayload(client, "/manage/v2/properties", payload);
    }
}
