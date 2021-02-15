package com.hi.techpoints;

import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.collections.CollectionUtils;
import org.jbpm.services.task.identity.DefaultUserInfo;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.test.util.db.PersistenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jBPM Human Task notification and reassign to other user
 * 
 * @author elavarasan_pk
 */
public class ProcessMain {
	private static final Logger log = LoggerFactory.getLogger(ProcessMain.class);

	public static final void main(String[] args) throws Exception {				  
		// 1. Create ProcessMain object
		ProcessMain processMain = new ProcessMain();

		// 2. Load *.bpmn file into the knowledge base
		KieBase kbase = processMain.readKnowledgeBase();

		// 3. Create a jBPM RuntimeManager
		RuntimeManager runtimeManager = processMain.createRuntimeEnvironment(kbase);

		// 4. Get RuntimeEngine out of manager
		RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
		
		// 5. Create KieSession from runtimeEngine - already initialized *.bpmn file on the environment
		KieSession ksession = runtimeEngine.getKieSession();
		
		// 6. Start a humantaskreassignnotifyprocess instance here
	    ksession.startProcess("com.hi.techpoints.process.humantaskreassignnotifyprocess");
		log.info("Process started ...");
		
		// 7. Process human task
		processMain.processTask(runtimeEngine);

		// 8. At Last dispose the runtime engine
		runtimeManager.disposeRuntimeEngine(runtimeEngine);
		System.exit(0);
	}

	private KieBase readKnowledgeBase() throws Exception {
		KieServices kieServices = KieServices.Factory.get();
		KieContainer kContainer = kieServices.getKieClasspathContainer();
		return kContainer.getKieBase("kbase");
	}

	public RuntimeManager createRuntimeEnvironment(KieBase kbase) {	
		  Properties properties = new Properties(); properties.put("driverClassName", "org.h2.Driver");
		  properties.put("className", "org.h2.jdbcx.JdbcDataSource");
		  properties.put("user", "sa"); properties.put("password", "");
		  properties.put("url", "jdbc:h2:tcp://localhost/~/jbpm-database");
		  properties.put("datasourceName", "jdbc/jbpm-ds");
		  PersistenceUtil.setupPoolingDataSource(properties);
		  
		  EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		 
		  RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get()
				.newDefaultBuilder().entityManagerFactory(emf)
				.knowledgeBase(kbase).userInfo(new DefaultUserInfo(true))
				.userGroupCallback(new JBossUserGroupCallbackImpl("classpath:/usergroup.properties"));
		 
		return RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get());
	}

	private void processTask(RuntimeEngine runtimeEngine) throws InterruptedException {
		  // 1. Get Task service
		  InternalTaskService taskService = (InternalTaskService)runtimeEngine.getTaskService();	
		  
		  //Assume user is not start a task within alloted time.
		  Thread.sleep(80000);
		  
		  // 2. Let Elavarasan execute Task 1
		  List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("kumar", "en-UK");
		  if(!CollectionUtils.isEmpty(list)) {
			  TaskSummary task = list.get(0);	
			  log.info("Kumar is executing task: {}", task.getName());
			  taskService.start(task.getId(), "kumar");
			  taskService.complete(task.getId(), "kumar", null);
		  }
	
		  // 3. Let Elavarasan execute Task 1
		  list = taskService.getTasksAssignedAsPotentialOwner("elavarasan", "en-UK");
		  if(!CollectionUtils.isEmpty(list)) {
			TaskSummary task = list.get(0);	
			log.info("Elavarasan is executing task: {}", task.getName());
		    taskService.start(task.getId(), "elavarasan");
		    taskService.complete(task.getId(), "elavarasan", null);
		  }
		  
		  // 4. Let mary execute Task 2 
		  list = taskService.getTasksAssignedAsPotentialOwner("ram", "en-UK"); 
		  if(!CollectionUtils.isEmpty(list)) {
			 TaskSummary task = list.get(0); 
		     log.info("Ram is executing task: {}", task.getName());
		     taskService.start(task.getId(), "ram"); 
		     taskService.complete(task.getId(), "ram", null);
		  }
	}

}