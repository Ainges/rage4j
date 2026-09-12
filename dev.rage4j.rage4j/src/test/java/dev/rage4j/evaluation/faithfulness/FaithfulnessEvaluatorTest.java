package dev.rage4j.evaluation.faithfulness;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.output.OutputParsingException;
import dev.rage4j.LoggingTestWatcher;
import dev.rage4j.evaluation.Evaluation;
import dev.rage4j.evaluation.model.ArrayResponse;
import dev.rage4j.evaluation.model.InferenceVerdict;
import dev.rage4j.model.Rage4jImage;
import dev.rage4j.model.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
class FaithfulnessEvaluatorTest
{
	private static final String QUESTION = "What is the capital of France?";
	private static final String ANSWER = "Paris is the capital of France.";
	private static final String GROUND_TRUTH = "Paris";
	private static final String CONTEXT = "Paris is the capital of France.";
	private static final double EXPECTED_SCORE_FULL_MATCH = 1.0;
	private static final double EXPECTED_SCORE_PARTIAL_MATCH = 0.5;
	private static final double EXPECTED_SCORE_NO_MATCH = 0.0;

	private FaithfulnessEvaluator evaluator;
	private FaithfulnessBot mockBot;
	private Sample sample;

	@BeforeEach
	void setUp()
	{
		mockBot = mock(FaithfulnessBot.class);
		evaluator = new FaithfulnessEvaluator(mockBot);
		sample = Sample.builder()
			.withQuestion(QUESTION)
			.withAnswer(ANSWER)
			.withGroundTruth(GROUND_TRUTH)
			.withContext(CONTEXT)
			.build();
	}

