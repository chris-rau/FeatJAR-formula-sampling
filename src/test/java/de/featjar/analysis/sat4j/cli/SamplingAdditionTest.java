package de.featjar.analysis.sat4j.cli;

import de.featjar.analysis.sat4j.computation.ComputeAtomicSetsSAT4J;
import de.featjar.analysis.sat4j.sampling.CardinalitySampling;
import de.featjar.analysis.sat4j.sampling.PrioritizedSampling;
import de.featjar.base.FeatJAR;
import de.featjar.base.computation.Computations;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.base.io.input.StringInputMapper;
import de.featjar.formula.assignment.BooleanAssignmentList;
import de.featjar.formula.assignment.BooleanAssignmentValueMap;
import de.featjar.formula.assignment.ComputeBooleanClauseList;
import de.featjar.formula.computation.ComputeCNFFormula;
import de.featjar.formula.computation.ComputeNNFFormula;
import de.featjar.formula.io.textual.BooleanAssignmentValueMapFormat;
import de.featjar.formula.structure.IFormula;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static de.featjar.Common.loadFormula;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SamplingAdditionTest {

    @BeforeAll
    public static void begin() {
        FeatJAR.testConfiguration().initialize();
    }

    @AfterAll
    public static void end() {
        FeatJAR.deinitialize();
    }

    public static BooleanAssignmentList loadFeatureModel() {
        return Computations.of(loadFormula("GPL/model.xml"))
                .cast(IFormula.class)
                .map(ComputeNNFFormula::new)
                .map(ComputeCNFFormula::new)
                .map(ComputeBooleanClauseList::new)
                .compute();
    }


    @Test
    public void testPrioritizedSampling() {

//        String priorityMapString = "-DFS,Directed=2\nCycle,-MSTPrim,MSTKruskal=1";
        String priorityMapString = "Cycle,MSTPrim,MSTKruskal=0\n" +
                "Cycle,MSTPrim,-MSTKruskal=1\n" +
                "Cycle,-MSTPrim,MSTKruskal=2\n" +
                "Cycle,-MSTPrim,-MSTKruskal=3\n" +
                "-Cycle,MSTPrim,MSTKruskal=4\n" +
                "-Cycle,MSTPrim,-MSTKruskal=5\n" +
                "-Cycle,-MSTPrim,MSTKruskal=6\n" +
                "-Cycle,-MSTPrim,-MSTKruskal=7\n" +
                "BFS,-Weighted,Undirected,OnlyVertices=8"; // 21,-29,33,35
        AInputMapper inputMapper = new StringInputMapper(priorityMapString, Charset.defaultCharset(), ".txt");

        BooleanAssignmentValueMap priorityMap = new BooleanAssignmentValueMapFormat().parse(inputMapper).get();

        BooleanAssignmentList featureModel = loadFeatureModel();

        BooleanAssignmentList sample = new PrioritizedSampling(2, priorityMap).computeSample(featureModel).get();
        System.out.println(sample);
    }

    @Test
    public void testCardinalitySampling() {

        String cardinalityMapString = "Number,Connected,Cycle=3"; // 12, 13, 17
        AInputMapper inputMapper = new StringInputMapper(cardinalityMapString, Charset.defaultCharset(), ".txt");

        BooleanAssignmentValueMap cardinalityMap = new BooleanAssignmentValueMapFormat().parse(inputMapper).get();

        BooleanAssignmentList featureModel = loadFeatureModel();

        BooleanAssignmentList sample = new CardinalitySampling(2, cardinalityMap).computeSample(featureModel).get();
        System.out.println(sample);
    }


}
