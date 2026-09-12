package dev.rage4j.evaluation.faithfulness;

import dev.langchain4j.service.output.OutputParsingException;

import java.util.List;
import java.util.Locale;

/**
 * Reads a plain-text {@code true}/{@code false} verdict the way a judge model
 * actually writes it, rather than the way the prompt asked for it.
 * LangChain4j's own {@code Boolean} parser accepts only the exact word; some
 * judge models (gpt-oss, for one), asked for "ONLY the word true or false",
 * regularly answer {@code true⏎true} or {@code false⏎false} — the word twice,
 * one per line — and occasionally two <em>different</em> words. The first two
 * are an unambiguous verdict and are accepted here; the last is a contradiction
 * and is still rejected, because a contradiction is not a verdict.
 * <p>
 * The parser is deliberately line-based and strict per line: a line is a
 * verdict only if it is nothing but the word (case-insensitive, surrounding
 * quotes and a trailing period tolerated). Prose containing the word is not a
 * verdict — "the claim is not true" must never parse as {@code true}.
 */
public final class InferenceVerdictParser
{
	private InferenceVerdictParser()
	{
	}

	/**
	 * @param raw
	 *            the judge's complete text answer
	 * @return the verdict when the text carries exactly one distinct verdict
	 *         word
	 * @throws OutputParsingException
	 *             when the text carries no verdict line at all, or verdict
	 *             lines that contradict each other; the message quotes the raw
	 *             text so the failure can be inspected
	 */
	public static boolean parse(String raw)
	{
		if (raw == null)
		{
			throw new OutputParsingException("No verdict: the judge returned null", null);
		}
		List<Boolean> verdicts = raw.lines()
			.map(InferenceVerdictParser::normalize)
			.filter(line -> line.equals("true") || line.equals("false"))
			.map(Boolean::valueOf)
			.distinct()
			.toList();
		if (verdicts.isEmpty())
		{
			throw new OutputParsingException("No verdict line in the judge's answer: \"" + raw + "\"", null);
		}
		if (verdicts.size() > 1)
		{
			throw new OutputParsingException("Contradictory verdicts in the judge's answer: \"" + raw + "\"", null);
		}
		return verdicts.getFirst();
	}

	private static String normalize(String line)
	{
		String s = line.strip().toLowerCase(Locale.ROOT);
		while (!s.isEmpty() && isDecoration(s.charAt(0)))
		{
			s = s.substring(1);
		}
		while (!s.isEmpty() && isDecoration(s.charAt(s.length() - 1)))
		{
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static boolean isDecoration(char c)
	{
		return c == '"' || c == '\'' || c == '`' || c == '.' || c == '*';
	}
}
