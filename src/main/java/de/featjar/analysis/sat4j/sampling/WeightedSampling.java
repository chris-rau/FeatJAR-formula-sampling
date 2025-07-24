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

import de.featjar.analysis.sat4j.cli.WeightedSamplingCommand;
import de.featjar.base.data.Result;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;

public class WeightedSampling extends ASamplingAddition {

    private final BooleanAssignmentValueMap weightMap;

    public WeightedSampling(BooleanAssignmentValueMap weightMap) {
        this.weightMap = weightMap;
    }

    @Override
    public Result<BooleanAssignmentList> computeSample(BooleanAssignmentList featureModel) {
        return new WeightedSamplingCommand().computeSample(featureModel, weightMap, iterations);
    }
}
