package com.bluersw;

import hudson.model.ParameterValue;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

import static com.bluersw.Constants.DEFAULT_VALUE;
import static com.bluersw.Constants.NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BasicTests {
	@Test
	public void testCreateValue_StaplerRequest(){
		AgentParameterDefinition instance = new AgentParameterDefinition(NAME, DEFAULT_VALUE);

		StaplerRequest request = mock(StaplerRequest.class);
		ParameterValue result = instance.createValue(request);

		assertEquals(result, new AgentParameterValue(NAME, DEFAULT_VALUE));
	}
}
