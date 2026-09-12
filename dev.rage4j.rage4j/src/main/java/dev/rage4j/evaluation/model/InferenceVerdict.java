package dev.rage4j.evaluation.model;

/**
 * Structured verdict of the faithfulness judge on one claim: whether the claim
 * can be inferred from the context. A record, so that LangChain4j can derive a
 * JSON schema from it and request structured output
 * ({@code response_format: json_schema}) from a model that supports it — a bare
 * {@code Boolean} return type has no schema and leaves the answer to a strict
 * text parser, which rejects anything but the exact word — and some judge
 * models (gpt-oss, for one) regularly answer {@code true⏎true}, each such
 * answer a lost measurement.
 * <p>
 * The field is {@code Boolean}, not {@code boolean}, on purpose: strict JSON
 * schemas express optionality as {@code "type": ["boolean", "null"]}, so
 * {@code {"inferred": null}} is a legal answer — and Jackson maps {@code null}
 * onto a primitive as {@code false}, which would turn "no verdict" into an
 * established "not inferred". A {@code null} here must be treated as no
 * verdict.
 *
 * @param inferred
 *            {@code true} when the claim follows from the context,
 *            {@code false} when it does not, {@code null} when the judge gave
 *            no verdict
 */
public record InferenceVerdict(Boolean inferred)
{
}
