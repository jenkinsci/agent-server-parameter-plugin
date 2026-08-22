package com.bluersw;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.ParametersDefinitionProperty;
import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.bluersw.Constants.DEFAULT_VALUE;
import static com.bluersw.Constants.NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AgentParameterDefinitionTest {
	@Rule
	public JenkinsRule jenkins = new JenkinsRule();

	@Test
	public void testScriptedPipeline() throws Exception {
		AgentParameterDefinition agentParam = new AgentParameterDefinition(NAME, DEFAULT_VALUE);

		WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
		job.addProperty(new ParametersDefinitionProperty(agentParam));
		String pipelineScript
				= "node {\n"
				+ "  print params['agent'] \n"
				+ "}";
		job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
		WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
		String expectedString = DEFAULT_VALUE;
		jenkins.assertLogContains(expectedString, completedBuild);
	}

	@Test
	public void testPipelineStartupIsNotAssignedToSelectedAgent() throws Exception {
		WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-pipeline-label");
		AgentParameterValue value = new AgentParameterValue(NAME, "windows-agent");

		assertNull(value.getAssignedLabel(job));
	}

	@Test
	public void testFreestyleBuildIsAssignedToSelectedAgent() throws Exception {
		FreeStyleProject job = jenkins.createFreeStyleProject("test-freestyle-label");
		AgentParameterValue value = new AgentParameterValue(NAME, "linux-agent");

		assertEquals(Label.get("linux-agent"), value.getAssignedLabel(job));
	}

	@Test
	public void testComputerNamesUseRunnableNodeLabels() throws Exception {
		DumbSlave agent = jenkins.createOnlineSlave();
		AgentParameterDefinition parameter = new AgentParameterDefinition(NAME, null);

		List<String> names = parameter.getComputerNames();

		assertEquals(jenkins.jenkins.getSelfLabel().getName(), names.get(0));
		assertTrue(names.contains(agent.getSelfLabel().getName()));
		assertFalse(names.contains(jenkins.jenkins.toComputer().getDisplayName()));
	}

	@Test
	public void testSelectedAgentIsPersistedAsDefault() throws Exception {
		FreeStyleProject job = jenkins.createFreeStyleProject("test-persisted-default");
		AgentParameterDefinition parameter = new AgentParameterDefinition(NAME, DEFAULT_VALUE);
		job.addProperty(new ParametersDefinitionProperty(parameter));

		String result = parameter.getDescriptor().doSetDefaultValue(job, NAME, "linux-agent");

		assertEquals(Messages.AgentParameterDefinition_DescriptorImpl_success_updateDefault(), result);
		assertEquals("linux-agent", parameter.getDefaultValue());
		String configXml = Files.readString(job.getConfigFile().getFile().toPath());
		assertTrue(configXml.contains("<defaultValue>linux-agent</defaultValue>"));
	}

	@Test
	public void testBuildFormListsAllNodesAndSelectsDefault() throws Exception {
		DumbSlave agent = jenkins.createOnlineSlave();
		String agentName = agent.getSelfLabel().getName();
		WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-build-form-options");
		job.addProperty(new ParametersDefinitionProperty(
				new AgentParameterDefinition(NAME, agentName)));

		JenkinsRule.WebClient webClient = jenkins.createWebClient();
		webClient.setThrowExceptionOnFailingStatusCode(false);
		HtmlPage page = webClient.getPage(job, "build?delay=0sec");
		HtmlSelect select = page.getFirstByXPath(
				"//select[contains(concat(' ', normalize-space(@class), ' '), ' agent-server-parameter-select ')]");

		assertNotNull(select);
		assertEquals(agentName, select.getSelectedOptions().get(0).getValueAttribute());
		assertTrue(select.getOptions().stream()
				.anyMatch(option -> agentName.equals(option.getValueAttribute())));
		assertTrue(select.getOptions().stream()
				.anyMatch(option -> jenkins.jenkins.getSelfLabel().getName()
						.equals(option.getValueAttribute())));
	}

	@Test
	public void testBuildFormEscapesParameterName() throws Exception {
		String maliciousName = "\"><img id=\"xss-name\" src=\"x\" onerror=\"alert(1)\">";
		WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-escaped-build-form");
		job.addProperty(new ParametersDefinitionProperty(
				new AgentParameterDefinition(maliciousName, null)));

		JenkinsRule.WebClient webClient = jenkins.createWebClient();
		webClient.setThrowExceptionOnFailingStatusCode(false);
		HtmlPage page = webClient.getPage(job, "build?delay=0sec");
		String response = page.getWebResponse().getContentAsString();

		assertNull(page.getFirstByXPath("//*[@id='xss-name']"));
		assertTrue(response.contains("&lt;img"));
		assertFalse(response.contains("<img id=\"xss-name\""));
	}

	@Test
	public void testIndexViewUsesExternalScriptInitialization() throws Exception {
		try (InputStream view = AgentParameterDefinition.class
				.getResourceAsStream("AgentParameterDefinition/index.jelly");
			 InputStream rebuildView = AgentParameterRebuild.class
				.getResourceAsStream("AgentParameterRebuild/value.jelly");
			 InputStream script = AgentParameterDefinition.class
				.getResourceAsStream("javascript/agent-parameter.js")) {
			assertNotNull(view);
			assertNotNull(rebuildView);
			assertNotNull(script);
			String jelly = new String(view.readAllBytes(), StandardCharsets.UTF_8);
			String rebuildJelly = new String(rebuildView.readAllBytes(), StandardCharsets.UTF_8);
			String javascript = new String(script.readAllBytes(), StandardCharsets.UTF_8);
			assertFalse(jelly.contains("<script"));
			assertFalse(javascript.contains("jQuery"));
			assertTrue(jelly.contains("data-update-url"));
			assertTrue(jelly.contains("data-parameter-name"));
			assertTrue(jelly.contains("items=\"${it.computerNames}\""));
			assertFalse(jelly.contains("<f:select"));
			assertTrue(jelly.contains("escapeEntryTitleAndDescription\" value=\"false"));
			assertTrue(jelly.contains("title=\"${h.escape(it.name)}\""));
			assertTrue(jelly.contains("description=\"${it.formattedDescription}\""));
			assertTrue(rebuildJelly.contains("escapeEntryTitleAndDescription\" value=\"false"));
			assertTrue(rebuildJelly.contains("title=\"${h.escape(it.name)}\""));
			assertTrue(rebuildJelly.contains("description=\"${it.formattedDescription}\""));
			assertTrue(javascript.contains("Behaviour.specify"));
		}
	}
}
