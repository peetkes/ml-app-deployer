package com.rjrudin.marklogic.appdeployer.command.clusters;

import java.io.File;

import org.junit.Test;

import com.rjrudin.marklogic.appdeployer.AbstractAppDeployerTest;

public class ModifyLocalClusterTest extends AbstractAppDeployerTest {

    /**
     * I filed bug 36547, as ssl-fips-enabled is not available in 8.0-4.
     */
    @Test
    public void test() {
        appConfig.getConfigDir().setBaseDir(new File("src/test/resources/sample-app/cluster-test"));

        initializeAppDeployer(new ModifyLocalClusterCommand());
        appDeployer.deploy(appConfig);
    }
}
