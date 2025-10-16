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

import de.featjar.analysis.sat4j.computation.YASA;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.computation.Computations;
import de.featjar.base.computation.IComputation;
import de.featjar.base.data.Result;
import de.featjar.base.log.Log;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.combination.ICombinationSpecification;
import de.featjar.formula.combination.MultiCombinationSpecification;
import de.featjar.formula.combination.VariableCombinationSpecification;
import de.featjar.formula.computation.ComputeProjectedSample;
import de.featjar.formula.computation.ComputeRankedSample;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CombinedSamplingCommand extends ASamplingAdditionCommand {

    public static final Option<Path> PRIORITY_MAP = Option.newOption("priority-map", Option.PathParser)
            .setDescription("Clusters with assigned priority values.")
            .setDefaultValue(null);

    public static final Option<Path> CARDINALITY_MAP = Option.newOption("cardinality-map", Option.PathParser)
            .setDescription("How often a cluster must at least appear in the sample.")
            .setDefaultValue(null);

    public static final Option<Path> CLUSTER_INTERACTION_MAP = Option.newOption(
                    "cluster-interaction-map", Option.PathParser)
            .setDescription("Size of interactions to be covered in combination with a given cluster.")
            .setDefaultValue(null);

    public static final Option<Path> WEIGHT_MAP = Option.newOption("weight-map", Option.PathParser)
            .setDescription(
                    "Variables with assigned weight values. k-wise interactions are covered for variables with weight k.")
            .setDefaultValue(null);

    /**
     * Value of t for general sampling.
     */
    public static final Option<Integer> DEFAULT_T_OPTION = Option.newOption("t", Option.IntegerParser) //
            .setDescription("Value of general parameter t.")
            .setDefaultValue(1);

    @Override
    protected IComputation<BooleanAssignmentList> newComputation(OptionList optionParser) {
        BooleanAssignmentValueMap cardinalityMap = loadBooleanAssignmentValueMap(optionParser, CLUSTER_INTERACTION_MAP)
                .orElseLog(Log.Verbosity.WARNING);
        BooleanAssignmentValueMap clusterInteractionMap =
                loadBooleanAssignmentValueMap(optionParser, CARDINALITY_MAP).orElseLog(Log.Verbosity.WARNING);
        BooleanAssignmentValueMap priorityMap =
                loadBooleanAssignmentValueMap(optionParser, PRIORITY_MAP).orElseLog(Log.Verbosity.WARNING);
        BooleanAssignmentValueMap weightMap =
                loadBooleanAssignmentValueMap(optionParser, WEIGHT_MAP).orElseLog(Log.Verbosity.WARNING);

        BooleanAssignmentList featureModel = parseFeatureModel(optionParser).orElseLog(Log.Verbosity.ERROR);

        return createCombinedSamplingComputation(
                featureModel,
                clusterInteractionMap,
                priorityMap,
                weightMap,
                cardinalityMap,
                optionParser.get(DEFAULT_T_OPTION),
                optionParser.get(ITERATIONS_OPTION));
    }

    public IComputation<BooleanAssignmentList> createCombinedSamplingComputation(
            BooleanAssignmentList featureModel,
            BooleanAssignmentValueMap clusterInteractionMap,
            BooleanAssignmentValueMap priorityMap,
            BooleanAssignmentValueMap weightMap,
            BooleanAssignmentValueMap cardinalityMap,
            int t,
            int iterations) {
        List<ICombinationSpecification> combinationsList = new ArrayList<>();
        combinationsList.add(new VariableCombinationSpecification(t, featureModel.getVariableMap()));

        clusterInteractionMap.adapt(featureModel.getVariableMap(), true);
        combinationsList.addAll(ClusterInteractionSamplingCommand.createClusterInteractionCombinationSpecifications(
                featureModel, clusterInteractionMap));

        priorityMap.adapt(featureModel.getVariableMap(), true);
        combinationsList.add(PrioritizedSamplingCommand.createPriorityCombinationSpecification(priorityMap));

        weightMap.adapt(featureModel.getVariableMap(), true);
        combinationsList.addAll(
                WeightedSamplingCommand.createPriorityCombinationSpecifications(featureModel, weightMap));

        cardinalityMap.adapt(featureModel.getVariableMap(), true);
        CardinalitySamplingCommand.CardinalityCombinationSpecificationsWrapper cardinalityWrapper =
                CardinalitySamplingCommand.createCardinalityCombinationSpecifications(featureModel, cardinalityMap);
        List<ICombinationSpecification> cardinalityCombinations = cardinalityWrapper.getCombinationsList();
        VariableMap newVariableMap = cardinalityWrapper.getNewVariableMap();
        int[] artificialVariables = cardinalityWrapper.getArtificialVariables();
        featureModel.adapt(newVariableMap);
        return Computations.of(featureModel)
                .map(YASA::new)
                .set(YASA.COMBINATION_SET, new MultiCombinationSpecification(combinationsList))
                .set(YASA.ITERATIONS, iterations)
                .map(ComputeProjectedSample::new)
                .set(ComputeProjectedSample.ADAPT_VARIABLE_MAP, Boolean.TRUE)
                .set(ComputeProjectedSample.EXCLUDE_VARIABLES, new BooleanAssignment(artificialVariables))
                .map(ComputeRankedSample::new)
                .set(ComputeRankedSample.RANK_VALUES, priorityMap.toValuedBooleanAssignmentList());
    }

    public Result<BooleanAssignmentList> computeSample(
            BooleanAssignmentList featureModel,
            BooleanAssignmentValueMap clusterInteractionMap,
            BooleanAssignmentValueMap priorityMap,
            BooleanAssignmentValueMap weightMap,
            BooleanAssignmentValueMap cardinalityMap,
            int t,
            int iterations) {
        return createCombinedSamplingComputation(
                        featureModel, clusterInteractionMap, priorityMap, weightMap, cardinalityMap, t, iterations)
                .computeResult();
    }
}
