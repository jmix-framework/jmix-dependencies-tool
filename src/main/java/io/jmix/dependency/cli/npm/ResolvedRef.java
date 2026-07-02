package io.jmix.dependency.cli.npm;

/**
 * A downloadable npm tarball reference: its URL and (optional) Subresource Integrity value.
 */
public record ResolvedRef(String url, String integrity) {
}
