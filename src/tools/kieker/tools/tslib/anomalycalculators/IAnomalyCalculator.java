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

package kieker.tools.tslib.anomalycalculators;

import kieker.tools.tslib.ITimeSeriesPoint;
import kieker.tools.tslib.forecast.IForecastResult;

/**
 * 
 * @author Tillmann Carlos Bielefeld
 * 
 * @since 1.9
 * 
 * @param <T>
 *            The type of the calculator.
 */
public interface IAnomalyCalculator<T> {

	/**
	 * @since 1.9
	 */
	public AnomalyScore calculateAnomaly(IForecastResult<T> forecast, ITimeSeriesPoint<T> current);

}
