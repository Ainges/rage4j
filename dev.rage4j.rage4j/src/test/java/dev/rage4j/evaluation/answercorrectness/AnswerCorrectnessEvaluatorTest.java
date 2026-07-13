package dev.rage4j.evaluation.answercorrectness;

import dev.rage4j.LoggingTestWatcher;
import dev.rage4j.evaluation.Evaluation;
import dev.rage4j.evaluation.model.ClaimClassification;
import dev.rage4j.model.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class AnswerCorrectnessEvaluatorTest
{
	private static final String GROUND_TRUTH = "Paris is the capital of France.";
	private static final String ANSWER = "Paris is the capital of France and the largest city in France.";

	private AnswerCorrectnessEvaluator evaluator;
	private AnswerCorrectnessBot mockBot;
	private Sample sample;

	@BeforeEach
	void setUp()
	{
		mockBot = mock(AnswerCorrectnessBot.class);
		evaluator = new AnswerCorrectnessEvaluator(mockBot);

		sample = Sample.builder()
			.withGroundTruth(GROUND_TRUTH)
			.withAnswer(ANSWER)
			.build();
	}

	static Stream<Arguments> evaluateCorrectnessTestCases()
	{
		return Stream.of(
			Arguments.of(
				new String[] { "Paris is the capital of France" },
				new String[] { "Paris is the largest city in France" },
				new String[0],
				0.6667, "true and false positives"),
			Arguments.of(new String[0], new String[0], new String[0], 0.0, "no true/false positives or negatives"),
			Arguments.of(new String[] { "Paris is the capital of France" }, new String[0], new String[0], 1.0, "only true positives"));
	}

	@ParameterizedTest(name = "evaluates correctly with {4}")
	@MethodSource("evaluateCorrectnessTestCases")
	void testEvaluateCorrectness(String[] truePositives, String[] falsePositives, String[] falseNegatives,
		double expectedScore, String scenario)
	{
		when(mockBot.classifyClaims(GROUND_TRUTH, ANSWER))
			.thenReturn(new ClaimClassification(truePositives, falsePositives, falseNegatives));

		Evaluation result = evaluator.evaluate(sample);

		assertEquals("Answer correctness", result.getName());
		assertEquals(expectedScore, result.getValue(), 0.001);
	}

	@Test
	void testClaimReportedAsTruePositiveAndFalseNegativeIsNotDoubleCounted()
	{
		// The prompt instructs the LLM to assign every claim to exactly one
		// list, but nothing structurally prevents it from repeating a true
		// positive as a false negative. That is contradictory: a fact cannot
		// be present in the answer (TP) and missing from it (FN) at the same
		// time.
		when(mockBot.classifyClaims(GROUND_TRUTH, ANSWER))
			.thenReturn(new ClaimClassification(
				new String[] { "Paris is the capital of France", "Paris is in France" },
				new String[0],
				new String[] { "Paris is in France" }));

		Evaluation result = evaluator.evaluate(sample);

		// "Paris is in France" is already counted as a true positive, so the
		// contradictory false negative must not lower the score:
		// expected F1 = 1.0, not 2 / (2 + 0.5) = 0.8.
		assertEquals(1.0, result.getValue(), 0.001);
	}

	@Test
	void testClaimReportedAsTruePositiveAndFalsePositiveIsNotDoubleCounted()
	{
		when(mockBot.classifyClaims(GROUND_TRUTH, ANSWER))
			.thenReturn(new ClaimClassification(
				new String[] { "Paris is the capital of France" },
				new String[] { "Paris is the capital of France" },
				new String[0]));

		Evaluation result = evaluator.evaluate(sample);

		// The contradictory false positive is already a true positive and must
		// not lower the score: expected F1 = 1.0, not 1 / (1 + 0.5) = 0.6667.
		assertEquals(1.0, result.getValue(), 0.001);
	}

	@Test
	void testMissingClassificationListsYieldZeroScore()
	{
		// The LLM may omit lists entirely; this must not throw.
		when(mockBot.classifyClaims(GROUND_TRUTH, ANSWER))
			.thenReturn(new ClaimClassification());

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(0.0, result.getValue(), 0.001);
	}

	@Test
	void testEvaluateAnswerCorrectnessNullAnswer()
	{
		Sample nullAnswerSample = Sample.builder()
			.withGroundTruth(GROUND_TRUTH)
			.withAnswer(null)
			.build();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(nullAnswerSample));
		assertEquals("Sample must have an answer for Answer Correctness evaluation", exception.getMessage());
	}

	@Test
	void testEvaluateAnswerCorrectnessNullGroundTruth()
	{
		Sample nullGroundTruthSample = Sample.builder()
			.withGroundTruth(null)
			.withAnswer(ANSWER)
			.build();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(nullGroundTruthSample));
		assertEquals("Sample must have a ground truth for Answer Correctness evaluation", exception.getMessage());
	}
}
