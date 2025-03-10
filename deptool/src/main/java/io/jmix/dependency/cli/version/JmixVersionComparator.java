package io.jmix.dependency.cli.version;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

public class JmixVersionComparator implements Comparator<String> {

    private static final Pattern VERSION_SPLIT_REGEX = Pattern.compile("[.-]");

    public static final JmixVersionComparator INSTANCE = new JmixVersionComparator();

    /**
     * <ul>
     *     <li>null, null -> 0</li>
     *     <li>null, !null -> -1</li>
     *     <li>!null, null -> 1</li>
     *     <li>2.0, 1.0 -> 1</li>
     *     <li>1.0, 2.0 -> -1</li>
     *     <li>2.0, 2.0 -> 0</li>
     *     <li>2.0, 2.1 -> -1</li>
     *     <li>2.0.1, 2.0.0 -> 1</li>
     *     <li>2.0, 2.0.1 -> -1</li>
     * </ul>
     *
     * @return >0 if v1 is newer than v2, 0 if equals, <0 if v1 is older than v2
     */
    @Override
    public int compare(String v1, String v2) {
        if (v1 == null && v2 != null) {
            return -1;
        }

        if (v1 != null && v2 == null) {
            return 1;
        }

        if (Objects.equals(v1, v2)) {
            return 0;
        }

        String[] strings1 = VERSION_SPLIT_REGEX.split(v1);
        String[] strings2 = VERSION_SPLIT_REGEX.split(v2);

        for (int i = 0; i < strings1.length; i++) {
            if (strings2.length <= i) {
                return 1;
            }

            String s1 = strings1[i];
            String s2 = strings2[i];

            if (s1.equals(s2)) continue;

            try {
                Integer n1 = Integer.valueOf(s1);
                Integer n2 = Integer.valueOf(s2);
                return n1 - n2;
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        }

        if (strings1.length < strings2.length) {
            return -1;
        }

        return 0;
    }
}