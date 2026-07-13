package dev.rage4j.evaluation.model;

/**
 * Represents the classification of atomic claims extracted from a ground truth
 * and a generated answer, used in the AiServices of the RAGE4j library to get
 * the claim partition as a single structured response from Langchain4j. Every
 * claim belongs to exactly one of the three lists. Class is initiated
 * automatically and is needed only for service purposes.
 */
public class ClaimClassification
{
	private String[] truePositives;
	private String[] falsePositives;
	private String[] falseNegatives;

	public ClaimClassification(String[] truePositives, String[] falsePositives, String[] falseNegatives)
	{
		this.truePositives = truePositives;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
	}

	public ClaimClassification()
	{
		// no args
	}

	public String[] getTruePositives()
	{
		return truePositives;
	}

	public void setTruePositives(String[] truePositives)
	{
		this.truePositives = truePositives;
	}

	public String[] getFalsePositives()
	{
		return falsePositives;
	}

	public void setFalsePositives(String[] falsePositives)
	{
		this.falsePositives = falsePositives;
	}

	public String[] getFalseNegatives()
	{
		return falseNegatives;
	}

	public void setFalseNegatives(String[] falseNegatives)
	{
		this.falseNegatives = falseNegatives;
	}
}
