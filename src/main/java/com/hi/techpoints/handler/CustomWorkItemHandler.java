package com.hi.techpoints.handler;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;
import org.jbpm.process.workitem.core.util.RequiredParameterValidator;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;

/**
 * This is custom workitem handler to perform `MyWorkItem` component operation.
 * 
 * @author elavarasan_pk
 *
 */
public class CustomWorkItemHandler extends AbstractLogOrThrowWorkItemHandler {
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		try {
			RequiredParameterValidator.validate(this.getClass(), workItem);

			// 1. sample parameters
			String sampleParam = (String) workItem.getParameter("SampleParam");
			String sampleParamTwo = (String) workItem.getParameter("SampleParamTwo");

			// 2. Concat input parameter values in result parameter
			Map<String, Object> results = new HashMap<String, Object>();
			results.put("SampleResult", sampleParam + " " + sampleParamTwo);
			manager.completeWorkItem(workItem.getId(), results);
		} catch (Throwable cause) {
			handleException(cause);
		}
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing, this work item cannot be aborted
	}

}
