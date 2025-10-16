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
package de.featjar.analysis.sat4j.io;

import de.featjar.base.data.RangeMap;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.base.io.input.StringInputMapper;
import de.featjar.formula.VariableMap;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.io.textual.BooleanAssignmentValueMapFormat;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanAssignmentValueMapFormatTest {

    @Test
    public void testBooleanAssignmentValueMapFormat() {
        String testString = "feature1,-feature2,feature3,+feature4=1\n"
                + "--feature1,++feature2,-feature3,feature4=2\n"
                + "feature5=4";
        AInputMapper inputMapper = new StringInputMapper(testString, Charset.defaultCharset(), ".txt");
        BooleanAssignmentValueMap valueMap =
                new BooleanAssignmentValueMapFormat().parse(inputMapper).get();
        System.out.println(valueMap);

        Assertions.assertEquals(3, valueMap.size());
        VariableMap variableMap = valueMap.getVariableMap();
        Assertions.assertEquals(7, variableMap.size());
        Assertions.assertTrue(variableMap.containsAllObjects(new RangeMap<>(
                List.of("feature1", "feature2", "feature3", "feature4", "feature5", "-feature1", "+feature2"))));
        String serialized =
                new BooleanAssignmentValueMapFormat().serialize(valueMap).get();
        System.out.println(serialized);
        Assertions.assertTrue(serialized.contains("+feature1,-feature2,+feature3,+feature4=1"));
        Assertions.assertTrue(serialized.contains("--feature1,++feature2,-feature3,+feature4=2"));
        Assertions.assertTrue(serialized.contains("+feature5=4"));
    }
}
