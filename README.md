# helmgap

Helmgap allows you to deploy a Helm Chart into airgap environments by generating a container image registry archive for a given chart. Under the hood it wraps a combination of Helm and kbld (k14s) to firstly generate the manifest with images and then derive the chart images.

The end product is a tar file containing all the images required by the chart which can be read using `docker load registry.tar` or `crd import registry.tar`

## Requirements
You will need 
- helm (`brew install helm`)
- kbld (`brew tap k14s/tap && brew install kbld`)
- Java 9+

## Usage

1. Add the helmgap dependency to your maven pom.xml
```xml
<dependency>
    <groupId>uk.co.solong</groupId>
    <artifactId>helmgap</artifactId>
    <version>1.0</version>
</dependency>
```

2. Call the library
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byShortName("stable", "hackmd", "0.1.0");
HelmGap helmgap = new HelmGap();
AirgapInstall files = helmgap.buildAirgap(chartDescriptor);
File chart = files.getChartPullArchive(); //the chart you requested.
File registry = files.getRegistryArchive(); //the airgap registry you requested.
```

That's it!
# Advanced Usage
There are 3 different chart descriptors supported:
- Short Name (use when the repository already exists on the machine via `helm repo add`)
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byShortName("stable", "hackmd", "0.1.0");
```
- Repo URL (use when the repo url is known but not necessarily on the machine)
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byRepoUrl("https://kubernetes-charts.storage.googleapis.com", "hackmd", "0.1.0");
```
- Chart URL (use when the chart url is known - you must still provide a friendly name and version for the chart, it cannot be derived from the url, this may change in future)
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byChartUrl("http://storage.googleapis.com/kubernetes-charts/dask-1.1.0.tgz", "dask", "1.1.0");
```
