# Jmix Dependencies Tool

## Overview

`deptool` is a command-line utility that helps to prepare custom Nexus repository for developing projects based on Jmix in an isolated environment (without internet access).

`deptool` provides the following functionality:

* resolve all dependencies for Jmix framework
* resolve dependencies for any custom library
* export resolved dependencies in Maven repository format (for java) or tgz archives (for npm)
* upload exported dependencies into custom Nexus repository

## Installation

In order to run the tool Java 11+ must be installed on the machine.

The tool distribution (zip archive) may be downloaded from the [Releases](https://github.com/jmix-framework/jmix-dependencies-tool/releases) page.

After extracting the archive use `bin/deptool` or `bin/deptool.bat` files for executing CLI commands.

Distribution directories structure:

```text
deptool
  -  bin (contains CLI executables)
  -  lib (required Java libraries)
  -  resolver (Gradle project used by the tool to resolve dependencies)
  -  npm-resolver (Gradle project used by the tool to resolve npm dependencies)
```

NOTE: to be able to export npm dependencies you need to install Node.js with npm package manager.

## Usage Scenarios

The general usage scenario is the following:

- Resolve all Jmix dependencies for the specific version of the framework and its add-ons. If commercial add-ons are needed, this step requires the Enterprise subscription key. Resolve also additional libraries required for the project, if any. On this step, the resolved Java artifacts will be downloaded to the local Gradle cache. Npm dependencies are resolved as package-lock.json file.
- Export the resolved Java artifacts to the local Maven repository in the portable format.
- Export the resolved npm artifacts as a folder with packages as a tgz archives.
- Upload exported artifacts to a custom Nexus repository.
- Configure the project for using the custom Nexus repository.

## Dependencies Resolution

The tool delegates dependencies resolution to a special Gradle project. The flow for getting required dependencies for Jmix framework of specific version or for any other library is the following: 

* We pass dependency coordinates or Jmix version number to the `deptool` command.
* The `deptool` invokes Gradle task of the special **resolver** project that is shipped with in the tool distribution.
* Gradle downloads dependencies artifacts to its artifacts cache.
* Using the `deptool` utility we copy resolved artifacts from Gradle cache into output directory.

### Resolve Jmix Dependencies (resolve-jmix)

The tool can resolve all dependencies required for specific Jmix version.

```shell
./deptool resolve-jmix --jmix-version 2.1.0
```

The command resolves and downloads dependencies required for Jmix starters to the Gradle cache. 

Command options:

* `--jmix-version` (required) - the Jmix framework version.
* `--jmix-plugin-version` - Jmix plugin version. If not defined the value from the `--jmix-version` will be used.
* `--resolve-commercial-addons` - whether to resolve Jmix commercial add-ons. The `--jmix-license-key` option must be provided in this case. By default, only open-source modules dependencies are resolved.
* `--jmix-license-key` - your Jmix license key. This option is required if you resolve Jmix commercial add-ons.
* `--commercial-subscription-plan` - Type of your commercial subscription plan - 'enterprise' or 'bpm' (default). Relevant only if `--resolve-commercial-addons` is present.
* `--gradle-user-home` - gradle user home directory. It is the directory where dependencies will be downloaded to by Gradle. This directory must distinct from the user home of the gradle installed on your machine in order to contain only dependencies required for Jmix. The default value is `../gradle-home`.
* `--resolver-project` - a path to a special gradle project used for dependencies resolution. This project is delivered within the distribution bundle. The default value is `../resolver`.
* `--gradle-version` - version of Gradle installation that will be used.
* `--public-repository` - url for repository with public artifacts (`https://global.repo.jmix.io/repository/public` by default)
* `--premium-repository` - url for repository with premium artifacts (`https://global.repo.jmix.io/repository/premium` by default)
* `--repository` - additional Maven repository that must be used for dependencies resolution. If no authentication is required then pass repository URL as parameter value. If authentication is required, then pass URL, username and password separated by `|`, e.g. `http://localhost:8081/jmix|admin|admin`

If you run the command from within the `deptool/bin` directory then the only required option is the `--jmix-version`. Other options have default values that work for that case. If you run the command from some other place then you need to configure a location of gradle home and a location of the resolver project.

Keep in mind that multiple invocations of the `resolve` command for resolving dependencies for different Jmix versions will accumulate dependencies for all resolved versions in the gradle user home (`../gradle-home`). If you need to export dependencies for specific version only and do not need dependencies of previous resolving operations then you have to clean the `gradle-home` directory.  

### Resolve a Single Library (resolve-lib)

The command transitively resolves artifacts used by any single dependency.

```shell
./deptool resolve-lib javax.validation:validation-api:1.0.0.GA
```

Command options:

* `--jmix-version` - see `resolve-jmix` command documentation. The option is not required for the resolve-lib command.
* `--jmix-plugin-version` - see `resolve-jmix` command documentation.
* `--gradle-user-home` - see `resolve-jmix` command documentation.
* `--resolver-project` - see `resolve-jmix` command documentation.
* `--jmix-license-key` - your Jmix license key. This option is required if you resolve dependencies that use Jmix commercial add-ons.
* `--gradle-version` - version of Gradle installation that will be used.
* `--public-repository` - url for repository with public artifacts (`https://global.repo.jmix.io/repository/public` by default)
* `--premium-repository` - url for repository with premium artifacts (`https://global.repo.jmix.io/repository/premium` by default)
* `--repository` - additional Maven repository that must be used for dependencies resolution. If no authentication is required then pass repository URL as parameter value. If authentication is required, then pass URL, username and password separated by `|`, e.g. `http://localhost:8081/jmix|admin|admin`


If `--jmix-version` option is defined then Jmix BOM will be used during dependency resolution. Jmix BOM is not used by default.

### Resolve NPM Dependencies (resolve-npm)

The tool can resolve all npm dependencies required for specific Jmix version.

```shell
./deptool resolve-npm --jmix-version 2.1.0
```

The command resolves npm dependencies required for Jmix frontend as package-lock.json.

```
NOTE: save generated package-lock.json file - it's required for further project configuration
```

Command options:

* `--jmix-version` (required) - the Jmix framework version.
* `--jmix-plugin-version` - Jmix plugin version. If not defined the value from the `--jmix-version` will be used.
* `--resolve-commercial-addons` - whether to resolve Jmix commercial add-ons. The `--jmix-license-key` option must be provided in this case. By default, only open-source modules dependencies are resolved.
* `--jmix-license-key` - your Jmix license key. This option is required if you resolve Jmix commercial add-ons.
* `--commercial-subscription-plan` - Type of your commercial subscription plan - 'enterprise' or 'bpm' (default). Relevant only if `--resolve-commercial-addons` is present.
* `--gradle-user-home` - gradle user home directory. It is the directory where dependencies will be downloaded to by Gradle. This directory must distinct from the user home of the gradle installed on your machine in order to contain only dependencies required for Jmix. The default value is `../gradle-home`.
* `--resolver-project` - a path to a special gradle project used for dependencies resolution. This project is delivered within the distribution bundle. The default value is `../npm-resolver`.
* `--gradle-version` - version of Gradle installation that will be used.
* `--public-repository` - url for repository with public artifacts (`https://global.repo.jmix.io/repository/public` by default)
* `--premium-repository` - url for repository with premium artifacts (`https://global.repo.jmix.io/repository/premium` by default)
* `--repository` - additional Maven repository that must be used for dependencies resolution. If no authentication is required then pass repository URL as parameter value. If authentication is required, then pass URL, username and password separated by `|`, e.g. `http://localhost:8081/jmix|admin|admin`

If you run the command from within the `deptool/bin` directory then the only required option is the `--jmix-version`. Other options have default values that work for that case. If you run the command from some other place then you need to configure a location of gradle home and a location of the resolver project.

Keep in mind that each invocation of the `resolve-npm` command will override the result of the previous one.

## Export Resolved Dependencies (export)

The command copies all resolved artifact from the gradle user home directory to the specific target directory. In the target directory files will be organized in a Maven repository format.

```shell
./deptool export
```

By default, if you run the `deptool` from the `deptool/bin` directory the command will export artifacts to the `deptool/export` directory. If you want to change the output directory location, use the `--target-dir` option.

Command options:

* `--target-dir` - a directory where dependencies artifacts will be exported to (`../export` by default).
* `--gradle-user-home` - gradle home directory with resolved dependencies. See `resolve-jmix` command documentation.
* `--report-file` - a path to a file which will contain a list of all exported artifacts. 

## Export Resolved NPM Dependencies (export-npm)

The command download all npm packages declared in package-lock.json as a folders with tgz archives. 
The command copies all resolved artifact from the gradle user home directory to the specific target directory. In the target directory files will be organized in a Maven repository format.

```shell
./deptool export-npm
```

By default, if you run the `deptool` from the `deptool/bin` directory the command will export artifacts to the `deptool/export-npm` directory. If you want to change the output directory location, use the `--target-dir` option.

Command options:

* `--package-lock-file` - path to the package-lock.json file with declared dependencies (`../npm-resolver/package-lock.json` by default).
* `--target-dir` - a directory where dependencies artifacts will be exported to (`../export-npm` by default).
* `--resolver-project` - a path to a special gradle project used for dependencies resolution. This project is delivered within the distribution bundle. The default value is `../npm-resolver`.

## Upload Exported Dependencies to Nexus Repository (upload)

The command uploads artifacts exported by the `export` command to the Nexus repository.

```shell
./deptool upload --nexus-url http://localhost:8081 \
  --nexus-repository jmix \
  --nexus-username admin \
  --nexus-password adminpass \
  --artifacts-dir ../export
```

Command options:

* `--nexus-url` (required) - Nexus repository URL.
* `--nexus-repository` (required) - Nexus repository name.
* `--nexus-username` (required) - Nexus user username.
* `--nexus-password` (required) - Nexus user password.
* `--artifacts-dir` (required) - a directory with artifacts to be uploaded to Nexus.

## Upload Exported NPM Dependencies to Nexus Repository (upload-npm)

The command uploads artifacts exported by the `export-npm` command to the Nexus repository.

```shell
./deptool upload-npm --nexus-url http://localhost:8081 \
  --nexus-repository jmix-npm \
  --nexus-username admin \
  --nexus-password adminpass \
  --artifacts-dir ../export-npm
```

Command options:

* `--nexus-url` (required) - Nexus repository URL.
* `--nexus-repository` (required) - Nexus repository name.
* `--nexus-username` (required) - Nexus user username.
* `--nexus-password` (required) - Nexus user password.
* `--artifacts-dir` (required) - a directory with artifacts to be uploaded to Nexus.

## Configure Projects For Working with Custom Nexus Repository

While creating a new Jmix project in Jmix Studio, add custom Nexus repository.

![New Project](images/new-project.png)

After the project is created, add the following lines to the beginning of the `settings.gradle` using correct Nexus repository URL:

```groovy
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == 'io.jmix') {
                useModule("io.jmix.gradle:jmix-gradle-plugin:${requested.version}")
            }
        }
    }

    repositories {
        maven {
            allowInsecureProtocol true
            url 'http://localhost:8081/repository/jmix/'
            credentials {
                username(rootProject.hasProperty('repoUser') ? rootProject['repoUser'] : 'admin')
                password(rootProject.hasProperty('repoPass') ? rootProject['repoPass'] : 'admin')
            }
        }
    }
}
```

These lines are necessary for resolving Jmix gradle plugin.

`allowInsecureProtocol true` instruction is required if your nexus repository uses http protocol.
Add this instruction to the maven repository configuration in the `build.gradle` file as well.

Remove the `mavenCentral()` instruction from the `repositories` section of the `build.gradle` file.

### Configure NPM
Copy previously saved package-lock.json file at hte root of your project.

To use custom npm registry create file `.npmrc` at the root of your project and add there the following line:
`registry=http://localhost:8081/repository/jmix-npm/`

Additional properties (like authentication, etc) can be found in documentation https://docs.npmjs.com/cli/v10/configuring-npm/npmrc


## Install Custom Nexus Repository

Download [Sonatype Nexus OSS](https://help.sonatype.com/repomanager3/product-information/download/download-archives---repository-manager-3).

Unzip the archive.

Go to the `bin` directory and run Nexus using the command:

On Linux:

```shell
./nexus run
```

On Windows:

```shell
nexus.exe /run
```

Open the URL in the browser: http://localhost:8081

Click the **Sign in** button and change the default admin password.

Go to the **Server administration and configuration** section and click the **Repositories** button.

![Repositories](images/repositories.png)

Click the **Create repository** button and select the `maven2 (hosted)` repository type.

Fill the **Name** field (e.g. `jmix`) and select the **Version policy**: `Mixed`.

![Repositories](images/repository-create.png)

Create NPM repository the same way - use `npm (hosted)` repository type and fill the **Name** (e.g. jmix-npm).


## Implementation Details 

### Resolver Project

The `deptool` utility resolves dependencies by invocation of gradle tasks in the `resolver` project that is packed into the distribution. This project contains a simple predefined `build.gradle` file. In this file Jmix plugin and Jmix BOM are enabled if corresponding project properties are defined (`-PjmixVersion` and `-PjmixPluginVersion`). The `build.gradle` also applies a special gradle plugin that contains tasks that do the resolution.

The `JmixDependenciesPlugin` adds the `resolveDependencies` task. This task is invoked by the `deptool` utility for dependencies resolution.

```shell
./gradlew resolveDependencies \
  --dependency javax.validation:validation-api:1.0.0.GA \
  --repository "http://some-external-repo.com:8081/repo|user|password" \
  -PjmixVersion=1.4.2 \
  -PjmixPluginVersion=1.4.2 \
  -PjmixLicenseKey=<your_key>
```

### NPM Resolver Project

The `deptool` utility resolves dependencies by invocation of gradle tasks in the `npm-resolver` project that is packed into the distribution. This project contains a simple predefined `build.gradle` file. In this file Jmix plugin and Jmix BOM are enabled if corresponding project properties are defined (`-PjmixVersion` and `-PjmixPluginVersion`). The `build.gradle` also applies a special gradle plugin that contains tasks that do the resolution.

The `JmixNpmDependenciesPlugin` adds the `resolveNpmDependencies` task. This task is invoked by the `deptool` utility for dependencies resolution.

Also, if you need to declare additional npm dependencies, you can add or edit `package-lock.json` file in [special directory](./deptool/src/main/resources/jmix-dependencies/additional/npm) for a suitable Jmix version.

```shell
./gradlew resolveDependencies \
  --dependency io.jmix.flowui:jmix-flowui \
  --repository "http://some-external-repo.com:8081/repo|user|password" \
  -PjmixVersion=2.1.0 \
  -PjmixPluginVersion=2.1.0 \
  -PjmixLicenseKey=<your_key>
```
Task configure project using provided data, resolve java dependencies and use Vaadin plugin (vaadinBuildFrontend task) to generate package-lock.json 


## Building the Distribution

If you build the official release, use Java 11 for this. 

To build the distribution locally run the following command to build the `deptool` tool distribution:

```shell
./gradlew zipDist
```

or pass the "version" parameter if you want to build the distribution with the specific version:

```shell
./gradlew zipDist -Pversion=1.2.3
```

The task produces the `build/distributions/deplool-<version>.zip` archive.

After extracting the archive use `bin/deptool.sh` or `bin/deptool.bat` files for executing CLI commands.

Distribution directories structure:

```text
deptool
  -  bin (contains CLI executables)
  -  lib (required Java libraries)
  -  resolver (Gradle project used by the tool to resolve dependencies)
  -  npm-resolver (Gradle project used by the tool to resolve npm dependencies)
```
