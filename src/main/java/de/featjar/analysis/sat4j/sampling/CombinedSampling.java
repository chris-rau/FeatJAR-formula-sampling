/*
 * Copyright (C) 2025 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-formula-sampling.
 *
 * formula-sampling is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula-sampling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula-sampling. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/chris-rau/formula-sampling> for further information.
 */
package de.featjar.analysis.sat4j.sampling;

import de.featjar.analysis.sat4j.cli.CombinedSamplingCommand;
import de.featjar.base.data.Result;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;

public class CombinedSampling extends ASamplingAddition {

    private final int t;
    private final BooleanAssignmentValueMap priorityMap;
    private final BooleanAssignmentValueMap cardinalityMap;
    private final BooleanAssignmentValueMap clusterInteractionMap;
    private final BooleanAssignmentValueMap weightMap;

    public CombinedSampling(CombinedSamplingBuilder combinedSamplingBuilder) {
        this.t = combinedSamplingBuilder.t;
        this.iterations = combinedSamplingBuilder.iterations;
        this.priorityMap = combinedSamplingBuilder.priorityMap;
        this.cardinalityMap = combinedSamplingBuilder.cardinalityMap;
        this.clusterInteractionMap = combinedSamplingBuilder.clusterInteractionMap;
        this.weightMap = combinedSamplingBuilder.weightMap;
    }

    @Override
    public Result<BooleanAssignmentList> computeSample(BooleanAssignmentList featureModel) {
        return new CombinedSamplingCommand()
                .computeSample(
                        featureModel, clusterInteractionMap, priorityMap, weightMap, cardinalityMap, t, iterations);
    }

    public static class CombinedSamplingBuilder {
        private int t = 1;
        private int iterations = 1;
        private BooleanAssignmentValueMap priorityMap = new BooleanAssignmentValueMap(new VariableMap());
        private BooleanAssignmentValueMap cardinalityMap = new BooleanAssignmentValueMap(new VariableMap());
        private BooleanAssignmentValueMap clusterInteractionMap = new BooleanAssignmentValueMap(new VariableMap());
        private BooleanAssignmentValueMap weightMap = new BooleanAssignmentValueMap(new VariableMap());

        public CombinedSamplingBuilder setT(int t) {
            this.t = t;
            return this;
        }

        public CombinedSamplingBuilder setIterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        public CombinedSamplingBuilder setPriorityMap(BooleanAssignmentValueMap priorityMap) {
            this.priorityMap = priorityMap;
            return this;
        }

        public CombinedSamplingBuilder setCardinalityMap(BooleanAssignmentValueMap cardinalityMap) {
            this.cardinalityMap = cardinalityMap;
            return this;
        }

        public CombinedSamplingBuilder setClusterInteractionMap(BooleanAssignmentValueMap clusterInteractionMap) {
            this.clusterInteractionMap = clusterInteractionMap;
            return this;
        }

        public CombinedSamplingBuilder setWeightMap(BooleanAssignmentValueMap weightMap) {
            this.weightMap = weightMap;
            return this;
        }

        public CombinedSampling build() {
            return new CombinedSampling(this);
        }
    }
}