	@Test
	void testEvaluateFaithfulness_FullMatch()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString())).thenReturn(new InferenceVerdict(true));

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_FULL_MATCH, result.getValue());
		assertEquals("Faithfulness", result.getName());
	}

	@Test
	void testEvaluateFaithfulness_PartialMatch()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris", "London" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString())).thenReturn(new InferenceVerdict(true));
		when(mockBot.canBeInferred(anyList(), eq("London"), anyString())).thenReturn(new InferenceVerdict(false));

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_PARTIAL_MATCH, result.getValue());
		assertEquals("Faithfulness", result.getName());
	}

	@Test
	void testEvaluateFaithfulness_NoMatch()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "London", "Berlin" }));
		when(mockBot.canBeInferred(anyList(), anyString(), anyString())).thenReturn(new InferenceVerdict(false));

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_NO_MATCH, result.getValue());
		assertEquals("Faithfulness", result.getName());
	}

	@Test
	void testEvaluateFaithfulnessEmptyClaims()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] {}));

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_NO_MATCH, result.getValue());
		assertEquals("Faithfulness", result.getName());
	}

	@Test
	void testEvaluateFaithfulnessNullContext()
	{
		Sample nullContextSample = Sample.builder()
			.withQuestion(QUESTION)
			.withAnswer(ANSWER)
			.withGroundTruth(GROUND_TRUTH)
			.withContext(null)
			.build();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(nullContextSample));
		assertEquals("Sample must have a context for Faithfulness evaluation", exception.getMessage());
	}

	@Test
	void testEvaluateFaithfulnessNullAnswer()
	{
		Sample nullAnswerSample = Sample.builder()
			.withQuestion(QUESTION)
			.withAnswer(null)
			.withGroundTruth(GROUND_TRUTH)
			.withContext(CONTEXT)
			.build();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(nullAnswerSample));
		assertEquals("Sample must have an answer for Faithfulness evaluation", exception.getMessage());
	}

	@Test
	void testEvaluateFaithfulnessImagesWithoutVisionThrows()
	{
		Sample sampleWithImages = Sample.builder()
			.withQuestion(QUESTION)
			.withAnswer(ANSWER)
			.withGroundTruth(GROUND_TRUTH)
			.withContext(CONTEXT)
			.withImage(Rage4jImage.fromBytes(new byte[] { 1 }, "image/png", "landmark.png"))
			.build();

		UnsupportedOperationException exception = assertThrows(
			UnsupportedOperationException.class,
			() -> evaluator.evaluate(sampleWithImages));
		assertTrue(exception.getMessage().contains("Faithfulness"));
		assertTrue(exception.getMessage().contains("vision"));
		// bot must not be invoked at all when the guard fires
		verify(mockBot, never()).extractClaims(anyString());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testEvaluateFaithfulnessForwardsImagesWhenVisionEnabled()
	{
		FaithfulnessBot visionBot = mock(FaithfulnessBot.class);
		FaithfulnessEvaluator visionEvaluator = new FaithfulnessEvaluator(visionBot, true);

		Rage4jImage img1 = Rage4jImage.fromBytes(new byte[] { 1 }, "image/png", "eiffel-tower.png");
		Rage4jImage img2 = Rage4jImage.fromUrl("https://example.com/louvre.jpg");
		Sample sampleWithImages = Sample.builder()
			.withQuestion(QUESTION)
			.withAnswer(ANSWER)
			.withGroundTruth(GROUND_TRUTH)
			.withContext(CONTEXT)
			.withImages(List.of(img1, img2))
			.build();

		when(visionBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(visionBot.canBeInferred(anyList(), anyString(), anyString())).thenReturn(new InferenceVerdict(true));

		Evaluation result = visionEvaluator.evaluate(sampleWithImages);

		assertEquals(EXPECTED_SCORE_FULL_MATCH, result.getValue());
		ArgumentCaptor<List<ImageContent>> imageCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<String> claimCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
		verify(visionBot).canBeInferred(imageCaptor.capture(), claimCaptor.capture(), contextCaptor.capture());
		assertEquals("Paris", claimCaptor.getValue());
		assertEquals(CONTEXT, contextCaptor.getValue());
		assertEquals(2, imageCaptor.getValue().size());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testEvaluateFaithfulnessVisionEnabledButNoImagesPassesEmptyList()
	{
		FaithfulnessBot visionBot = mock(FaithfulnessBot.class);
		FaithfulnessEvaluator visionEvaluator = new FaithfulnessEvaluator(visionBot, true);

		when(visionBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(visionBot.canBeInferred(anyList(), anyString(), anyString())).thenReturn(new InferenceVerdict(true));

		Evaluation result = visionEvaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_FULL_MATCH, result.getValue());
		ArgumentCaptor<List<ImageContent>> imageCaptor = ArgumentCaptor.forClass(List.class);
		verify(visionBot).canBeInferred(imageCaptor.capture(), anyString(), anyString());
		assertTrue(imageCaptor.getValue().isEmpty());
	}

	// --- structured verdict first, plain-text verdict as the net --------------------------

	@Test
	void structuredVerdictIsUsedWithoutAskingForText()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString())).thenReturn(new InferenceVerdict(true));

		evaluator.evaluate(sample);

		verify(mockBot, never()).canBeInferredAsText(anyList(), anyString(), anyString());
	}

	@Test
	void unparsableStructuredVerdictFallsBackToTheTextVerdict()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris", "London" }));
		when(mockBot.canBeInferred(anyList(), anyString(), anyString()))
			.thenThrow(new OutputParsingException("Failed to parse \"{\"inferred\": true}\n{\"inferred\": true}\"", null));
		// the shape gpt-oss writes: the word twice, one per line — accepted by the parser
		when(mockBot.canBeInferredAsText(anyList(), eq("Paris"), anyString())).thenReturn("true\ntrue");
		when(mockBot.canBeInferredAsText(anyList(), eq("London"), anyString())).thenReturn("false\nfalse");

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_PARTIAL_MATCH, result.getValue());
		verify(mockBot, times(2)).canBeInferredAsText(anyList(), anyString(), anyString());
	}

	@Test
	void nullStructuredVerdictIsNoVerdictAndFallsBackToTheTextVerdict()
	{
		// {"inferred": null} is legal under a strict schema; it must not count as false
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString())).thenReturn(new InferenceVerdict(null));
		when(mockBot.canBeInferredAsText(anyList(), eq("Paris"), anyString())).thenReturn("true");

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_FULL_MATCH, result.getValue());
	}

	@Test
	void missingStructuredVerdictObjectFallsBackToTheTextVerdict()
	{
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString())).thenReturn(null);
		when(mockBot.canBeInferredAsText(anyList(), eq("Paris"), anyString())).thenReturn("false");

		Evaluation result = evaluator.evaluate(sample);

		assertEquals(EXPECTED_SCORE_NO_MATCH, result.getValue());
	}

	@Test
	void contradictoryTextVerdictLeavesTheSampleWithoutAValue()
	{
		// one claim without a verdict is a sample without a faithfulness value — never a partial score
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris", "London" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString())).thenReturn(new InferenceVerdict(true));
		when(mockBot.canBeInferred(anyList(), eq("London"), anyString()))
			.thenThrow(new OutputParsingException("unparsable", null));
		when(mockBot.canBeInferredAsText(anyList(), eq("London"), anyString())).thenReturn("false\ntrue");

		OutputParsingException e = assertThrows(OutputParsingException.class, () -> evaluator.evaluate(sample));

		assertTrue(e.getMessage().contains("false\ntrue"), "the raw judge text must travel with the failure");
	}

	@Test
	void transportFailureOfTheStructuredCallIsNotRetriedAsText()
	{
		// a timeout or a rejected request is not a formatting problem: a second call in another
		// format would only repeat it, and hide which failure class it was
		when(mockBot.extractClaims(ANSWER)).thenReturn(new ArrayResponse(new String[] { "Paris" }));
		when(mockBot.canBeInferred(anyList(), eq("Paris"), anyString()))
			.thenThrow(new IllegalStateException("Input length exceeds model's maximum context length"));

		assertThrows(IllegalStateException.class, () -> evaluator.evaluate(sample));
		verify(mockBot, never()).canBeInferredAsText(anyList(), anyString(), anyString());
	}
}
