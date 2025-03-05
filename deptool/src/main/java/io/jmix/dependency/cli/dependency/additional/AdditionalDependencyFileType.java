package io.jmix.dependency.cli.dependency.additional;

import java.io.InputStream;

public enum AdditionalDependencyFileType {

    PACKAGE_LOCK(AdditionalDependencyType.NPM, "package-lock.json");

    private final String fileName;

    private final AdditionalDependencyType dependencyType;

    AdditionalDependencyFileType(AdditionalDependencyType dependencyType, String fileName) {
        this.fileName = fileName;
        this.dependencyType = dependencyType;
    }

    public AdditionalDependencyType getDependencyType() {
        return dependencyType;
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream findFileContent(String jmixVersion) {
        return getDependencyType().findFileContent(jmixVersion, getFileName());
    }
}
