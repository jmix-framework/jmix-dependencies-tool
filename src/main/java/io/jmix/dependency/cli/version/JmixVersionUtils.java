package io.jmix.dependency.cli.version;

import io.jmix.dependency.cli.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import static io.jmix.dependency.cli.util.StringUtils.isBlank;

public class JmixVersionUtils {

    private static final String defaultSuffixSeparator = "-";
    private static final String SNAPSHOT_SUFFIX = "SNAPSHOT";
    private static final Pattern VERSION_SPLIT_REGEX = Pattern.compile("[.-]");
    private static final Pattern RC_PATTERN = Pattern.compile("(-RC\\d*)$");

    public static final JmixVersionComparator VERSION_COMPARATOR = JmixVersionComparator.INSTANCE;

    public static JmixVersion toJmixVersion(String version) {
        if (!isValidVersion(version)) {
            throw new IllegalArgumentException("Invalid Jmix version. Version should be in format x.y.z");
        }

        Integer major = extractMajorNumber(version);
        Integer minor = extractMinorNumber(version);
        Integer patch = extractPatchNumber(version);

        String suffix = extractSuffix(version, defaultSuffixSeparator);
        if (StringUtils.isNotBlank(suffix)) {
            suffix = defaultSuffixSeparator + suffix;
        }

        return new JmixVersion(major, minor, patch, suffix);
    }

    /**
     * @return true if {@code version} has major, minor and patch parts.
     */
    public static boolean isValidVersion(String version) {
        return hasPatch(version);
    }

    public static boolean hasPatch(String version) {
        if (isBlank(version)) {
            return false;
        }
        return VERSION_SPLIT_REGEX.split(version).length > 2;
    }

    public static int compare(String v1, String v2) {
        return VERSION_COMPARATOR.compare(v1, v2);
    }

    /**
     * Extracts a minor version from version string.
     * Examples:
     * <ul>
     *     <li>null -> null</li>
     *     <li>"" -> ""</li>
     *     <li>abc -> ""</li>
     *     <li>1.4.2 -> 1.4</li>
     *     <li>1.4 -> 1.4</li>
     *     <li>1 -> 1.0</li>
     * </ul>
     */
    public static String getMinorVersion(String version) {
        if (version == null) {
            return null;
        }

        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            if (parts.length == 0) {
                return "";
            } else {
                return parts[0] + "." + 0;
            }
        }

        return parts[0] + "." + parts[1];
    }

    public static Integer extractMajorNumber(String version) {
        String major = getVersionPartByIndex(version, 0);
        return parseNumberSafely(major);
    }

    /**
     * Extracts a minor number from version string.
     * <ul>
     *     <li>null -> null</li>
     *     <li>"" -> null</li>
     *     <li>abc -> null</li>
     *     <li>1.4.2 -> 4</li>
     *     <li>1.4 -> 4</li>
     *     <li>1 -> 0</li>
     * </ul>
     */
    public static Integer extractMinorNumber(String version) {
        if (isBlank(version)) {
            return null;
        }

        String[] parts = VERSION_SPLIT_REGEX.split(version);
        if (parts.length < 2) {
            if (parts.length == 0) {
                return null;
            } else {
                return 0;
            }
        }

        String minor = getVersionPartByIndex(version, 1);
        return parseNumberSafely(minor);
    }

    /**
     * Extracts a patch number from version string.
     * <ul>
     *     <li>null -> null</li>
     *     <li>"" -> null</li>
     *     <li>abc -> null</li>
     *     <li>1.4.2 -> 2</li>
     *     <li>1.4 -> 0</li>
     *     <li>1 -> 0</li>
     * </ul>
     */
    public static Integer extractPatchNumber(String version) {
        String[] parts = VERSION_SPLIT_REGEX.split(version);
        if (parts.length < 3) {
            if (parts.length == 0) {
                return null;
            } else {
                return 0;
            }
        }

        String patch = getVersionPartByIndex(version, 2);
        return parseNumberSafely(patch);
    }

    public static String extractSuffix(String version) {
        return extractSuffix(version, defaultSuffixSeparator);
    }

    public static String extractSuffix(String version, String separator) {
        if (isBlank(version)) {
            return "";
        }

        String[] split = version.split(separator);
        if (split.length < 2) {
            return "";
        }

        return split[1];
    }

    public static boolean isSnapshot(String version) {
        return StringUtils.isNotBlank(version) && SNAPSHOT_SUFFIX.equalsIgnoreCase(extractSuffix(version));
    }

    public static boolean isRC(String version) {
        return StringUtils.isNotBlank(version) && RC_PATTERN.matcher(version).find();
    }

    /**
     * @return {@code true} if {@code version} has suffix, {@code false} otherwise.
     * @see #extractSuffix
     */
    public static boolean isUnstable(String version) {
        return StringUtils.isNotBlank(version) && StringUtils.isNotBlank(extractSuffix(version));
    }

    /**
     *
     * @return {@code false} if {@link #isUnstable} returns {@code true} and vice versa.
     */
    public static boolean isStable(String version) {
        return !isUnstable(version);
    }

    /**
     * <p>Returns version without a suffix if any</p>
     *
     * <pre>
     * null           -> null
     * ""             -> null
     * " "            -> null
     * "bob"          -> "bob"
     * "2.5.0-SNAPSHOT" -> "2.5.0"
     * </pre>
     */
    public static String getStableVersion(String version) {
        String suffix = extractSuffix(version);
        if (isBlank(version) || isBlank(suffix)) {
            return version;
        } else {
            int suffixIndex = version.lastIndexOf(suffix) - 1;
            if (suffixIndex > 0) {
                return version.substring(0, suffixIndex);
            } else {
                return null;
            }
        }
    }

    public static void sort(List<String> versions) {
        versions.sort((v1, v2) -> -VERSION_COMPARATOR.compare(v1, v2));
    }

    /**
     * <ul>
     *     <li>null, _ -> null</li>
     *     <li>"", _ -> null</li>
     *     <li>abc, _ -> null</li>
     *
     *     <li>1.4.0, 0 -> 1</li>
     *     <li>1.4.0, 1 -> 4</li>
     *     <li>1.4.0, 2 -> 0</li>
     *     <li>1.4.0, 3 -> null</li>
     * </ul>
     */
    private static String getVersionPartByIndex(String version, int partIndex) {
        if (isBlank(version)) {
            return null;
        }

        String[] parts = VERSION_SPLIT_REGEX.split(version);
        if (parts.length >= partIndex + 1) {
            return parts[partIndex];
        } else {
            return null;
        }
    }

    private static Integer parseNumberSafely(String number) {
        if (number != null) {
            try {
                return Integer.parseInt(number);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
