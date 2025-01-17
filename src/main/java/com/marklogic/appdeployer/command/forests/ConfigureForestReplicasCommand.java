/*
 * Copyright (c) 2023 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.appdeployer.command.forests;

import com.marklogic.appdeployer.command.AbstractUndoableCommand;
import com.marklogic.appdeployer.command.CommandContext;
import com.marklogic.appdeployer.command.SortOrderConstants;
import com.marklogic.mgmt.api.API;
import com.marklogic.mgmt.api.forest.Forest;
import com.marklogic.mgmt.mapper.DefaultResourceMapper;
import com.marklogic.mgmt.mapper.ResourceMapper;
import com.marklogic.mgmt.resource.databases.DatabaseManager;
import com.marklogic.mgmt.resource.forests.ForestManager;
import com.marklogic.mgmt.resource.forests.ForestStatus;
import com.marklogic.mgmt.resource.groups.GroupManager;
import com.marklogic.mgmt.resource.hosts.HostManager;

import java.util.*;

/**
 * Command for configuring - i.e. creating and setting - replica forests for existing databases.
 * <p>
 * Very useful for the out-of-the-box forests such as Security, Schemas, App-Services, and Meters, which normally need
 * replicas for failover in a cluster.
 */
public class ConfigureForestReplicasCommand extends AbstractUndoableCommand {

	private Map<String, Integer> databaseNamesAndReplicaCounts = new HashMap<>();
	private boolean deleteReplicasOnUndo = true;
	private GroupHostNamesProvider groupHostNamesProvider;

	public ConfigureForestReplicasCommand() {
		setExecuteSortOrder(SortOrderConstants.DEPLOY_FOREST_REPLICAS);
		setUndoSortOrder(SortOrderConstants.DELETE_FOREST_REPLICAS);
	}

	@Override
	public void execute(CommandContext context) {
		if (context.getAppConfig().getDatabaseNamesAndReplicaCounts() != null) {
			this.databaseNamesAndReplicaCounts = context.getAppConfig().getDatabaseNamesAndReplicaCounts();
		}

		if (databaseNamesAndReplicaCounts == null || databaseNamesAndReplicaCounts.isEmpty()) {
			logger.info("No database names and replica counts defined, so not configuring any forest replicas");
			return;
		}

		List<String> hostNames = new HostManager(context.getManageClient()).getHostNames();
		if (hostNames.size() < 2) {
			if (logger.isInfoEnabled()) {
				logger.info("Only found one host, so not configuring any replica forests; host: " + hostNames.get(0));
			}
			return;
		}

		for (String databaseName : databaseNamesAndReplicaCounts.keySet()) {
			int replicaCount = databaseNamesAndReplicaCounts.get(databaseName);
			if (replicaCount > 0) {
				configureDatabaseReplicaForests(databaseName, replicaCount, hostNames, context);
			}
		}
	}

	@Override
	public void undo(CommandContext context) {
		if (deleteReplicasOnUndo) {
			if (context.getAppConfig().getDatabaseNamesAndReplicaCounts() != null) {
				setDatabaseNamesAndReplicaCounts(context.getAppConfig().getDatabaseNamesAndReplicaCounts());
			}

			DatabaseManager dbMgr = new DatabaseManager(context.getManageClient());
			ForestManager forestMgr = new ForestManager(context.getManageClient());

			for (String databaseName : databaseNamesAndReplicaCounts.keySet()) {
				logger.info(format("Deleting forest replicas for database %s", databaseName));
				if (!dbMgr.exists(databaseName)) {
					logger.warn(format("Database %s does not exist, so not able to delete forest replica for it; perhaps a previous command deleted the database?", databaseName));
				} else {
					List<String> forestNames = dbMgr.getForestNames(databaseName);
					for (String forestName : forestNames) {
						deleteReplicas(forestName, forestMgr);
					}
					logger.info(format("Finished deleting forest replicas for database %s", databaseName));
				}
			}
		} else {
			logger.info("deleteReplicasOnUndo is set to false, so not deleting any replicas");
		}
	}

	/**
	 * @param forestName
	 * @param forestMgr
	 */
	protected void deleteReplicas(String forestName, ForestManager forestMgr) {
		if (forestMgr.exists(forestName)) {
			ForestStatus status = forestMgr.getForestStatus(forestName);
			if (status.isPrimary() && status.hasReplicas()) {
				logger.info(format("Deleting forest replicas for primary forest %s", forestName));
				forestMgr.deleteReplicas(forestName);
				logger.info(format("Finished deleting forest replicas for primary forest %s", forestName));
			}
		}
	}

	/**
	 * @param databaseName
	 * @param replicaCount
	 * @param hostNames
	 * @param context
	 */
	protected void configureDatabaseReplicaForests(String databaseName, int replicaCount, List<String> hostNames, CommandContext context) {
		List<Forest> forestsNeedingReplicas = determineForestsNeedingReplicas(databaseName, context);

		ForestBuilder forestBuilder = new ForestBuilder();
		List<String> selectedHostNames = getHostNamesForDatabaseForests(databaseName, hostNames, context);
		ForestPlan forestPlan = new ForestPlan(databaseName, selectedHostNames).withReplicaCount(replicaCount);
		List<String> dataDirectories = forestBuilder.determineDataDirectories(databaseName, context.getAppConfig());
		forestBuilder.addReplicasToForests(forestsNeedingReplicas, forestPlan, context.getAppConfig(), dataDirectories);

		// TODO Use CMA here in the future? Need to test to see if a forest name + replicas are allowable
		ForestManager forestManager = new ForestManager(context.getManageClient());
		for (Forest forest : forestsNeedingReplicas) {
			final String forestName = forest.getForestName();

			Forest forestWithOnlyReplicas = new Forest();
			forestWithOnlyReplicas.setForestReplica(forest.getForestReplica());
			String json = forestWithOnlyReplicas.getJson();

			logger.info(format("Creating forest replicas for primary forest %s", forestName));
			context.getManageClient().putJson(forestManager.getPropertiesPath(forestName), json);
			logger.info(format("Finished creating forest replicas for primary forest %s", forestName));
		}
	}

