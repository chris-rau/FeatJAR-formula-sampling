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

import de.featjar.base.computation.Computations;
import de.featjar.base.data.Result;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.feature.model.transformer.ComputeFormula;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;

public abstract class ASamplingAddition {

    int iterations = 1;

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    abstract Result<BooleanAssignmentList> computeSample(BooleanAssignmentList featureModel);

    public Result<BooleanAssignmentList> computeSample(IFeatureModel featureModel) {
        return computeSample(Computations.of(featureModel)
                .map(ComputeFormula::new)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new)
                .computeResult()
                .orElseThrow());
    }
}
