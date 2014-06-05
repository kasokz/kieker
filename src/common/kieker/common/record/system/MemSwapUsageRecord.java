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

/**
 * 
 */
package kieker.common.record.system;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import kieker.common.record.AbstractMonitoringRecord;
import kieker.common.record.IMonitoringRecord;
import kieker.common.util.registry.IRegistry;

/**
 * @author Andre van Hoorn, Jan Waller
 * 
 * @since 1.3
 */
public class MemSwapUsageRecord extends AbstractMonitoringRecord implements IMonitoringRecord.Factory, IMonitoringRecord.BinaryFactory {
	public static final int SIZE = TYPE_SIZE_LONG + TYPE_SIZE_STRING + (6 * TYPE_SIZE_LONG);
	public static final Class<?>[] TYPES = {
		long.class,
		String.class,
		long.class,
		long.class,
		long.class,
		long.class,
		long.class,
		long.class,
	};

	/** A constant which can be used as a default value for non existing fields of the record. */
	public static final String DEFAULT_VALUE = "N/A";

	private static final long serialVersionUID = -2159074202253748453L;

	private final long memUsed;
	private final long memFree;
	private final long swapTotal;
	private final long swapUsed;
	private final long swapFree;

	/**
	 * Date/time of measurement.
	 */
	private final long timestamp;

	/**
	 * Name of the host, the resource belongs to.
	 */
	private final String hostname;

	private final long memTotal;

	/**
	 * Constructs a new {@link MemSwapUsageRecord} with the given values. If certain {@link String} values shall remain undefined, use the constant
	 * {@link #DEFAULT_VALUE}.
	 * 
	 * @param timestamp
	 *            The timestamp of the measure.
	 * @param hostname
	 *            The name of the host.
	 * @param memTotal
	 *            The total available memory.
	 * @param memUsed
	 *            The used memory.
	 * @param memFree
	 *            The free memory.
	 * @param swapTotal
	 *            The total available swap.
	 * @param swapUsed
	 *            The used swap.
	 * @param swapFree
	 *            The free swap.
	 */
	public MemSwapUsageRecord(final long timestamp, final String hostname, final long memTotal, final long memUsed, final long memFree, final long swapTotal,
			final long swapUsed, final long swapFree) {
		this.timestamp = timestamp;
		this.hostname = (hostname == null) ? DEFAULT_VALUE : hostname; // NOCS
		this.memTotal = memTotal;
		this.memUsed = memUsed;
		this.memFree = memFree;
		this.swapTotal = swapTotal;
		this.swapUsed = swapUsed;
		this.swapFree = swapFree;
	}

	/**
	 * This constructor converts the given array into a record. It is recommended to use the array which is the result of a call to {@link #toArray()}.
	 * 
	 * @param values
	 *            The values for the record.
	 */
	public MemSwapUsageRecord(final Object[] values) { // NOPMD (values stored directly)
		AbstractMonitoringRecord.checkArray(values, TYPES);
		this.timestamp = (Long) values[0];
		this.hostname = (String) values[1];
		this.memTotal = (Long) values[2];
		this.memUsed = (Long) values[3];
		this.memFree = (Long) values[4];
		this.swapTotal = (Long) values[5];
		this.swapUsed = (Long) values[6];
		this.swapFree = (Long) values[7];
	}

	/**
	 * This constructor converts the given array into a record.
	 * 
	 * @param buffer
	 *            The bytes for the record.
	 * 
	 * @throws BufferUnderflowException
	 *             if buffer not sufficient
	 */
	public MemSwapUsageRecord(final ByteBuffer buffer, final IRegistry<String> stringRegistry) throws BufferUnderflowException {
		this.timestamp = buffer.getLong();
		this.hostname = stringRegistry.get(buffer.getInt());
		this.memTotal = buffer.getLong();
		this.memUsed = buffer.getLong();
		this.memFree = buffer.getLong();
		this.swapTotal = buffer.getLong();
		this.swapUsed = buffer.getLong();
		this.swapFree = buffer.getLong();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] toArray() {
		return new Object[] { this.getTimestamp(), this.getHostname(), this.getMemTotal(), this.getMemUsed(), this.getMemFree(), this.getSwapTotal(),
			this.getSwapUsed(), this.getSwapFree(), };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeBytes(final ByteBuffer buffer, final IRegistry<String> stringRegistry) throws BufferOverflowException {
		buffer.putLong(this.getTimestamp());
		buffer.putInt(stringRegistry.get(this.getHostname()));
		buffer.putLong(this.getMemTotal());
		buffer.putLong(this.getMemUsed());
		buffer.putLong(this.getMemFree());
		buffer.putLong(this.getSwapTotal());
		buffer.putLong(this.getSwapUsed());
		buffer.putLong(this.getSwapFree());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated This record uses the {@link kieker.common.record.IMonitoringRecord.Factory} mechanism. Hence, this method is not implemented.
	 */
	@Override
	@Deprecated
	public final void initFromArray(final Object[] values) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated This record uses the {@link kieker.common.record.IMonitoringRecord.BinaryFactory} mechanism. Hence, this method is not implemented.
	 */
	@Override
	@Deprecated
	public final void initFromBytes(final ByteBuffer buffer, final IRegistry<String> stringRegistry) throws BufferUnderflowException {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] getValueTypes() {
		return TYPES; // NOPMD
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSize() {
		return SIZE;
	}

	public final long getMemTotal() {
		return this.memTotal;
	}

	public final long getMemUsed() {
		return this.memUsed;
	}

	public final long getMemFree() {
		return this.memFree;
	}

	public final long getSwapTotal() {
		return this.swapTotal;
	}

	public final long getSwapUsed() {
		return this.swapUsed;
	}

	public final long getSwapFree() {
		return this.swapFree;
	}

	public final long getTimestamp() {
		return this.timestamp;
	}

	public final String getHostname() {
		return this.hostname;
	}
}