	/**
	 * Per #389, the list of replicas needs to be calculated for all forests at once so that ForestBuilder produces the
	 * correct results.
	 *
	 * @param databaseName
	 * @param context
	 * @return
	 */
	protected List<Forest> determineForestsNeedingReplicas(String databaseName, CommandContext context) {
		ForestManager forestManager = new ForestManager(context.getManageClient());
		DatabaseManager dbMgr = new DatabaseManager(context.getManageClient());
		API api = new API(context.getManageClient());
		ResourceMapper resourceMapper = new DefaultResourceMapper(api);

		List<Forest> forestsNeedingReplicas = new ArrayList<>();

		for (String forestName : dbMgr.getForestNames(databaseName)) {
			logger.info(format("Checking the status of forest %s to determine if it is a primary forest and whether or not it has replicas already.", forestName));
			ForestStatus status = forestManager.getForestStatus(forestName);
			if (!status.isPrimary()) {
				logger.info(format("Forest %s is not a primary forest, so not configuring replica forests", forestName));
				continue;
			}
			if (status.hasReplicas()) {
				logger.info(format("Forest %s already has replicas, so not configuring replica forests", forestName));
				continue;
			}

			String forestJson = forestManager.getPropertiesAsJson(forestName);
			Forest forest = resourceMapper.readResource(forestJson, Forest.class);
			forestsNeedingReplicas.add(forest);
		}

		return forestsNeedingReplicas;
	}

	/**
	 * If databaseHosts has been populated on the AppConfig object inside the CommandContext, and there's an entry for
	 * the given database name, then this will only return the hosts that have been set for the given database name.
	 * Otherwise, all hosts are returned.
	 *
	 * @param databaseName
	 * @param hostNames
	 * @param context
	 * @return
	 */
	protected List<String> getHostNamesForDatabaseForests(String databaseName, List<String> hostNames, CommandContext context) {
		List<String> selectedHostNames = new ArrayList<>();

		Map<String, List<String>> databaseGroupMap = context.getAppConfig().getDatabaseGroups();
		List<String> databaseGroups = databaseGroupMap != null ? databaseGroupMap.get(databaseName) : null;

		Map<String, List<String>> databaseHostMap = context.getAppConfig().getDatabaseHosts();
		List<String> databaseHosts = databaseHostMap != null ? databaseHostMap.get(databaseName) : null;

		if (databaseGroups != null && !databaseGroups.isEmpty()) {
			if (groupHostNamesProvider == null) {
				groupHostNamesProvider = groupName -> new GroupManager(context.getManageClient()).getHostNames(groupName);
			}

			if (logger.isInfoEnabled()) {
				logger.info(format("Creating replica forests on hosts in groups %s for database '%s'", databaseGroups, databaseName));
			}

			for (String groupName : databaseGroups) {
				List<String> groupHostNames = groupHostNamesProvider.getGroupHostNames(groupName);
				if (groupHostNames == null || groupHostNames.isEmpty()) {
					logger.warn("No hosts found for group: " + groupName);
					continue;
				}

				for (String hostName : hostNames) {
					if (groupHostNames.contains(hostName)) {
						selectedHostNames.add(hostName);
					}
				}
			}

			if (!selectedHostNames.isEmpty()) {
				if (logger.isInfoEnabled()) {
					logger.info(format("Creating forests on hosts %s based on groups %s for database '%s'", selectedHostNames, databaseGroups, databaseName));
				}
				if (databaseHosts != null && !databaseHosts.isEmpty()) {
					logger.warn(format("Database groups and database hosts were both specified for database '%s'; " +
						"only database groups are being used, database hosts will be ignored.", databaseName));
				}
				return selectedHostNames;
			}

			logger.warn("Did not find any valid hosts in selected groups: " + databaseGroups);
		}

		/**
		 * If no database groups were specified, then retain any host that is either in the set of database hosts, or
		 * all hosts in case no database hosts were specified.
		 */
		for (String hostName : hostNames) {
			if ((databaseHosts == null || databaseHosts.contains(hostName))) {
				selectedHostNames.add(hostName);
			}
		}

		return selectedHostNames;
	}

	public void setDeleteReplicasOnUndo(boolean deleteReplicasOnUndo) {
		this.deleteReplicasOnUndo = deleteReplicasOnUndo;
	}

	public Map<String, Integer> getDatabaseNamesAndReplicaCounts() {
		return databaseNamesAndReplicaCounts;
	}

	public void setDatabaseNamesAndReplicaCounts(Map<String, Integer> databaseNamesAndReplicaCounts) {
		this.databaseNamesAndReplicaCounts = databaseNamesAndReplicaCounts;
	}

	public void setGroupHostNamesProvider(GroupHostNamesProvider groupHostNamesProvider) {
		this.groupHostNamesProvider = groupHostNamesProvider;
	}
}

/**
 * This really only exists to facilitate unit testing.
 */
interface GroupHostNamesProvider {
	List<String> getGroupHostNames(String groupName);
}
