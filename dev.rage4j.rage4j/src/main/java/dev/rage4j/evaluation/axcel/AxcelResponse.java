package dev.rage4j.evaluation.axcel;

import java.util.List;

/**
 * Wraps the list of {@link AxcelFactEvaluation} entries returned by the
 * {@link AxcelBot}. A single wrapper object is used instead of returning
 * {@code List<AxcelFactEvaluation>} directly, mirroring the
 * {@link dev.rage4j.evaluation.model.ArrayResponse} idiom used elsewhere in
 * RAGE4j: Langchain4j (and its Quarkus integration) can generate output-format
 * instructions for a plain POJO return type, but not for a raw
 * {@code Collection<Pojo>} return type. Class is initiated automatically and is
 * needed only for service purposes.
 */
public class AxcelResponse
{
	private List<AxcelFactEvaluation> facts;

	public AxcelResponse()
	{
		// no args
	}

	public AxcelResponse(List<AxcelFactEvaluation> facts)
	{
		this.facts = facts;
	}

	public List<AxcelFactEvaluation> getFacts()
	{
		return facts;
	}

	public void setFacts(List<AxcelFactEvaluation> facts)
	{
		this.facts = facts;
	}
}
