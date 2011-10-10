/***************************************************************************
 * Copyright 2011 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
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

package kieker.tools.traceAnalysis.plugins.visualization.callTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import kieker.analysis.plugin.configuration.AbstractInputPort;
import kieker.analysis.plugin.configuration.IInputPort;
import kieker.tools.traceAnalysis.plugins.traceReconstruction.TraceProcessingException;
import kieker.tools.traceAnalysis.systemModel.MessageTrace;
import kieker.tools.traceAnalysis.systemModel.repository.SystemModelRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author Andre van Hoorn
 */
public class AggregatedCallTreePlugin<T> extends AbstractCallTreePlugin<T> {
	private static final Log LOG = LogFactory.getLog(AggregatedCallTreePlugin.class);

	private final AbstractAggregatedCallTreeNode<T> root;
	private final File dotOutputFile;
	private final boolean includeWeights;
	private final boolean shortLabels;
	private int numGraphsSaved = 0;

	public AggregatedCallTreePlugin(final String name, final SystemModelRepository systemEntityFactory, final AbstractAggregatedCallTreeNode<T> root,
			final File dotOutputFile, final boolean includeWeights, final boolean shortLabels) {
		super(name, systemEntityFactory);
		this.root = root;
		this.dotOutputFile = dotOutputFile;
		this.includeWeights = includeWeights;
		this.shortLabels = shortLabels;
	}

	public void saveTreeToDotFile(final String outputFnBase, final boolean includeWeights, final boolean shortLabels) throws FileNotFoundException {
		AbstractCallTreePlugin.saveTreeToDotFile(super.getSystemEntityFactory(), this.root, outputFnBase, includeWeights, false, // do not include EOIs
				shortLabels);
		this.numGraphsSaved++;
		this.printMessage(new String[] { "Wrote call tree to file '" + outputFnBase + ".dot" + "'", "Dot file can be converted using the dot tool",
			"Example: dot -T svg " + outputFnBase + ".dot" + " > " + outputFnBase + ".svg", });
	}

	@Override
	public void printStatusMessage() {
		super.printStatusMessage();
		System.out.println("Saved " + this.numGraphsSaved + " call tree" + (this.numGraphsSaved > 1 ? "s" : "")); // NOCS
	}

	@Override
	public boolean execute() {
		return true; // no need to do anything here
	}

	/**
	 * Saves the call tree to the dot file if error is not true.
	 * 
	 * @param error
	 */
	@Override
	public void terminate(final boolean error) {
		if (!error) {
			try {
				this.saveTreeToDotFile(this.dotOutputFile.getCanonicalPath(), this.includeWeights, this.shortLabels);
			} catch (final IOException ex) {
				AggregatedCallTreePlugin.LOG.error("IOException", ex);
			}
		}
	}

	@Override
	public IInputPort<MessageTrace> getMessageTraceInputPort() {
		return this.messageTraceInputPort;
	}

	private final IInputPort<MessageTrace> messageTraceInputPort = new AbstractInputPort<MessageTrace>("Message traces") {

		@Override
		public void newEvent(final MessageTrace t) {
			try {
				AbstractCallTreePlugin.addTraceToTree(AggregatedCallTreePlugin.this.root, t, true); // aggregated
				AggregatedCallTreePlugin.this.reportSuccess(t.getTraceId());
			} catch (final TraceProcessingException ex) {
				AggregatedCallTreePlugin.LOG.error("TraceProcessingException", ex);
				AggregatedCallTreePlugin.this.reportError(t.getTraceId());
			}
		}
	};
}
