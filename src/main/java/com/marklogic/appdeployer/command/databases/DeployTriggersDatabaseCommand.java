package com.marklogic.appdeployer.command.databases;

import com.marklogic.appdeployer.command.CommandContext;
import com.marklogic.appdeployer.command.SortOrderConstants;

/**
 * Deprecated in 3.11.0 - ever since sorting was added to DeployOtherDatabasesCommand, there's been no need for
 * this command.
 */
@Deprecated
public class DeployTriggersDatabaseCommand extends DeployDatabaseCommand {

	public DeployTriggersDatabaseCommand() {
		setExecuteSortOrder(SortOrderConstants.DEPLOY_TRIGGERS_DATABASE);
		setUndoSortOrder(SortOrderConstants.DELETE_TRIGGERS_DATABASE);
		setDatabaseFilename("triggers-database.json");
		setCreateForestsOnEachHost(false);
	}

	@Override
	public void execute(CommandContext context) {
		if (context.getAppConfig().isCreateTriggersDatabase()) {
			setCreateDatabaseWithoutFile(true);
		}
		super.execute(context);
	}

	@Override
	public void undo(CommandContext context) {
		if (context.getAppConfig().isCreateTriggersDatabase()) {
			setCreateDatabaseWithoutFile(true);
		}
		super.undo(context);
	}

	protected String buildDefaultDatabasePayload(CommandContext context) {
		return format("{\"database-name\": \"%s\"}", context.getAppConfig().getTriggersDatabaseName());
	}

}