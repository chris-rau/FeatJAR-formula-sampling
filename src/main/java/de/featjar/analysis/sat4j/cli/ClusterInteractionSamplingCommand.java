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
import de.featjar.formula.combination.LiteralSetsCombinationSpecification;
import de.featjar.formula.combination.MultiCombinationSpecification;
import de.featjar.formula.combination.VariableCombinationSpecification;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClusterInteractionSamplingCommand extends ASamplingAdditionCommand {

    public static final Option<Path> CLUSTER_INTERACTION_MAP = Option.newOption(
                    "cluster-interaction-map", Option.PathParser)
            .setRequired(false)
            .setDescription("Size of interactions to be covered in combination with a given cluster.")
            .setDefaultValue(null);

    /**
     * Value of t for general sampling.
     */
    public static final Option<Integer> T_OPTION = Option.newOption("t", Option.IntegerParser) //
            .setDescription("Value of general parameter t.")
            .setDefaultValue(2);

    @Override
    protected IComputation<BooleanAssignmentList> newComputation(OptionList optionParser) {
        BooleanAssignmentValueMap clusterInteractionMap = loadBooleanAssignmentValueMap(
                        optionParser, CLUSTER_INTERACTION_MAP)
                .orElseLog(Log.Verbosity.WARNING);

        BooleanAssignmentList featureModel = parseFeatureModel(optionParser).orElseLog(Log.Verbosity.ERROR);
        return createClusterInteractionSamplingComputation(
                featureModel, clusterInteractionMap, optionParser.get(T_OPTION), optionParser.get(ITERATIONS_OPTION));
    }

    public static List<ICombinationSpecification> createClusterInteractionCombinationSpecifications(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap clusterInteractionMap) {
        // gather all possible literals in the model
        BooleanAssignment featureModelVariables = featureModel.getVariableMap().getVariables();
        BooleanAssignment featureModelLiterals = featureModelVariables.addAll(featureModelVariables.inverse());

        List<ICombinationSpecification> combinationsList = new ArrayList<>();

        for (BooleanAssignment cluster : clusterInteractionMap.getAssignments()) {
            int weight = clusterInteractionMap.getValue(cluster);
            int[] tValues = new int[] {weight - 1, cluster.size()};
            BooleanAssignment allWithoutCluster = featureModelLiterals.removeAllVariables(cluster);
            combinationsList.add(new LiteralSetsCombinationSpecification(
                    tValues, new BooleanAssignmentList(featureModel.getVariableMap(), allWithoutCluster, cluster)));
        }

        return combinationsList;
    }

    public IComputation<BooleanAssignmentList> createClusterInteractionSamplingComputation(
            BooleanAssignmentList featureModel,
            BooleanAssignmentValueMap clusterInteractionMap,
            int t,
            int iterations) {
        adaptFeatureModelToBooleanAssignmentValueMap(featureModel, clusterInteractionMap);

        List<ICombinationSpecification> combinationsList =
                createClusterInteractionCombinationSpecifications(featureModel, clusterInteractionMap);
        // add the regular t-wise sampling on top
        // Todo: optimization possible ?
        combinationsList.add(new VariableCombinationSpecification(t, featureModel.getVariableMap()));

        return Computations.of(featureModel)
                .map(YASA::new)
                .set(YASA.COMBINATION_SET, new MultiCombinationSpecification(combinationsList))
                .set(YASA.ITERATIONS, iterations);
    }

    public Result<BooleanAssignmentList> computeSample(
            BooleanAssignmentList featureModel,
            BooleanAssignmentValueMap clusterInteractionMap,
            int t,
            int iterations) {
        return createClusterInteractionSamplingComputation(featureModel, clusterInteractionMap, t, iterations)
                .computeResult();
    }

    @Override
    public Optional<String> getDescription() {
        return super.getDescription();
        // Todo
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("cluster-interaction-sampling");
    }
}
