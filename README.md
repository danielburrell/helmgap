# helmgap

Helmgap allows you to deploy a Helm Chart into airgap environments by generating a container image registry archive for a given chart. Under the hood it wraps a combination of Helm and kbld (k14s) to firstly generate the manifest with images and then derive the chart images.

The end product is a tar file containing all the images required by the chart which can be read using `docker load registry.tar` or `crd import registry.tar`

## Requirements
You will need 
- helm (`brew install helm`)
- kbld (`brew tap k14s/tap && brew install kbld`)
- Java 9+
- Linux

## Usage

1. Add the helmgap dependency to your maven pom.xml
```xml
<dependency>
    <groupId>uk.co.solong</groupId>
    <artifactId>helmgap</artifactId>
    <version>1.7</version>
</dependency>
```

2. Call the library
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byShortName("bitnami", "redis", "6.0.0");
HelmGap helmgap = new HelmGap();
AirgapInstall result = helmgap.buildAirgap(chartDescriptor);
File imageArchive = result.getAirgapInstallerArchive(); //the airgap registry you requested.
File lockFile = result.getLockFile(); //the associated lockfile requried for loading.

```

The above `imageArchive` file is
 - `redis-airgap-6.0.0.tgz` - the container images required by the helm chart, in KBLD image format.

The above `lockFile` file is 
 - `redis-6.0.0-lock.yml` - the mapping between the sha256 id of each image and the image reference in the chart.

You can also get a copy of the helm chart (useful!).
```java
File chart = files.getOriginalChart(); //the original chart
```

The above chart file is
 - `redis-6.0.0.tgz` - the original helm chart.

That's it!

# Advanced Usage
There are 3 different chart descriptors supported which cover all valid `helm pull` syntax:

- Short Name (can be used when the repository already exists on the machine via `helm repo add`)
    ```java
    ChartDescriptor chartDescriptor = ChartDescriptor.byShortName("stable", "hackmd", "0.1.0");
    ```

- Repo URL (can be used when the repo url is known, but the repo has not necessarily been added to the machine)
    ```java
    ChartDescriptor chartDescriptor = ChartDescriptor.byRepoUrl("https://charts.bitnami.com/bitnami", "hackmd", "0.1.0");
    ```

- Chart URL (use if only the chart url is known)
    ```java
    ChartDescriptor chartDescriptor = ChartDescriptor.byChartUrl("http://storage.googleapis.com/kubernetes-charts/dask-1.1.0.tgz");
    ```

- Local Archive (use when the chart is a tgz file on local disk)
    ```java
    ChartDescriptor chartDescriptor = ChartDescriptor.byArchive("/home/user/dask-1.1.0.tgz");
    ```

## Specifying helm and kbld

By default, helmgap will use your PATH to find helm and kbld. 
If they're not on the path, or you want to select different binaries, you can configure this as follows:

```java
new HelmGap("/path/to/helm", "/path/to/kbld")
```
