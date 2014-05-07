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

package kieker.panalysis.framework.core;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 * 
 * @param <T>
 *            The type of the pipe
 * @param <P>
 *            the extending pipe
 */
public abstract class AbstractPipe<T> implements IPipe<T> {

	private Runnable fireSignalClosing;
	private ISink<?> targetStage;

	public <S extends ISource, A extends T> void setSourcePort(final IOutputPort<S, T> sourcePort) {
		sourcePort.setAssociatedPipe(this);
	}

	public <S extends ISink<S>, A extends T> void setTargetPort(final IInputPort<S, T> targetPort) {
		targetPort.setAssociatedPipe(this);
		this.targetStage = targetPort.getOwningStage();
		this.fireSignalClosing = new Runnable() {
			// This Runnable avoids the declaration of targetStage and targetPort as attributes and the declaration of a type parameter
			public void run() {
				targetPort.getOwningStage().onSignalClosing(targetPort);
			}
		};
	}

	public ISink<?> getTargetStage() {
		return this.targetStage;
	}

	// BETTER remove if it does not add any new functionality
	protected abstract void putInternal(T token);

	public void put(final T token) {
		this.putInternal(token);
	}

	// BETTER remove if it does not add any new functionality
	protected abstract T tryTakeInternal();

	public final T tryTake() {
		return this.tryTakeInternal();
	}

	public void fireSignalClosing() {
		this.fireSignalClosing.run();
	}

}
