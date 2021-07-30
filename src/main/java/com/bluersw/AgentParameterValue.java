package com.bluersw;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Label;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.queue.SubTask;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author sunweisheng
 */
public class AgentParameterValue extends StringParameterValue {
	private static final long serialVersionUID = 6413402566819239460L;

	@DataBoundConstructor
	public AgentParameterValue(String name, String value){super(name, value);}

	/**
	 * Adds environmental variables for the builds to the given map.
	 * This provides a means for a parameter to pass the parameter values to the build to be performed.
	 * When this method is invoked, the map already contains the current "planned export" list. The implementation is expected to add more values to this map (or do nothing)
	 * */
	@Override
	public void buildEnvironment(Run<?, ?> build, EnvVars env) {
		env.put(this.name,this.value);
	}

	/**
	 * Controls where the build (that this parameter is submitted to) will happen.
	 * */
	@Override
	public Label getAssignedLabel(SubTask task) {
		return Label.get(this.value);
	}

	/**
	 * Returns a VariableResolver so that other components like Builders can perform variable substitution to reflect parameter values into the build process.
	 * This is yet another means in which a ParameterValue can influence a build.
	 * */
	@Override
	public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
		return new VariableResolver<String>() {
			public String resolve(String name) {
				return AgentParameterValue.this.name.equals(name) ? AgentParameterValue.this.value : null;
			}
		};
	}
}
