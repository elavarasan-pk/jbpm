package com.hi.techpoints;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.test.util.db.PersistenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessMain {
	private static final Logger log = LoggerFactory.getLogger(ProcessMain.class);
   
	public static void main(String[] args) {
		// 1. Create ProcessMain object
    	ProcessMain processMain = new ProcessMain();
    	
		// 2. Kie Container will load *.bpmn file into the knowledge base
        KieBase kbase = processMain.readKnowledgeBase();
        
        // 3. Create a jBPM RuntimeManager
        RuntimeManager runtimeManager = processMain.createRuntimeEnvironment(kbase);
        
        // 4. Get RuntimeEngine out of manager
        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
         
        // 5. Create KieSession from runtimeEngine - already initialized *.bpmn file on the environment 
        KieSession ksession = runtimeEngine.getKieSession();
              
        // 6. Start a businessruleprocess instance with parameter
        log.info("Process starting ...");
	    Map<String, Object> params = new HashMap<String, Object>();
	    params.put("patientBpRange", 121);	
	    ksession.startProcess("com.hi.techpoints.process.businessruleprocess", params);
	    
	    // 7. At Last dispose the runtime engine
        runtimeManager.disposeRuntimeEngine(runtimeEngine);
        System.exit(0);	    
	}
	
    private KieBase readKnowledgeBase()  {
    	KieServices kieServices = KieServices.Factory.get();
    	KieContainer kContainer = kieServices.getKieClasspathContainer();
    	KieBase kBase = kContainer.getKieBase("kbase");
    	return kBase;
    }
    
    public RuntimeManager createRuntimeEnvironment(KieBase kbase) {
    	Properties properties = new Properties();
		properties.put("driverClassName", "org.h2.Driver");
		properties.put("className", "org.h2.jdbcx.JdbcDataSource");
		properties.put("user", "sa");
		properties.put("password", "");
		properties.put("url", "jdbc:h2:tcp://localhost/~/jbpm-db");
		properties.put("datasourceName", "jdbc/jbpm-ds");
		PersistenceUtil.setupPoolingDataSource(properties);
    	
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");                            
        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get()
            .newDefaultBuilder().entityManagerFactory(emf).knowledgeBase(kbase);
        
        return RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get());
    }
	
}