package com.bluersw;

import hudson.model.ParameterValue;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerRequest2;

import static com.bluersw.Constants.DEFAULT_VALUE;
import static com.bluersw.Constants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicTests {

	@Test
	void testCreateValueUsesDefaultWhenRequestHasNoValue() {
		AgentParameterDefinition instance = new AgentParameterDefinition(NAME, DEFAULT_VALUE);
		StaplerRequest2 request = mock(StaplerRequest2.class);
		ParameterValue result = instance.createValue(request);

		assertEquals(new AgentParameterValue(NAME, DEFAULT_VALUE), result);
	}

	@Test
	void testCreateValueUsesSubmittedRequestValue() {
		AgentParameterDefinition instance = new AgentParameterDefinition(NAME, DEFAULT_VALUE);
		StaplerRequest2 request = mock(StaplerRequest2.class);
		when(request.getParameterValues(NAME)).thenReturn(new String[] {"windows-agent"});

		ParameterValue result = instance.createValue(request);

		assertEquals(new AgentParameterValue(NAME, "windows-agent"), result);
	}

	@Test
	void testCreateValueUsesSubmittedJsonValue() {
		AgentParameterDefinition instance = new AgentParameterDefinition(NAME, DEFAULT_VALUE);
		JSONObject submittedValue = new JSONObject();
		submittedValue.put("name", NAME);
		submittedValue.put("value", "linux-agent");

		ParameterValue result = instance.createValue((StaplerRequest2) null, submittedValue);

		assertEquals(new AgentParameterValue(NAME, "linux-agent"), result);
	}
}
