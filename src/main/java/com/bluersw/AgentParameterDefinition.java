package com.bluersw;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * @author sunweisheng
 * Jenkins 构建参数：Agent服务器选择（Jenkins build parameters: Agent server selection）
 */
public class AgentParameterDefinition extends ParameterDefinition implements  Comparable<AgentParameterDefinition> {

	private static final long serialVersionUID = 8844393428160958128L;
	private static final Logger LOGGER = Logger.getLogger(AgentParameterDefinition.class.getName());
	private static final String MASTER_DEFAULT = "master";
	private static final String DESCRIPTION = "Agent Server Parameter.";

	private final UUID uuid;
	private String defaultValue;

	/**
	 * 构造函数在构建项目配置构建参数时调用（The constructor is called when the build project configures build parameter）
	 * @param name 构建参数名称 （Build parameter name）
	 * @param defaultValue 该构建参数的默认值，会随着每次用户选择Agent服务器而改变（The default value of this build parameter will change each time the user selects the Agent server）
	 */
	@DataBoundConstructor
	public AgentParameterDefinition(String name, String defaultValue) {
		super(name, DESCRIPTION);
		this.uuid = UUID.randomUUID();
		this.setDefaultValue(defaultValue);
	}

	/**
	 * 获取构建参数默认值 （Get default values of build parameter）
	 * @return 构建参数默认值 （Build parameter default value）
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * 设置构建参数默认值 （Set default values for build parameter）
	 * @param defaultValue 要设置的构建参数默认值 （The default value of the build parameter to be set）
	 */
	@DataBoundSetter
	public void setDefaultValue(String defaultValue) {
		if (defaultValue == null || defaultValue.isEmpty() || isBlank(defaultValue)) {
			//默认是Master服务器（The default is the Master server）
			this.defaultValue = MASTER_DEFAULT;
		}
		else {
			this.defaultValue = defaultValue;
		}
	}

	/**
	 * 防止项目中有多个Agent Server Parameter，为每个构建参数的DIV元素创建唯一的ID值（Prevent multiple Agent Server Parameter in the project, create a unique ID value for each DIV element of the build parameter）
	 * @return DIV唯一ID（DIV unique ID）
	 */
	public String getDivId() {
		return String.format("%s-%s", getName().replaceAll("\\W", "_"), this.uuid);
	}

	/**
	 * 创建Agent服务器参数的参数结果对象（Create parameter result object for Agent server parameter）
	 * @param staplerRequest StaplerRequest对象（StaplerRequest object）
	 * @param jsonObject Agent服务器参数的结果对象，Json格式（Agent Server Parameter result object, Json format）
	 * @return 参数结果对象 （Parameter result object）
	 */
	@CheckForNull
	@Override
	public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
		Object value = jsonObject.get("value");
		StringBuilder strValue = new StringBuilder();
		if (value instanceof String) {
			strValue.append(value);
		}
		else if (value instanceof JSONArray) {
			JSONArray jsonValues = (JSONArray) value;
			for (int i = 0; i < jsonValues.size(); i++) {
				strValue.append(jsonValues.getString(i));
				if (i < jsonValues.size() - 1) {
					strValue.append(",");
				}
			}
		}

		if (strValue.length() == 0) {
			strValue.append(this.getDefaultValue());
		}

