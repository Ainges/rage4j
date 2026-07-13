package dev.rage4j.evaluation.answercorrectness;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.rage4j.evaluation.model.ClaimClassification;

public interface AnswerCorrectnessBot
{
	String USER_MESSAGE_PROMPT = """
		    Extract and classify claims from this ground truth and generated answer.

		    Ground truth:
		    "{{groundTruth}}"

		    Generated answer:
		    "{{actualAnswer}}"
		""";

	@SystemMessage("""
		You are a facts analyzer LLM. Decompose ground truth and generated answer into atomic claims (single facts) and classify EVERY claim into EXACTLY ONE of three lists:

		- truePositives: claims SEMANTICALLY present in BOTH ground truth and generated answer.
		- falsePositives: claims from the generated answer NOT semantically supported by the ground truth (hallucinations/extras).
		- falseNegatives: claims from the ground truth NOT semantically covered by the generated answer (missing info).

		Rules:
		- Decompose once, then assign: the same fact must never appear in more than one list.
		- Semantic match: paraphrases count (e.g. "description of the book X is Y" == "exact description for X: Y"). Ignore minor wording differences if meaning identical.
		- One fact per claim; maximize coverage of both texts.
		- Always fully write the claim you extracted in the output (do not write something like ["Paris is the la..."], instead write the full claim "Paris is the largest city of France").

		Example:
		GT: "Paris is the capital of France and has 2.1 million inhabitants."
		Answer: "Paris is the capital of France and has the Eiffel Tower."
		Output:
		{
		  "truePositives": ["Paris is the capital of France"],
		  "falsePositives": ["Paris has the Eiffel Tower"],
		  "falseNegatives": ["Paris has 2.1 million inhabitants"]
		}
		""")
	@UserMessage(USER_MESSAGE_PROMPT)
	ClaimClassification classifyClaims(@V("groundTruth") String groundTruth, @V("actualAnswer") String actualAnswer);
}
