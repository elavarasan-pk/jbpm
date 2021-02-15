package com.hi.techpoints;

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

/**
 * Simple jBPM hello world program
 * 
 * @author elavarasan_pk
 */
public class ProcessMain { 
    private static final Logger log = LoggerFactory.getLogger(ProcessMain.class);
    
    public static final void main(String[] args) throws Exception {
        // 1. Kie Container will load *.bpmn file into the knowledge base
        KieBase kbase = readKnowledgeBase();
        
        // 2. Create a jBPM RuntimeManager
        RuntimeManager runtimeManager = createRuntimeEnvironment(kbase);
        
        // 3. Get RuntimeEngine out of manager
        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get());
         
        // 4. Create KieSession from runtimeEngine - already initialized *.bpmn file on the environment 
        KieSession ksession = runtimeEngine.getKieSession();
        
        log.info("Process starting ...");
        // 5. Start a new process instance here
        ksession.startProcess("com.hi.techpoints.helloworld");     
        
        // 6. At Last dispose the runtime engine
        runtimeManager.disposeRuntimeEngine(runtimeEngine);
        System.exit(0);
    }

    private static KieBase readKnowledgeBase() throws Exception {
    	KieServices kieServices = KieServices.Factory.get();
    	KieContainer kContainer = kieServices.getKieClasspathContainer();
    	KieBase kBase = kContainer.getKieBase("kbase");
    	return kBase;
    }
    
    public static RuntimeManager createRuntimeEnvironment(KieBase kbase) {
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