		return new AgentParameterValue(jsonObject.getString("name"), strValue.toString());
	}

	/**
	 * 创建Agent服务器参数的参数结果对象（Create parameter result object for Agent server parameter）
	 * @param staplerRequest StaplerRequest对象（StaplerRequest object）
	 * @return 参数结果对象 （Parameter result object）
	 */
	@CheckForNull
	@Override
	public ParameterValue createValue(StaplerRequest staplerRequest) {
		String[] value = staplerRequest.getParameterValues(this.getName());
		if (value == null || value.length == 0 || isBlank(value[0])) {
			return this.getDefaultParameterValue();
		}
		else {
			return new AgentParameterValue(this.getName(), value[0]);
		}
	}

	/**
	 * 创建Agent服务器参数的参数结果对象（Create parameter result object for Agent server parameter）
	 * @param command CLICommand Object
	 * @param value Agent服务器参数的结果对象，字符串格式（Agent Server Parameter result object, String format）
	 * @return 参数结果对象 （Parameter result object）
	 */
	@Override
	public ParameterValue createValue(CLICommand command, String value) {
		if (isNotEmpty(value)) {
			return new AgentParameterValue(this.getName(), value);
		}
		return getDefaultParameterValue();
	}

	/**
	 * 创建含有默认值的结果对象 （Create a result object with default values）
	 * @return 默认的参数结果对象 （Default parameter result object）
	 */
	@Override
	public ParameterValue getDefaultParameterValue() {
		return new AgentParameterValue(this.getName(), this.getDefaultValue());
	}

	@SuppressFBWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS")
	@Override
	public int compareTo(AgentParameterDefinition o) {
		if (o.uuid.equals(this.uuid)) {
			return 0;
		}
		else {
			return -1;
		}
	}

	/**
	 * 获取所有Agent Server的显示名称（Get the display names of all Agent Servers）
	 * @return Agent Server的显示名称列表（List of display names of Agent Server）
	 */
	public List<String> getComputerNames() {
		Computer[] computers = Jenkins.get().getComputers();
		List<String> nameList = new ArrayList<>();
		for (Computer computer : computers) {
			nameList.add(computer.getDisplayName());
		}

		//确保列表中有Master节点（Make sure there is a Master node in the list）
		if (!nameList.contains(MASTER_DEFAULT)) {
			nameList.add(0, MASTER_DEFAULT);
		}

		String defaultName = this.getDefaultValue();
		if (nameList.contains(defaultName)) {
			nameList.remove(defaultName);
			//除了第一次使用，默认值就是上次构建用户选择的值，排列在列表的最上面。（Except for the first use, the default value is the value selected by the user of the last build, arranged at the top of the list.）
			nameList.add(0, defaultName);
		}

		return nameList;
	}

	/**
	 * 获得构建参数描述对象，除了属性值绑定之外，与UI交互的动作方法在此对象内实现（Obtain the build parameter description object. In addition to the attribute value binding, the action method for interacting with the UI is implemented in this object）
	 * @return 构建参数对象描述对象（Build parameter object description object）
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * 参数描述类，实现了与UI交互的方法。（The parameter description class implements the method of interacting with the UI.）
	 */
	@Symbol("agentParameter")
	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {

		public DescriptorImpl() {
			load();
		}

		/**
		 * 验证用户输入的参数名称是否合法，注意一定是“doCheck”+“要检查的参数名称”形式为方法名称。（Verify that the parameter name entered by the user is legal. Note that it must be in the form of "doCheck" + "parameter name to be checked" as the method name.）
		 * @param name 要检查的Name内容。（Name content to check.）
		 * @return 检查是否通过，如果没有通过返回错误信息。（Check if it passes, and return an error message if it fails.）
		 */
		public FormValidation doCheckName(@QueryParameter String name) {
			if (name.length() == 0) {
				return FormValidation.error(Messages.AgentParameterDefinition_DescriptorImpl_errors_missingName());
			}

			return FormValidation.ok();
		}

		/**
		 * 在项目配置界面显示此构建参数的名称
		 * @return "Agent Server Parameter"
		 */
		@NonNull
		@Override
		public String getDisplayName() {
			return Messages.AgentParameterDefinition_DescriptorImpl_DisplayName();
		}

		@Override
		/*
		 * We need this for JENKINS-26143 -- reflective creation cannot handle setChoices(Object). See that method for context.
		 */
		public ParameterDefinition newInstance(@Nullable StaplerRequest req, @NonNull JSONObject formData) {
			String name = formData.getString("name");
			String value = formData.getString("defaultValue");
			return new AgentParameterDefinition(name, value);
		}

		/**
		 * 客户端选择Agent服务器时JS脚本调用的服务器端方法，作用是更新此参数的默认值，以便于方便用户下次项目构建时不需要再次选择Agent服务器。方法名一定是"do"+"方法名"。（When the client selects the agent server, the server-side method called by the JS script is to update the default value of this parameter, so that the user does not need to select the agent server again when the project is built next time.The method name must be "do" + "method name".）
		 * @param job 当前项目的构建任务。（The build task of the current project.）
		 * @param name Agent Server Parameter的名称。（The name of the agent Server Parameter.）
		 * @param value 用户选择的Agent服务器的名称。（The name of the agent server selected by the user.）
		 * @return 操作结果说明。（Explanation of operation result.）
		 */
		public String doSetDefaultValue(@AncestorInPath Job job, @QueryParameter String name, @QueryParameter String value) {
			ParametersDefinitionProperty prop = (ParametersDefinitionProperty) job
					.getProperty(ParametersDefinitionProperty.class);
			if (prop != null) {
				ParameterDefinition pd = prop.getParameterDefinition(name);
				if (pd instanceof AgentParameterDefinition) {
					((AgentParameterDefinition) pd).setDefaultValue(value);
					return Messages.AgentParameterDefinition_DescriptorImpl_success_updateDefault();
				}
			}

			LOGGER.log(Level.SEVERE, String
					.format("%s When executing the doSetDefaultValue method, no build parameter named %s was found.", job
							.getDisplayName(), name));

			return Messages.AgentParameterDefinition_DescriptorImpl_errors_updateDefault();
		}

		/**
		 * 在项目构建页面设置参数时调用，将所有可以用于构建的服务器名称，绑定到下拉菜单中让用户选择，方法名称必须是"doFill"+要绑定数据的页面元素field属性值+"Items"。（Called when setting parameters on the project construction page, bind all server names that can be used for construction to the drop-down menu for the user to choose, the method name must be "doFill" + field attribute value of the page element to be bound data + "Items ".）
		 * @param job 当前项目的构建任务。（The build task of the current project.）
		 * @param name Agent Server Parameter的名称。（The name of the Agent Server Parameter.）
		 * @return Agent名称列表是Select元素，返回此元素的内容。（The agent name list is the Select element, and returns the content of this element.）
		 */
		public ListBoxModel doFillValueItems(@AncestorInPath Job job, @QueryParameter String name) {
			ParametersDefinitionProperty prop = (ParametersDefinitionProperty) job
					.getProperty(ParametersDefinitionProperty.class);
			if (prop != null) {
				ParameterDefinition pd = prop.getParameterDefinition(name);
				if (pd instanceof AgentParameterDefinition) {
					AgentParameterDefinition spd = (AgentParameterDefinition) pd;
					List<String> computerNames = spd.getComputerNames();
					ListBoxModel list = new ListBoxModel(computerNames.size());
					for (String computerName : computerNames) {
						list.add(computerName, computerName);
					}
					return list;
				}
			}

			LOGGER.log(Level.SEVERE, String
					.format("%s When executing the doFillValueItems method, no build parameter named %s was found.", job
							.getDisplayName(), name));

			return new ListBoxModel(0);
		}
	}
}
