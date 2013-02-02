/***************************************************************************
 * Copyright 2012 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package kieker.analysis.plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import kieker.analysis.analysisComponent.IAnalysisComponent;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.analysis.repository.AbstractRepository;
import kieker.common.configuration.Configuration;

/**
 * This is the interface for plugins within Kieker.
 * 
 * @author Nils Christian Ehmke, Jan Waller
 */
public interface IPlugin extends IAnalysisComponent {

	/**
	 * Initiates the start of a component.
	 * This method is called once when a AnalysisController's run() method is called.
	 * This implementation must not be blocking!
	 * Asynchronous consumers would spawn (an) asynchronous thread(s) in this method.
	 * 
	 * @return true on success; false otherwise.
	 */
	public boolean init();

	/**
	 * Initiates a termination of the plugin. This method is only used by the
	 * framework and should not be called manually.
	 * Use the method {@link kieker.analysis.AnalysisController#terminate(boolean)} instead.
	 * 
	 * After receiving this notification, the plugin should terminate any running
	 * methods, e.g., read for readers.
	 */
	public void terminate(final boolean error);

	/**
	 * This method should deliver a {@code Configuration} object containing the current configuration of this instance. In other words: The constructor should be
	 * able to use the given object to initialize a new instance of this class with the same intern properties.
	 * 
	 * @return A completely filled configuration object.
	 */
	public abstract Configuration getCurrentConfiguration();

	/**
	 * This method delivers the plugin name of this plugin. The name should be unique, e.g., the classname.
	 * 
	 * @return The name of the plugin.
	 */
	public abstract String getPluginName();

	/**
	 * This method delivers the description of this plugin type.
	 * 
	 * @return The description of the plugin type.
	 */
	public abstract String getPluginDescription();

	/**
	 * This method delivers the current name of this plugin instance. The name does not have to be unique.
	 * 
	 * @return The current name of the plugin instance.
	 */
	public abstract String getName();

	/**
	 * Connects the given repository to this plugin via the given name. <b>DO NOT USE THIS METHOD!</b> Use <code>AnalysisController.connect</code> instead!
	 * 
	 * @param name
	 *            The name of the port to connect the repository.
	 * @param repo
	 *            The repository which should be used.
	 * @throws AnalysisConfigurationException
	 *             if the repository-port is invalid, the repository itself is incompatible or the port is already used.
	 */
	public abstract void connect(final String name, final AbstractRepository repo) throws AnalysisConfigurationException;

	/**
	 * This method delivers an array of {@code AbstractRepository} containing the current repositories of this instance. In other words: The constructor should
	 * be able to use the given object to initialize a new instance of this class with the same intern properties.
	 * 
	 * @return An (possible empty) array of repositories.
	 */
	public abstract Map<String, AbstractRepository> getCurrentRepositories();

	public abstract String[] getAllOutputPortNames();

	public abstract String[] getAllInputPortNames();

	public abstract String[] getAllDisplayNames();

	/**
	 * Delivers the plugins with their ports which are connected with the given output port.
	 * 
	 * @param outputPortName
	 *            The name of the output port.
	 * @return An array of pairs, whereat the first element is the plugin and the second one the name of the input port. If the given output port is invalid, null is
	 *         returned
	 */
	public abstract List<PluginInputPortReference> getConnectedPlugins(final String outputPortName);

	/**
	 * 
	 * @return the current state of the plugin
	 */
	public abstract STATE getState();

	/**
	 * This simple class represents a container for the reference between a plugin, its input port and the corresponding method.
	 * 
	 * @author Nils Christian Ehmke
	 */
	public static final class PluginInputPortReference {

		private final IPlugin plugin;
		private final String inputPortName;
		private final Method inputPortMethod;
		private final Class<?>[] eventTypes;

		/**
		 * Creates a new instance of this class using the given parameters.
		 * 
		 * @param plugin
		 *            The plugin to store in this container.
		 * @param inputPortName
		 *            The name of the input port of the plugin.
		 * @param inputPortMethod
		 *            The corresponding method which is the input port.
		 * @param eventTypes
		 *            The event types of the port.
		 */
		public PluginInputPortReference(final IPlugin plugin, final String inputPortName, final Method inputPortMethod, final Class<?>[] eventTypes) {
			this.plugin = plugin;
			this.inputPortName = inputPortName;
			this.inputPortMethod = inputPortMethod;
			this.eventTypes = eventTypes.clone();
		}

		/**
		 * Getter for the attribute {@link PluginInputPortReference#plugin}.
		 * 
		 * @return The current value of the attribute.
		 */
		public final IPlugin getPlugin() {
			return this.plugin;
		}

		/**
		 * Getter for the attribute {@link PluginInputPortReference#inputPortMethod}.
		 * 
		 * @return The current value of the attribute.
		 */
		public final Method getInputPortMethod() {
			return this.inputPortMethod;
		}

		/**
		 * Getter for the attribute {@link PluginInputPortReference#eventTypes}.
		 * 
		 * @return The current value of the attribute.
		 */
		public final Class<?>[] getEventTypes() {
			return this.eventTypes.clone();
		}

		/**
		 * Getter for the attribute {@link PluginInputPortReference#inputPortName}.
		 * 
		 * @return The current value of the attribute.
		 */
		public final String getInputPortName() {
			return this.inputPortName;
		}
	}

	/**
	 * An enumeration used to describe the state of an {@link AbstractPlugin}.
	 * 
	 * @author Jan Waller
	 */
	public static enum STATE {
		/**
		 * The plugin has been initialized and is ready to be configured.
		 */
		READY,
		/**
		 * The plugin is currently running.
		 */
		RUNNING,
		/**
		 * The plugin has been notified to terminate.
		 */
		TERMINATING,
		/**
		 * The plugin has been terminated.
		 */
		TERMINATED,
		/**
		 * The plugin has been notified to terminate with error.
		 */
		FAILING,
		/**
		 * The plugin has been terminated with error.
		 */
		FAILED,
	}

}
