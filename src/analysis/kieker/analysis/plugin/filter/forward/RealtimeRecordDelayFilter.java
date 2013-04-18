/***************************************************************************
 * Copyright 2013 Kieker Project (http://kieker-monitoring.net)
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

package kieker.analysis.plugin.filter.forward;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.OutputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.annotation.Property;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import kieker.common.record.IMonitoringRecord;

/**
 * Forwards incoming {@link IMonitoringRecord}s with delays computed from the {@link kieker.common.record.IMonitoringRecord#getLoggingTimestamp()} value
 * (assumed to be in the configured resolution). For example, after initialization, if records with logging timestamps 3000 and 4500 nanos are received, the
 * first record is forwarded immediately; the second will be forwarded 1500 nanos later.
 * 
 * @author Andre van Hoorn, Robert von Massow, Jan Waller
 * 
 * @since 1.6
 */
@Plugin(
		description = "Forwards incoming records with delays computed from the timestamp values",
		outputPorts = {
			@OutputPort(name = RealtimeRecordDelayFilter.OUTPUT_PORT_NAME_RECORDS, eventTypes = { IMonitoringRecord.class },
					description = "Outputs the delayed records")
		},
		configuration = {
			@Property(name = RealtimeRecordDelayFilter.CONFIG_PROPERTY_NAME_NUM_WORKERS, defaultValue = "1"),
			@Property(name = RealtimeRecordDelayFilter.CONFIG_PROPERTY_NAME_ADDITIONAL_SHUTDOWN_DELAY_SECONDS, defaultValue = "5"),
			@Property(name = RealtimeRecordDelayFilter.CONFIG_PROPERTY_NAME_WARN_NEGATIVE_DELAY_SECONDS, defaultValue = "2"),
			@Property(name = RealtimeRecordDelayFilter.CONFIG_PROPERTY_NAME_TIMER, defaultValue = "MILLISECONDS")
		})
public class RealtimeRecordDelayFilter extends AbstractFilterPlugin {

	public static final String INPUT_PORT_NAME_RECORDS = "inputRecords";
	public static final String OUTPUT_PORT_NAME_RECORDS = "outputRecords";

	/**
	 * The number of threads to be used for the internal {@link java.util.concurrent.ThreadPoolExecutor}, processing the scheduled {@link IMonitoringRecord}s.
	 */
	public static final String CONFIG_PROPERTY_NAME_NUM_WORKERS = "numWorkers";

	/**
	 * The number of additional seconds to wait before execute the termination (after all records have been forwarded).
	 */
	public static final String CONFIG_PROPERTY_NAME_ADDITIONAL_SHUTDOWN_DELAY_SECONDS = "additionalShutdownDelaySeconds";

	/**
	 * The number of seconds of negative scheduling time that produces a warning.
	 */
	public static final String CONFIG_PROPERTY_NAME_WARN_NEGATIVE_DELAY_SECONDS = "warnOnNegativeSchedTimeSeconds";

	/**
	 * The precision of the used timer (MILLISECONDS or NANOSECONDS).
	 */
	public static final String CONFIG_PROPERTY_NAME_TIMER = "timerPrecision";

	private static final Log LOG = LogFactory.getLog(RealtimeRecordDelayFilter.class);

	private final TimeUnit timeunit;

	private final TimerWithPrecision timer;

	private final long warnOnNegativeSchedTime;

	private final int numWorkers;

	private final ScheduledThreadPoolExecutor executor;
	private final long shutdownDelay;

	private volatile long startTime = -1;
	private volatile long firstLoggingTimestamp;

	private volatile long latestSchedulingTime = -1;

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param configuration
	 *            The configuration for this component.
	 * @param projectContext
	 *            The project context for this component.
	 */
	public RealtimeRecordDelayFilter(final Configuration configuration, final IProjectContext projectContext) {
		super(configuration, projectContext);

		final String recordTimeunitProperty = projectContext.getProperty(IProjectContext.CONFIG_PROPERTY_NAME_RECORDS_TIME_UNIT);
		TimeUnit recordTimeunit;
		try {
			recordTimeunit = TimeUnit.valueOf(recordTimeunitProperty);
		} catch (final IllegalArgumentException ex) { // already caught in AnalysisController, should never happen
			LOG.warn(recordTimeunitProperty + " is no valid TimeUnit! Using NANOSECONDS instead.");
			recordTimeunit = TimeUnit.NANOSECONDS;
		}
		this.timeunit = recordTimeunit;

		final String strTimer = configuration.getStringProperty(CONFIG_PROPERTY_NAME_TIMER);
		TimerWithPrecision tmpTimer;
		try {
			tmpTimer = TimerWithPrecision.valueOf(strTimer);
		} catch (final IllegalArgumentException ex) {
			LOG.warn(strTimer + " is no valid timer precision! Using MILLISECONDS instead.");
			tmpTimer = TimerWithPrecision.MILLISECONDS;
		}
		this.timer = tmpTimer;

		this.warnOnNegativeSchedTime = this.timeunit.convert(this.configuration.getLongProperty(CONFIG_PROPERTY_NAME_WARN_NEGATIVE_DELAY_SECONDS), TimeUnit.SECONDS);

		this.numWorkers = configuration.getIntProperty(CONFIG_PROPERTY_NAME_NUM_WORKERS);
		this.shutdownDelay = this.timeunit.convert(this.configuration.getLongProperty(CONFIG_PROPERTY_NAME_ADDITIONAL_SHUTDOWN_DELAY_SECONDS), TimeUnit.SECONDS);

		this.executor = new ScheduledThreadPoolExecutor(this.numWorkers);
		this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(true);
		this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
	}

