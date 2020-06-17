package com.bluersw;

import hudson.model.ParametersDefinitionProperty;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.bluersw.Constants.DEFAULT_VALUE;
import static com.bluersw.Constants.NAME;

public class AgentParameterDefinitionTest {
	@Rule
	public JenkinsRule jenkins = new JenkinsRule();

	@Test
	public void testScriptedPipeline() throws Exception{
		AgentParameterDefinition slaveParam = new AgentParameterDefinition(NAME, DEFAULT_VALUE);

		WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
		job.addProperty(new ParametersDefinitionProperty(slaveParam));
		String pipelineScript
				= "node {\n"
				+ "  print params['agent'] \n"
				+ "}";
		job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
		WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
		String expectedString = DEFAULT_VALUE;
		jenkins.assertLogContains(expectedString, completedBuild);
	}
}
