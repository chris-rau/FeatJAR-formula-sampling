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
package de.featjar.analysis.sat4j.cli;

import de.featjar.analysis.sat4j.computation.*;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Result;
import de.featjar.base.log.Log;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.combination.BooleanAssignmentListCombinationSpecification;
import de.featjar.formula.combination.ICombinationSpecification;
import de.featjar.formula.combination.MultiCombinationSpecification;
import de.featjar.formula.combination.VariableCombinationSpecification;
import de.featjar.formula.computation.ComputeRankedSample;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class PrioritizedSamplingCommand extends ASamplingAdditionCommand {

    public static final Option<Path> PRIORITY_MAP = Option.newOption("priority-map", Option.PathParser)
            .setRequired(false)
            .setDescription("Clusters with assigned priority values.")
            .setDefaultValue(null);

    /**
     * Value of t for general sampling.
     */
    public static final Option<Integer> T_OPTION = Option.newOption("t", Option.IntegerParser) //
            .setDescription("Value of general parameter t.")
            .setDefaultValue(2);

    @Override
    public IComputation<BooleanAssignmentList> newComputation(OptionList optionParser) {
        BooleanAssignmentValueMap priorityMap =
                loadBooleanAssignmentValueMap(optionParser, PRIORITY_MAP).orElseLog(Log.Verbosity.WARNING);

        BooleanAssignmentList featureModel = parseFeatureModel(optionParser).orElseLog(Log.Verbosity.ERROR);

        return createPrioritizedSamplingComputation(
                featureModel, priorityMap, optionParser.get(T_OPTION), optionParser.get(ITERATIONS_OPTION));
    }

    public IComputation<BooleanAssignmentList> createPrioritizedSamplingComputation(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap priorityMap, int t, int iterations) {
        adaptFeatureModelToBooleanAssignmentValueMap(featureModel, priorityMap);
        ICombinationSpecification combinationSpecification = new MultiCombinationSpecification(List.of(
                new BooleanAssignmentListCombinationSpecification(priorityMap.getBooleanAssignmentList()),
                new VariableCombinationSpecification(t, featureModel.getVariableMap())));

        return Computations.of(featureModel)
                .map(YASA::new)
                .set(YASA.COMBINATION_SET, combinationSpecification)
                .set(YASA.ITERATIONS, iterations)
                .map(ComputeRankedSample::new)
                .set(ComputeRankedSample.RANK_VALUES, priorityMap.toValuedBooleanAssignmentList());
    }

    public Result<BooleanAssignmentList> computeSample(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap priorityMap, int t, int iterations) {
        return createPrioritizedSamplingComputation(featureModel, priorityMap, t, iterations)
                .computeResult();
    }

    @Override
    public Optional<String> getDescription() {
        return super.getDescription();
        // Todo
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("prioritized-sampling");
    }
}
