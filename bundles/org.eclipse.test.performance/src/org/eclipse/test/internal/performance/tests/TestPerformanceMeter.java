/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others. 
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.internal.performance.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

/**
 * Mock performance meter that generates deterministic values for two dimensions.
 */
class TestPerformanceMeter extends InternalPerformanceMeter {

    private long fStartTime;
    private List<DataPoint> fDataPoints = new ArrayList<>();
    private Map<Dim, Scalar>  fStart      = new HashMap<>();
    private Map<Dim, Scalar>  fStop       = new HashMap<>();

    /**
     * @param scenarioId
     *            the scenario id
     */
    TestPerformanceMeter(String scenarioId) {
        super(scenarioId);
        fStartTime = System.currentTimeMillis();
    }

    void addPair(Dim dimension, long start, long end) {
        fStart.put(dimension, new Scalar(dimension, start));
        fStop.put(dimension, new Scalar(dimension, end));
    }

    @Override
    public void dispose() {
        fDataPoints = null;
        super.dispose();
    }

    @Override
    public Sample getSample() {
        if (fDataPoints != null)
            return new Sample(getScenarioName(), fStartTime, new HashMap<>(),
                    fDataPoints.toArray(new DataPoint[fDataPoints.size()]));
        return null;
    }

    @Override
    public void start() {
        fDataPoints.add(new DataPoint(BEFORE, fStart));
    }

    @Override
    public void stop() {
        fDataPoints.add(new DataPoint(AFTER, fStop));
    }
}
