package com.marklogic.appdeployer.ml7;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.AppDeployer;
import com.marklogic.appdeployer.ManageClient;
import com.marklogic.appdeployer.pkg.DatabasePackageMerger;
import com.marklogic.appdeployer.pkg.HttpServerPackageMerger;
import com.marklogic.clientutil.LoggingObject;
import com.marklogic.xccutil.template.XccTemplate;

public class Ml7AppDeployer extends LoggingObject implements AppDeployer {

    private ManageClient manageClient;

    public Ml7AppDeployer(ManageClient manageClient) {
        this.manageClient = manageClient;
    }

    @Override
    public void installPackages(AppConfig config) {
        String packageName = config.getPackageName();
        manageClient.deletePackage(packageName);
        manageClient.createPackage(packageName);

        installDatabases(config);
        createRestApiServers(config);
        installXdbcServers(config);

        logger.info("Finished installing packages for application: " + config.getName());
    }

    @Override
    public void uninstallApp(AppConfig appConfig) {
        String xquery = loadStringFromClassPath("ml-app-deployer/uninstall-app.xqy");
        xquery = xquery.replace("%%APP_NAME%%", appConfig.getName());
        XccTemplate t = new XccTemplate(appConfig.getXccUrl());
        logger.info("Uninstalling app with name: " + appConfig.getName());
        t.executeAdhocQuery(xquery);
    }

    @Override
    public void mergeDatabasePackages(AppConfig appConfig) {
        List<String> paths = appConfig.getDatabasePackageFilePaths();
        if (paths != null && !paths.isEmpty()) {
            File outputFile = new File(appConfig.getMergedDatabasePackageFilePath());
            if (outputFile.exists()) {
                logger.info("Deleting existing merged database package file: " + outputFile.getAbsolutePath());
                outputFile.delete();
            }

            String xml = new DatabasePackageMerger().mergeDatabasePackages(paths);
            File dir = outputFile.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            try {
                FileCopyUtils.copy(xml, new FileWriter(outputFile));
                appConfig.setContentDatabaseFilePath(appConfig.getMergedDatabasePackageFilePath());
            } catch (IOException ie) {
                throw new RuntimeException(ie);
            }
        }
    }

    @Override
    public void mergeHttpServerPackages(AppConfig appConfig) {
        List<String> paths = appConfig.getHttpServerPackageFilePaths();
        if (paths != null && !paths.isEmpty()) {
            File outputFile = new File(appConfig.getMergedHttpServerPackageFilePath());
            if (outputFile.exists()) {
                logger.info("Deleting existing merged http server package file: " + outputFile.getAbsolutePath());
                outputFile.delete();
            }

            String xml = new HttpServerPackageMerger().mergeHttpServerPackages(paths);
            File dir = outputFile.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            try {
                FileCopyUtils.copy(xml, new FileWriter(outputFile));
                appConfig.setHttpServerFilePath(appConfig.getMergedHttpServerPackageFilePath());
            } catch (IOException ie) {
                throw new RuntimeException(ie);
            }
        }
    }

    @Override
    public void updateContentDatabase(AppConfig appConfig) {
        installContentDatabases(appConfig);
        manageClient.installPackage(appConfig.getPackageName());
    }

    protected void installDatabases(AppConfig appConfig) {
        boolean installPackage = false;
        if (new File(appConfig.getTriggersDatabaseFilePath()).exists()) {
            manageClient.addDatabase(appConfig.getPackageName(), appConfig.getTriggersDatabaseName(),
                    appConfig.getTriggersDatabaseFilePath());
            installPackage = true;
        }

        if (new File(appConfig.getSchemasDatabaseFilePath()).exists()) {
            manageClient.addDatabase(appConfig.getPackageName(), appConfig.getSchemasDatabaseName(),
                    appConfig.getSchemasDatabaseFilePath());
            installPackage = true;
        }

        if (new File(appConfig.getContentDatabaseFilePath()).exists()) {
            installContentDatabases(appConfig);
            installPackage = true;
        }

        if (installPackage) {
            manageClient.installPackage(appConfig.getPackageName());
        }
    }

