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
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.combination.ICombinationSpecification;
import de.featjar.formula.combination.MultiCombinationSpecification;
import de.featjar.formula.combination.VariableCombinationSpecification;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WeightedSamplingCommand extends ASamplingAdditionCommand {

    public static final Option<Path> WEIGHT_MAP = Option.newOption("weight-map", Option.PathParser)
            .setDescription(
                    "Variables with assigned weight values. k-wise interactions are covered for variables with weight k.")
            .setDefaultValue(null);

    @Override
    public IComputation<BooleanAssignmentList> newComputation(OptionList optionParser) {
        BooleanAssignmentValueMap weightMap =
                loadBooleanAssignmentValueMap(optionParser, WEIGHT_MAP).orElseLog(Log.Verbosity.WARNING);

        BooleanAssignmentList featureModel = parseFeatureModel(optionParser).orElseLog(Log.Verbosity.ERROR);
        return createWeightedSamplingComputation(featureModel, weightMap, optionParser.get(ITERATIONS_OPTION));
    }

    public static List<ICombinationSpecification> createPriorityCombinationSpecifications(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap weightMap) {
        List<ICombinationSpecification> combinationSets = new ArrayList<>();
        for (BooleanAssignment weightAssignment : weightMap.getAssignments()) {
            int[] variables = weightAssignment.getAbsoluteValues(); // Todo: Use literals??
            int weight = weightMap.getValue(weightAssignment);
            combinationSets.add(new VariableCombinationSpecification(weight, variables, featureModel.getVariableMap()));
        }
        return combinationSets;
    }

    public IComputation<BooleanAssignmentList> createWeightedSamplingComputation(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap weightMap, int iterations) {
        adaptFeatureModelToBooleanAssignmentValueMap(featureModel, weightMap);

        List<ICombinationSpecification> combinationSets =
                createPriorityCombinationSpecifications(featureModel, weightMap);
        for (BooleanAssignment weightAssignment : weightMap.getAssignments()) {
            int[] variables = weightAssignment.getAbsoluteValues(); // Todo: Use literals??
            int weight = weightMap.getValue(weightAssignment);
            combinationSets.add(new VariableCombinationSpecification(weight, variables, featureModel.getVariableMap()));
        }

        return Computations.of(featureModel)
                .map(YASA::new)
                .set(YASA.COMBINATION_SET, new MultiCombinationSpecification(combinationSets))
                .set(YASA.ITERATIONS, iterations);
    }

    public Result<BooleanAssignmentList> computeSample(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap weightMap, int iterations) {
        return createWeightedSamplingComputation(featureModel, weightMap, iterations)
                .computeResult();
    }

    @Override
    public Optional<String> getDescription() {
        return super.getDescription();
        // Todo
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("weighted-sampling");
    }
}
