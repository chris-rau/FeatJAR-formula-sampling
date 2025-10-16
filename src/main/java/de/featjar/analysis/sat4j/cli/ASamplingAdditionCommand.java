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

import de.featjar.analysis.AAnalysisCommand;
import de.featjar.base.FeatJAR;
import de.featjar.base.cli.Option;
import de.featjar.base.cli.OptionList;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
import de.featjar.formula.io.textual.BooleanAssignmentValueMapFormat;
import de.featjar.formula.io.xml.XMLFeatureModelFormulaFormat;
import java.nio.file.Path;

// Todo: Combine all sampling additions
public abstract class ASamplingAdditionCommand extends AAnalysisCommand<BooleanAssignmentList> {

    /**
     * Path to the relevant feature model.
     */
    public static final Option<Path> FEATURE_MODEL = Option.newOption("feature-model", Option.PathParser)
            .setDescription("Feature model to sample.")
            .setDefaultValue(null);

    public static final Option<Integer> ITERATIONS_OPTION = Option.newOption("i", Option.IntegerParser) //
            .setDescription("Number of iterations.") //
            .setDefaultValue(1);

    protected static Result<BooleanAssignmentList> parseFeatureModel(OptionList optionParser) {
        Result<Path> featureModelPath = optionParser.getResult(FEATURE_MODEL);
        return IO.load(featureModelPath.get(), new XMLFeatureModelFormulaFormat())
                .toComputation()
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new)
                .computeResult();
    }

    protected static Result<BooleanAssignmentValueMap> loadBooleanAssignmentValueMap(
            OptionList optionParser, Option<Path> pathOption) {
        Result<Path> path = optionParser.getResult(pathOption);
        BooleanAssignmentValueMap map = new BooleanAssignmentValueMap(new VariableMap());
        if (path.isPresent()) {
            return IO.load(path.get(), new BooleanAssignmentValueMapFormat());
        }
        return Result.of(map);
    }

    protected static void adaptFeatureModelToBooleanAssignmentValueMap(
            BooleanAssignmentList featureModel, BooleanAssignmentValueMap booleanAssignmentValueMap) {
        if (!featureModel.getVariableMap().containsAllObjects(booleanAssignmentValueMap.getVariableMap())) {
            FeatJAR.log()
                    .error(() -> "VariableMap of BooleanAssignmentValueMap is not subset of feature model VariableMap");
        }
        featureModel.adapt(booleanAssignmentValueMap.getVariableMap());
    }
}