    protected void installContentDatabases(AppConfig appConfig) {
        mergeDatabasePackages(appConfig);

        manageClient.addDatabase(appConfig.getPackageName(), appConfig.getContentDatabaseName(),
                appConfig.getContentDatabaseFilePath());
        if (appConfig.isTestPortSet()) {
            manageClient.addDatabase(appConfig.getPackageName(), appConfig.getTestContentDatabaseName(),
                    appConfig.getContentDatabaseFilePath());
        }
    }

    protected void createRestApiServers(AppConfig appConfig) {
        manageClient.createRestApiServer(appConfig.getRestServerName(), appConfig.getContentDatabaseName(),
                appConfig.getRestPort(), appConfig.getModulesDatabaseName());

        if (appConfig.isTestPortSet()) {
            manageClient.createRestApiServer(appConfig.getRestServerName(), appConfig.getTestContentDatabaseName(),
                    appConfig.getTestRestPort(), appConfig.getModulesDatabaseName());
        }
    }

    protected void installXdbcServers(AppConfig appConfig) {
        boolean installPackage = false;

        if (appConfig.getXdbcPort() != null && appConfig.getXdbcPort() > 0) {
            addXdbcServer(appConfig, appConfig.getXdbcServerName(), appConfig.getXdbcPort(),
                    appConfig.getContentDatabaseName());
            installPackage = true;
        }

        if (appConfig.isTestPortSet()) {
            addXdbcServer(appConfig, appConfig.getTestXdbcServerName(), appConfig.getTestXdbcPort(),
                    appConfig.getTestContentDatabaseName());
            installPackage = true;
        }

        if (appConfig.getModulesXdbcPort() != null && appConfig.getModulesXdbcPort() > 0) {
            addXdbcServer(appConfig, appConfig.getModulesXdbcServerName(), appConfig.getModulesXdbcPort(),
                    appConfig.getModulesDatabaseName());
            installPackage = true;
        }

        if (installPackage) {
            manageClient.installPackage(appConfig.getPackageName());
        }
    }

    protected void addXdbcServer(AppConfig appConfig, String serverName, Integer serverPort, String databaseName) {
        String xml = null;
        String file = appConfig.getXdbcServerFilePath();
        try {
            if (file != null && new File(file).exists()) {
                xml = FileCopyUtils.copyToString(new FileReader(file));
            } else {
                xml = loadStringFromClassPath("ml-app-deployer/xdbc-server-template.xml");
            }
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
        addServer(appConfig, xml, serverName, serverPort, databaseName);
    }

    protected void addServer(AppConfig appConfig, String xml, String serverName, Integer serverPort, String databaseName) {
        xml = replaceTokensInServerPackageXml(appConfig, xml, serverName, serverPort, databaseName);
        logger.info(String.format("Adding server %s in group %s to package %s", serverName, appConfig.getGroupName(),
                appConfig.getPackageName()));
        manageClient.addServer(appConfig.getPackageName(), serverName, appConfig.getGroupName(), xml);
    }

    protected String replaceTokensInServerPackageXml(AppConfig appConfig, String xml, String serverName,
            Integer serverPort, String databaseName) {
        xml = xml.replace("%%GROUP_NAME%%", appConfig.getGroupName());
        xml = xml.replace("%%SERVER_NAME%%", serverName);
        xml = xml.replace("%%PORT%%", serverPort.toString());
        xml = xml.replace("%%DATABASE_NAME%%", databaseName);
        xml = xml.replace("%%MODULES_DATABASE_NAME%%", appConfig.getName() + "-modules");
        return xml;
    }

    protected String loadStringFromClassPath(String path) {
        try {
            return new String(FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream()));
        } catch (IOException ie) {
            throw new RuntimeException("Unable to load string from classpath resource at: " + path + "; cause: "
                    + ie.getMessage(), ie);
        }
    }
}
