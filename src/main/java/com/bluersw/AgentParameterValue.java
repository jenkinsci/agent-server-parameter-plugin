package com.bluersw;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author sunweisheng
 */
public class AgentParameterValue extends StringParameterValue {
	private static final long serialVersionUID = 6413402566819239460L;

	@DataBoundConstructor
	public AgentParameterValue(String name, String value){super(name, value);}
}
