package com.marklogic.appdeployer.plugin.modules;

import java.io.File;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.AppPluginContext;
import com.marklogic.appdeployer.plugin.AbstractPlugin;
import com.marklogic.appdeployer.plugin.SortOrderConstants;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.clientutil.modulesloader.ModulesLoader;
import com.marklogic.clientutil.modulesloader.impl.DefaultModulesLoader;

public class LoadModulesPlugin extends AbstractPlugin {

    private ModulesLoader modulesLoader;

    @Override
    public Integer getSortOrderOnDeploy() {
        return SortOrderConstants.LOAD_MODULES_ORDER;
    }

    @Override
    public void onDeploy(AppPluginContext context) {
        if (modulesLoader == null) {
            modulesLoader = new DefaultModulesLoader();
        }
        AppConfig config = context.getAppConfig();
        DatabaseClient client = DatabaseClientFactory.newClient(config.getHost(), config.getRestPort(),
                config.getUsername(), config.getPassword(), config.getAuthentication());
        for (String modulesPath : config.getModulePaths()) {
            logger.info("Loading modules from dir: " + modulesPath);
            modulesLoader.loadModules(new File(modulesPath), client);
        }
    }

    @Override
    public void onUndeploy(AppPluginContext context) {
    }

    public void setModulesLoader(ModulesLoader modulesLoader) {
        this.modulesLoader = modulesLoader;
    }
}