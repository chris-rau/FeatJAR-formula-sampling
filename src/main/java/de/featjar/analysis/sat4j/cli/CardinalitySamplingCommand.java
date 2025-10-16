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
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignment;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.combination.ICombinationSpecification;
import de.featjar.formula.combination.LiteralSetsCombinationSpecification;
import de.featjar.formula.combination.MultiCombinationSpecification;
import de.featjar.formula.combination.VariableCombinationSpecification;
import de.featjar.formula.computation.ComputeProjectedSample;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

public class CardinalitySamplingCommand extends ASamplingAdditionCommand {

    public static class CardinalityCombinationSpecificationsWrapper {
        private final List<ICombinationSpecification> combinationsList;
        private final VariableMap newVariableMap;
        private final int[] artificialVariables;

        public CardinalityCombinationSpecificationsWrapper(
                List<ICombinationSpecification> combinationsList,
                VariableMap newVariableMap,
                int[] artificialVariables) {
            this.combinationsList = combinationsList;
            this.newVariableMap = newVariableMap;
            this.artificialVariables = artificialVariables;
        }

        public List<ICombinationSpecification> getCombinationsList() {
            return combinationsList;
        }

        public VariableMap getNewVariableMap() {
            return newVariableMap;
        }

        public int[] getArtificialVariables() {
            return artificialVariables;
        }
    }

    public static final Option<Path> CARDINALITY_MAP = Option.newOption("cardinality-map", Option.PathParser)
            .setDescription("How often a cluster must at least appear in the sample.")
            .setDefaultValue(null);

    /**
     * Value of t for general sampling.
     */
    public static final Option<Integer> T_OPTION = Option.newOption("t", Option.IntegerParser) //
            .setDescription("Value of general parameter t.")
            .setDefaultValue(2);

    /**
     * Ensures that clusters are included in the sample multiple times, according to the given cardinality map.
     * @param optionParser
     * @return
     */
    @Override
    protected IComputation<BooleanAssignmentList> newComputation(OptionList optionParser) {
        BooleanAssignmentValueMap cardinalityMap =
                loadBooleanAssignmentValueMap(optionParser, CARDINALITY_MAP).orElseLog(Log.Verbosity.WARNING);

        BooleanAssignmentList featureModel = parseFeatureModel(optionParser).orElseLog(Log.Verbosity.ERROR);
        return createCardinalitySamplingComputation(
                featureModel, cardinalityMap, optionParser.get(T_OPTION), optionParser.get(ITERATIONS_OPTION));
    }

    public static CardinalityCombinationSpecificationsWrapper createCardinalityCombinationSpecifications(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap cardinalityMap) {
        List<ICombinationSpecification> combinationsList = new ArrayList<>();

        // calculate the maximum cardinality to know how many artificial variables are necessary
        int maxCardinality = StreamSupport.stream(cardinalityMap.spliterator(), false)
                .map(Map.Entry::getValue)
                .max(Integer::compareTo)
                .orElse(0);

        VariableMap oldVariableMap = featureModel.getVariableMap();
        VariableMap newVariableMap = oldVariableMap.clone();

        int[] artificialVariables = new int[maxCardinality];
        for (int i = 0; i < maxCardinality; i++) {
            artificialVariables[i] = newVariableMap.add(UUID.randomUUID().toString());
        }

        // for each cluster add a LiteralSetsCombinationSpecification that covers the combination of two sets of
        // literals:
        // 1. |Cluster| literals of Cluster (all literals)
        // 2. 1 literal of the first c artificial variables where c is the cardinality of the cluster
        for (BooleanAssignment cluster : cardinalityMap.getAssignments()) {
            int cardinality = cardinalityMap.getValue(cluster);
            if (cardinality <= 0) {
                continue;
            }

            BooleanAssignment artificials =
                    new BooleanAssignment(Arrays.copyOfRange(artificialVariables, 0, cardinality));
            int[] tValues = new int[] {cluster.size(), 1};
            combinationsList.add(new LiteralSetsCombinationSpecification(
                    tValues, new BooleanAssignmentList(oldVariableMap, cluster, artificials)));
        }
        return new CardinalityCombinationSpecificationsWrapper(combinationsList, newVariableMap, artificialVariables);
    }

    public IComputation<BooleanAssignmentList> createCardinalitySamplingComputation(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap cardinalityMap, int t, int iterations) {
        adaptFeatureModelToBooleanAssignmentValueMap(featureModel, cardinalityMap);
        VariableMap oldVariableMap = featureModel.getVariableMap();
        CardinalityCombinationSpecificationsWrapper combinationsWrapper =
                createCardinalityCombinationSpecifications(featureModel, cardinalityMap);
        List<ICombinationSpecification> combinationsList = combinationsWrapper.getCombinationsList();
        VariableMap newVariableMap = combinationsWrapper.getNewVariableMap();
        int[] artificialVariables = combinationsWrapper.getArtificialVariables();
        // add the regular t-wise sampling on top
        // Todo: optimization possible by removing single feature entries of cardinality map
        combinationsList.add(new VariableCombinationSpecification(t, oldVariableMap));

        featureModel.adapt(newVariableMap);

        return Computations.of(featureModel)
                .map(YASA::new)
                .set(YASA.COMBINATION_SET, new MultiCombinationSpecification(combinationsList))
                .set(YASA.ITERATIONS, iterations)
                .map(ComputeProjectedSample::new)
                .set(ComputeProjectedSample.ADAPT_VARIABLE_MAP, Boolean.TRUE)
                .set(ComputeProjectedSample.EXCLUDE_VARIABLES, new BooleanAssignment(artificialVariables));
    }

    public Result<BooleanAssignmentList> computeSample(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap cardinalityMap, int t, int iterations) {
        return createCardinalitySamplingComputation(featureModel, cardinalityMap, t, iterations)
                .computeResult();
    }

    @Override
    public Optional<String> getDescription() {
        // todo
        return super.getDescription();
    }

    @Override
    public Optional<String> getShortName() {
        return Optional.of("cardinality-sampling");
    }
}
