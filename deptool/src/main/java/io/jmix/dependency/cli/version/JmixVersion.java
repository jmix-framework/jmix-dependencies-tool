/*
 * Copyright (c) 2008-2022 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package io.jmix.dependency.cli.version;

public record JmixVersion(int major, int minor, int patch, String suffix) implements Comparable<JmixVersion> {

    private static final Character DOT = '.';

    public static JmixVersion from(String versionString) {
        return JmixVersionUtils.toJmixVersion(versionString);
    }

    public JmixVersion withMajor(int major) {
        return new JmixVersion(major, minor, patch, suffix);
    }

    public JmixVersion withMinor(int minor) {
        return new JmixVersion(major, minor, patch, suffix);
    }

    public JmixVersion withPatch(int patch) {
        return new JmixVersion(major, minor, patch, suffix);
    }

    public boolean isSnapshot() {
        return JmixVersionUtils.isSnapshot(versionString());
    }

    public boolean isRC() {
        return JmixVersionUtils.isRC(versionString());
    }

    public boolean isStable() {
        return JmixVersionUtils.isStable(versionString());
    }

    public String versionString() {
        return versionString(true);
    }

    public String versionString(boolean withSuffix) {
        StringBuilder versionBuilder = new StringBuilder()
                .append(major).append(DOT)
                .append(minor).append(DOT)
                .append(patch);

        if (withSuffix) {
            versionBuilder.append(suffix);
        }

        return versionBuilder.toString();
    }

    public JmixVersion copy() {
        return JmixVersion.from(versionString());
    }

    @Override
    public String toString() {
        return versionString();
    }

    @Override
    public int compareTo(JmixVersion o) {
        return JmixVersionUtils.compare(versionString(), o.versionString());
    }
}
