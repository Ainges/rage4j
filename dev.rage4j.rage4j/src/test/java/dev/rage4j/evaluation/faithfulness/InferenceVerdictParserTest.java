package dev.rage4j.evaluation.faithfulness;

import dev.langchain4j.service.output.OutputParsingException;
import dev.rage4j.LoggingTestWatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shapes below are the ones a judge (gpt-oss) actually produced when asked
 * for only the word true or false: {@code true⏎true}, {@code false⏎false},
 * {@code false⏎true}, {@code true⏎false} and {@code false⏎⏎⏎true}. The first
 * two must parse, the last three must not.
 */
@ExtendWith(LoggingTestWatcher.class)
class InferenceVerdictParserTest
{
	@ParameterizedTest
	@ValueSource(strings = { "true", "True", "TRUE", " true ", "true.", "\"true\"", "**true**", "true\n", "true\ntrue", "true\n\ntrue\n" })
	void acceptsTheWordTrueHoweverOftenItIsRepeated(String raw)
	{
		assertTrue(InferenceVerdictParser.parse(raw));
	}

	@ParameterizedTest
	@ValueSource(strings = { "false", "False", "false.", "'false'", "false\nfalse", "false\n\n\nfalse" })
	void acceptsTheWordFalseHoweverOftenItIsRepeated(String raw)
	{
		assertFalse(InferenceVerdictParser.parse(raw));
	}

	@ParameterizedTest
	@ValueSource(strings = { "false\ntrue", "true\nfalse", "false\n\n\ntrue" })
	void rejectsContradictingVerdicts(String raw)
	{
		OutputParsingException e = assertThrows(OutputParsingException.class, () -> InferenceVerdictParser.parse(raw));
		assertTrue(e.getMessage().contains("Contradictory"), e.getMessage());
		assertTrue(e.getMessage().contains(raw), "the raw text must be quoted so the failure can be inspected");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "maybe", "The claim is true.", "the claim is not true", "yes" })
	void rejectsTextThatIsNotAVerdictLine(String raw)
	{
		OutputParsingException e = assertThrows(OutputParsingException.class, () -> InferenceVerdictParser.parse(raw));
		assertTrue(e.getMessage().contains("No verdict"), e.getMessage());
	}

	@Test
	void rejectsNull()
	{
		OutputParsingException e = assertThrows(OutputParsingException.class, () -> InferenceVerdictParser.parse(null));
		assertEquals("No verdict: the judge returned null", e.getMessage());
	}

	@Test
	void proseAroundAVerdictLineDoesNotHideIt()
	{
		// A verdict line among non-verdict lines is still exactly one verdict;
		// the prose lines
		// are ignored, not misread.
		assertTrue(InferenceVerdictParser.parse("Verdict:\ntrue"));
		assertFalse(InferenceVerdictParser.parse("false\n(the context lists 30, not 42)"));
	}
}