	@InputPort(name = INPUT_PORT_NAME_RECORDS, eventTypes = { IMonitoringRecord.class }, description = "Receives the records to be delayed")
	public final void inputRecord(final IMonitoringRecord monitoringRecord) {
		final long currentTime = this.timer.getCurrentTime(this.timeunit);

		synchronized (this) {
			if (this.startTime == -1) { // init on first record
				this.firstLoggingTimestamp = monitoringRecord.getLoggingTimestamp();
				this.startTime = currentTime;
			}

			// Compute scheduling time
			long schedTimeFromNow = (monitoringRecord.getLoggingTimestamp() - this.firstLoggingTimestamp) // relative to 1st record
					- (currentTime - this.startTime); // substract elapsed time
			if (schedTimeFromNow < -this.warnOnNegativeSchedTime) {
				final long schedTimeSeconds = TimeUnit.SECONDS.convert(schedTimeFromNow, this.timeunit);
				LOG.warn("negative scheduling time: " + schedTimeFromNow + " (" + this.timeunit.toString() + ") / " + schedTimeSeconds
						+ " (seconds)-> scheduling with a delay of 0");
			}
			if (schedTimeFromNow < 0) {
				schedTimeFromNow = 0; // i.e., schedule immediately
			}

			final long absSchedTime = currentTime + schedTimeFromNow;
			if (absSchedTime > this.latestSchedulingTime) {
				this.latestSchedulingTime = absSchedTime;
			}

			// Schedule
			this.executor.schedule(new Runnable() {

				public void run() {
					RealtimeRecordDelayFilter.this.deliverIndirect(OUTPUT_PORT_NAME_RECORDS, monitoringRecord);
				}
			}, schedTimeFromNow, this.timeunit);
		}
	}

	final boolean deliverIndirect(final String outputPortName, final Object data) { // NOPMD package for inner class
		return this.deliver(outputPortName, data);
	}

	@Override
	public void terminate(final boolean error) {
		this.executor.shutdown();

		if (!error) {
			long shutdownDelaySecondsFromNow =
					TimeUnit.SECONDS.convert((this.latestSchedulingTime - this.timer.getCurrentTime(this.timeunit)) + this.shutdownDelay, this.timeunit);
			if (shutdownDelaySecondsFromNow < 0) {
				shutdownDelaySecondsFromNow = 0;
			}
			try {
				LOG.info("Awaiting termination delay of " + shutdownDelaySecondsFromNow + " seconds ...");
				if (!this.executor.awaitTermination(shutdownDelaySecondsFromNow, TimeUnit.SECONDS)) {
					LOG.error("Termination delay triggerred before all scheduled records sent");
				}
			} catch (final InterruptedException e) {
				LOG.error("Interrupted while awaiting termination delay", e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Configuration getCurrentConfiguration() {
		final Configuration configuration = new Configuration();
		configuration.setProperty(CONFIG_PROPERTY_NAME_NUM_WORKERS, Integer.toString(this.numWorkers));
		configuration
				.setProperty(CONFIG_PROPERTY_NAME_ADDITIONAL_SHUTDOWN_DELAY_SECONDS, Long.toString(TimeUnit.SECONDS.convert(this.shutdownDelay, this.timeunit)));
		return configuration;
	}

	/**
	 * @author Jan Waller
	 */
	private static enum TimerWithPrecision {
		MILLISECONDS {
			@Override
			public long getCurrentTime(final TimeUnit timeunit) {
				return timeunit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			}

		},
		NANOSECONDS {
			@Override
			public long getCurrentTime(final TimeUnit timeunit) {
				return timeunit.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
			}
		};

		public abstract long getCurrentTime(TimeUnit timeunit);
	}
}
