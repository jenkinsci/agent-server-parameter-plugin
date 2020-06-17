package com.bluersw;

import com.sonyericsson.rebuild.RebuildParameterPage;
import com.sonyericsson.rebuild.RebuildParameterProvider;
import hudson.Extension;
import hudson.model.ParameterValue;

/**
 * @author sunweisheng
 */
@Extension(optional = true)
public class AgentParameterRebuild extends RebuildParameterProvider {
	@Override
	public RebuildParameterPage getRebuildPage(ParameterValue parameterValue) {
		if (parameterValue instanceof AgentParameterValue) {
			return new RebuildParameterPage(parameterValue.getClass(),"value.jelly");
		}
		return null;
	}
}
