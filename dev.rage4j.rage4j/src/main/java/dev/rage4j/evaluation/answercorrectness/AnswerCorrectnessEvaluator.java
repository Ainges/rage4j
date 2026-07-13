package dev.rage4j.evaluation.answercorrectness;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.rage4j.evaluation.Evaluation;
import dev.rage4j.evaluation.Evaluator;
import dev.rage4j.evaluation.model.ClaimClassification;
import dev.rage4j.model.Sample;
import org.apache.commons.math3.analysis.function.Divide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@code AnswerCorrectnessEvaluator} class evaluates the correctness of an
 * answer by comparing it to the ground truth using true positive, false
 * positive, and false negative claims. The claims are extracted and partitioned
 * in a single LLM call that assigns every claim to exactly one of the three
 * sets, so the same fact cannot be counted twice. The result is expressed as an
 * F1 score, which balances precision and recall.
 */
public class AnswerCorrectnessEvaluator implements Evaluator
{
	private static final String METRIC_NAME = "Answer correctness";
	private static final Logger LOG = LoggerFactory.getLogger(AnswerCorrectnessEvaluator.class);

	private final AnswerCorrectnessBot bot;

	/**
	 * Constructs an {@code AnswerCorrectnessEvaluator} using a
	 * {@code ChatModel}. The evaluator creates an instance of
	 * {@code AnswerCorrectnessBot} to assess the correctness of an answer.
	 *
	 * @param model
	 *            The {@code ChatModel} used to create the bot for evaluation.
	 */
	public AnswerCorrectnessEvaluator(ChatModel model)
	{
		bot = AiServices.create(AnswerCorrectnessBot.class, model);
	}

	/**
	 * Constructs an {@code AnswerCorrectnessEvaluator} with an existing
	 * {@code AnswerCorrectnessBot}. This constructor is useful for testing or
	 * custom scenarios where the bot is injected.
	 *
	 * @param answerCorrectnessBot
	 *            The bot used to evaluate the correctness of answers.
	 */
	public AnswerCorrectnessEvaluator(AnswerCorrectnessBot answerCorrectnessBot)
	{
		this.bot = answerCorrectnessBot;
	}

	/**
	 * Evaluates the correctness of the provided sample's answer against its
	 * ground truth. The correctness is calculated based on true positives,
	 * false positives, and false negatives, and the result is returned as an F1
	 * score.
	 *
	 * @param sample
	 *            The sample containing the answer and ground truth to be
	 *            evaluated.
	 * @return An {@code Evaluation} object containing the F1 score and the
	 *         metric name.
	 * @throws IllegalStateException
	 *             if either the answer or ground truth is missing in the
	 *             sample.
	 */
	@Override
	public Evaluation evaluate(Sample sample)
	{
		if (!sample.hasAnswer())
		{
			throw new IllegalArgumentException("Sample must have an answer for Answer Correctness evaluation");
		}
		if (!sample.hasGroundTruth())
		{
			throw new IllegalArgumentException("Sample must have a ground truth for Answer Correctness evaluation");
		}

		String groundTruth = sample.getGroundTruth();
		String answer = sample.getAnswer();
		LOG.info("Evaluating new sample");
		LOG.info("Ground truth: {}", groundTruth);
		LOG.info("Answer: {}", answer);

		ClaimClassification classification = bot.classifyClaims(groundTruth, answer);

		String[] truePositiveClaims = orEmpty(classification.getTruePositives());
		Set<String> truePositiveSet = new HashSet<>(Arrays.asList(truePositiveClaims));
		String[] falsePositiveClaims = withoutTruePositives(orEmpty(classification.getFalsePositives()), truePositiveSet, "false positive");
		String[] falseNegativeClaims = withoutTruePositives(orEmpty(classification.getFalseNegatives()), truePositiveSet, "false negative");

		double truePositives = truePositiveClaims.length;
		double falsePositives = falsePositiveClaims.length;
		double falseNegatives = falseNegativeClaims.length;

		if (truePositives == 0 && falsePositives == 0 && falseNegatives == 0)
		{
			LOG.info("No true positives, false positives, or false negatives found.");
			return new Evaluation(METRIC_NAME, 0);
		}
		else
		{
			LOG.info("True positives: {}", (Object)truePositiveClaims);
			LOG.info("False positives: {}", (Object)falsePositiveClaims);
			LOG.info("False negatives: {}", (Object)falseNegativeClaims);
		}

		double denominator = truePositives + new Divide().value(falsePositives + falseNegatives, 2);
		double f1Metric = new Divide().value(truePositives, denominator);

		LOG.info("Answer Correctness (F1) Metric: {}", f1Metric);

		return new Evaluation(METRIC_NAME, f1Metric);
	}

	private static String[] orEmpty(String[] claims)
	{
		return claims == null ? new String[0] : claims;
	}

	/**
	 * Removes claims that are already classified as true positives. The prompt
	 * instructs the LLM to assign every claim to exactly one list, but nothing
	 * structurally prevents it from repeating a true positive here. Such a
	 * contradictory claim must not be counted against the score.
	 */
	private static String[] withoutTruePositives(String[] claims, Set<String> truePositiveSet, String listName)
	{
		String[] filtered = Arrays.stream(claims)
			.filter(claim -> !truePositiveSet.contains(claim))
			.toArray(String[]::new);
		if (filtered.length < claims.length)
		{
			LOG.warn("Ignoring {} contradictory {} claim(s) already classified as true positive", claims.length - filtered.length, listName);
		}
		return filtered;
	}
}
