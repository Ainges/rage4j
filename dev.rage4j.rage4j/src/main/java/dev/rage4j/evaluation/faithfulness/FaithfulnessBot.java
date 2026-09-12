package dev.rage4j.evaluation.faithfulness;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.rage4j.evaluation.model.ArrayResponse;
import dev.rage4j.evaluation.model.InferenceVerdict;

import java.util.List;

public interface FaithfulnessBot
{
	@SystemMessage("You are a claims extractor llm. Your task is extraction of several self-consisted claims from a given text. Each claim must represent atomic knowledge fact. Try to extract as many claims as possible.")
	@UserMessage("Extract claims from this text: '''{{text}}'''")
	ArrayResponse extractClaims(@V("text") String text);

	/**
	 * Structured verdict on one claim. The return type is a record, so
	 * LangChain4j derives a JSON schema from it: a model built with
	 * {@code response_format: json_schema} answers with the schema enforced,
	 * any other model gets the schema as an instruction appended to the user
	 * message and its answer is parsed as JSON. Either way the parser is
	 * LangChain4j's JSON parser, not its strict {@code Boolean} parser, which
	 * rejects {@code true⏎true} — a shape some judge models (gpt-oss, for one)
	 * write regularly.
	 */
	@SystemMessage("""
		You are a fact checker LLM. You are provided with a context, a claim, \
		and optionally one or more images that are part of the context. \
		Determine whether the claim can be inferred from the context (and from the images, if present). \
		A claim can be inferred if it follows directly from the context OR if it can be derived through reasoning, \
		including: arithmetic calculations (e.g. summing numbers, counting items), \
		parsing structured data formats such as JSON or XML, \
		visual analysis of any provided images, \
		or logical implications of the context content. \
		Respond with a JSON object of the form {"inferred": true} or {"inferred": false}, nothing else.
		Do not add explanations, reasoning, or any other text.""")
	@UserMessage("""
		Context: {{context}}
		Claim: {{claim}}

		Can the claim be inferred from the context and any provided images?
		""")
	InferenceVerdict canBeInferred(
		@UserMessage List<ImageContent> images,
		@V("claim") String claim,
		@V("context") String context);

	/**
	 * The same question as {@link #canBeInferred} with a plain-text answer,
	 * used by {@link FaithfulnessEvaluator} as the fallback when the structured
	 * verdict cannot be parsed or carries no value. The prompt is the one the
	 * metric used before structured output existed, unchanged, so a fallback
	 * verdict is comparable to earlier measurements. The text is read by
	 * {@link InferenceVerdictParser}, which accepts the word repeated and
	 * rejects a contradiction.
	 */
	@SystemMessage("""
		You are a fact checker LLM. You are provided with a context, a claim, \
		and optionally one or more images that are part of the context. \
		Determine whether the claim can be inferred from the context (and from the images, if present). \
		A claim can be inferred if it follows directly from the context OR if it can be derived through reasoning, \
		including: arithmetic calculations (e.g. summing numbers, counting items), \
		parsing structured data formats such as JSON or XML, \
		visual analysis of any provided images, \
		or logical implications of the context content. \
		You MUST respond with ONLY the word "true" or "false", nothing else.
		Do not add explanations, reasoning, or any other text.""")
	@UserMessage("""
		Context: {{context}}
		Claim: {{claim}}

		Can the claim be inferred from the context and any provided images? Answer ONLY with true or false:
		""")
	String canBeInferredAsText(
		@UserMessage List<ImageContent> images,
		@V("claim") String claim,
		@V("context") String context);
}
