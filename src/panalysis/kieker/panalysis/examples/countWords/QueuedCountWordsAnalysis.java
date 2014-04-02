/***************************************************************************
 * Copyright 2014 Kieker Project (http://kieker-monitoring.net)
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

package kieker.panalysis.examples.countWords;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.chw.util.Pair;

import kieker.panalysis.framework.concurrent.StageTerminationPolicy;
import kieker.panalysis.framework.concurrent.WorkerThread;
import kieker.panalysis.framework.core.AbstractFilter;
import kieker.panalysis.framework.core.Analysis;
import kieker.panalysis.framework.core.IPipeline;
import kieker.panalysis.framework.core.IStage;
import kieker.panalysis.framework.sequential.MethodCallPipe;
import kieker.panalysis.framework.sequential.QueuePipe;
import kieker.panalysis.stage.basic.RepeaterSource;
import kieker.panalysis.stage.basic.distributor.Distributor;
import kieker.panalysis.stage.basic.merger.Merger;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public class QueuedCountWordsAnalysis extends Analysis {

	private static final int NUM_TOKENS_TO_REPEAT = 1000;
	private static final String START_DIRECTORY_NAME = ".";
	private static final int SECONDS = 1000;

	private WorkerThread workerThread;

	@Override
	public void init() {
		super.init();

		final IPipeline pipeline = this.buildNonIoPipeline();

		this.workerThread = new WorkerThread(pipeline, 0);
	}

	@Override
	public void start() {
		super.start();

		this.workerThread.terminate(StageTerminationPolicy.TERMINATE_STAGE_AFTER_UNSUCCESSFUL_EXECUTION);

		this.workerThread.start();
		try {
			this.workerThread.join(60 * SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private IPipeline buildNonIoPipeline() {
		// create stages
		final RepeaterSource<String> repeaterSource = RepeaterSource.create(START_DIRECTORY_NAME, NUM_TOKENS_TO_REPEAT);
		final DirectoryName2Files findFilesStage = new DirectoryName2Files();
		final Distributor<File> distributor = new Distributor<File>();
		final CountWordsStage countWordsStage0 = new CountWordsStage();
		final CountWordsStage countWordsStage1 = new CountWordsStage();
		final Merger<Pair<File, Integer>> merger = new Merger<Pair<File, Integer>>();
		final OutputWordsCountSink outputWordsCountStage = new OutputWordsCountSink();

		// add each stage to a stage list
		final List<IStage> stages = new LinkedList<IStage>();
		stages.add(repeaterSource);
		stages.add(findFilesStage);
		stages.add(distributor);
		stages.add(countWordsStage0);
		stages.add(countWordsStage1);
		stages.add(merger);
		stages.add(outputWordsCountStage);

		// connect stages by pipes
		QueuePipe.connect(repeaterSource.OUTPUT, findFilesStage.DIRECTORY_NAME);
		QueuePipe.connect(findFilesStage.fileOutputPort, distributor.genericInputPort);
		QueuePipe.connect(distributor.getNewOutputPort(), countWordsStage0.FILE);
		QueuePipe.connect(distributor.getNewOutputPort(), countWordsStage1.FILE);
		QueuePipe.connect(countWordsStage0.WORDSCOUNT, merger.getNewInputPort());
		QueuePipe.connect(countWordsStage1.WORDSCOUNT, merger.getNewInputPort());
		QueuePipe.connect(merger.outputPort, outputWordsCountStage.fileWordcountTupleInputPort);

		repeaterSource.START.setAssociatedPipe(new MethodCallPipe<Boolean>(Boolean.TRUE));

		final IPipeline pipeline = new IPipeline() {
			@SuppressWarnings("unchecked")
			public List<? extends IStage> getStartStages() {
				return Arrays.asList(repeaterSource);
			}

			public List<IStage> getStages() {
				return stages;
			}

			public void fireStartNotification() throws Exception {
				for (final IStage stage : this.getStartStages()) {
					stage.notifyPipelineStarts();
				}
			}

			public void fireStopNotification() {
				for (final IStage stage : this.getStartStages()) {
					stage.notifyPipelineStops();
				}
			}
		};

		return pipeline;
	}

	WorkerThread getWorkerThread() {
		return this.workerThread;
	}

	public static void main(final String[] args) {
		final QueuedCountWordsAnalysis analysis = new QueuedCountWordsAnalysis();
		analysis.init();
		final long start = System.currentTimeMillis();
		analysis.start();
		final long end = System.currentTimeMillis();
		// analysis.terminate();
		final long duration = end - start;
		System.out.println("duration: " + duration + " ms"); // NOPMD (Just for example purposes)

		final IPipeline pipeline = analysis.workerThread.getPipeline();

		for (final IStage stage : pipeline.getStages()) {
			if (stage instanceof AbstractFilter<?>) {
				System.out.println(stage.getClass().getName() + ": " + ((AbstractFilter<?>) stage).getOverallDuration()); // NOPMD (Just for example purposes)
			}
		}

		final DirectoryName2Files findFilesStage = (DirectoryName2Files) pipeline.getStages().get(1);
		System.out.println("findFilesStage: " + findFilesStage.getNumFiles()); // NOPMD (Just for example purposes)

		final OutputWordsCountSink outputWordsCountStage = (OutputWordsCountSink) pipeline.getStages().get(6);
		System.out.println("outputWordsCountStage: " + outputWordsCountStage.getNumFiles()); // NOPMD (Just for example purposes)
	}
}