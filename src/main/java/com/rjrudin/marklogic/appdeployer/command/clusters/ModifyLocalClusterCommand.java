package com.rjrudin.marklogic.appdeployer.command.clusters;

import java.io.File;

import com.rjrudin.marklogic.appdeployer.command.AbstractCommand;
import com.rjrudin.marklogic.appdeployer.command.CommandContext;
import com.rjrudin.marklogic.appdeployer.command.SortOrderConstants;
import com.rjrudin.marklogic.mgmt.clusters.ClusterManager;

/**
 * Looks for a file with a name starting with "local-cluster" (e.g. "local-cluster.json" or "local-cluster.xml") in the
 * "clusters" directory. The cluster endpoints - https://docs.marklogic.com/REST/management/clusters - offer a lot more
 * functionality; this command is just for the https://docs.marklogic.com/REST/PUT/manage/v2/properties endpoint.
 */
public class ModifyLocalClusterCommand extends AbstractCommand {

    public ModifyLocalClusterCommand() {
        setExecuteSortOrder(SortOrderConstants.MODIFY_LOCAL_CLUSTER);
    }

    @Override
    public void execute(CommandContext context) {
        File configDir = context.getAppConfig().getConfigDir().getClustersDir();
        if (configDir != null && configDir.exists()) {
            for (File f : configDir.listFiles()) {
                if (f.isFile() && f.getName().startsWith("local-cluster")) {
                    String payload = copyFileToString(f);
                    logger.info("Payload: " + payload);
                    payload = tokenReplacer.replaceTokens(payload, context.getAppConfig(), false);
                    logger.info("Payload: " + payload);
                    new ClusterManager(context.getManageClient()).modifyLocalCluster(payload);
                }
            }
        }
    }

}
