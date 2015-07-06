package com.marklogic.appdeployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.marklogic.client.DatabaseClientFactory.Authentication;

/**
 * Encapsulates the connection information for an application and where its modules can be found. Should possibly add
 * ConfigDir to this.
 */
public class AppConfig {

    private String name;
    private String host = "localhost";

    // User/password for authenticating against the REST API
    private String username;
    private String password;
    private Authentication authentication = Authentication.DIGEST;

    // User/password for making XDBC calls, usually against port 8000
    private String xdbcUsername;
    private String xdbcPassword;

    private Integer restPort;
    private Integer testRestPort;

    private List<String> modulePaths;
    private ConfigDir configDir;
    private List<ConfigDir> dependencyConfigDirs;

    private String groupName = "Default";

    // Passed into the TokenReplacer that subclasses of AbstractCommand use
    private Map<String, String> customTokens = new HashMap<>();

    // Allows for creating a triggers database without a config file for one
    private boolean createTriggersDatabase = true;

    public AppConfig() {
        this("src/main/ml-modules");
    }

    public AppConfig(String defaultModulePath) {
        modulePaths = new ArrayList<String>();
        modulePaths.add(defaultModulePath);
        configDir = new ConfigDir();

        this.username = "admin";
        this.password = "admin";
        this.xdbcUsername = username;
        this.xdbcPassword = password;
    }

    public boolean isTestPortSet() {
        return testRestPort != null && testRestPort > 0;
    }

    public String getPackageName() {
        return name + "-package";
    }

    public String getRestServerName() {
        return name;
    }

    public String getTestRestServerName() {
        return name + "-test";
    }

    public String getXdbcServerName() {
        return getContentDatabaseName() + "-xdbc";
    }

    public String getTestXdbcServerName() {
        return getTestContentDatabaseName() + "-xdbc";
    }

    public String getModulesXdbcServerName() {
        return name + "-modules-xdbc";
    }

    public String getContentDatabaseName() {
        return name + "-content";
    }

    public String getTestContentDatabaseName() {
        return name + "-test-content";
    }

    public String getModulesDatabaseName() {
        return name + "-modules";
    }

    public String getTriggersDatabaseName() {
        return name + "-triggers";
    }

    public String getSchemasDatabaseName() {
        return name + "-schemas";
    }

    public String getContentXccUrl() {
        return String.format("xcc://%s:%s@%s:8000/%s", username, password, host, getContentDatabaseName());
    }

    public String getTestContentXccUrl() {
        return String.format("xcc://%s:%s@%s:8000/%s", username, password, host, getTestContentDatabaseName());
    }

    public String getModulesXccUrl() {
        return String.format("xcc://%s:%s@%s:8000/%s", username, password, host, getModulesDatabaseName());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRestPort() {
        return restPort;
    }

    public void setRestPort(Integer restPort) {
        this.restPort = restPort;
    }

    public Integer getTestRestPort() {
        return testRestPort;
    }

    public void setTestRestPort(Integer testRestPort) {
        this.testRestPort = testRestPort;
    }

    public List<String> getModulePaths() {
        return modulePaths;
    }

    public void setModulePaths(List<String> modulePaths) {
        this.modulePaths = modulePaths;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public ConfigDir getConfigDir() {
        return configDir;
    }

    public void setConfigDir(ConfigDir configDir) {
        this.configDir = configDir;
    }

    public List<ConfigDir> getDependencyConfigDirs() {
        return dependencyConfigDirs;
    }

    public void setDependencyConfigDirs(List<ConfigDir> dependencyConfigDirs) {
        this.dependencyConfigDirs = dependencyConfigDirs;
    }

    public String getXdbcUsername() {
        return xdbcUsername;
    }

    public void setXdbcUsername(String xdbcUsername) {
        this.xdbcUsername = xdbcUsername;
    }

    public String getXdbcPassword() {
        return xdbcPassword;
    }

    public void setXdbcPassword(String xdbcPassword) {
        this.xdbcPassword = xdbcPassword;
    }

    public Map<String, String> getCustomTokens() {
        return customTokens;
    }

    public void setCustomTokens(Map<String, String> customTokens) {
        this.customTokens = customTokens;
    }

    public boolean isCreateTriggersDatabase() {
        return createTriggersDatabase;
    }

    public void setCreateTriggersDatabase(boolean createTriggerDatabase) {
        this.createTriggersDatabase = createTriggerDatabase;
    }

}
