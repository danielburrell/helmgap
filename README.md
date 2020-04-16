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
    <version>1.6</version>
</dependency>
```

2. Call the library
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byShortName("stable", "hackmd", "0.1.0");
HelmGap helmgap = new HelmGap();
AirgapInstall result = helmgap.buildAirgap(chartDescriptor);
File imagesArchive = result.getAirgapInstallerArchive(); //<-- this is your airgap!
```

If you inspect imagesArchive, you'll find `hackmd-airgap-0.1.0.tgz` with all the images inside.

---

# Optional Features
Optionally, you can also get a copy of the original helm chart as well (handy!):
```java
File chart = files.getOriginalChart()
```
In this case this would return `hackmd-0.1.0.tgz` - the original helm chart.

# Advanced Usage
There are 3 ways to tell HelmGap where your chart is located. All correspond to the `helm pull` syntax:
- Short Name (can be used when the repository already exists on the machine via `helm repo add`)
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byShortName("stable", "hackmd", "0.1.0");
```
- Repo URL (can be used when the repo url is known, but the repo has not necessarily been added to the machine)
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byRepoUrl("https://kubernetes-charts.storage.googleapis.com", "hackmd", "0.1.0");
```
- Chart URL (use if only the chart url is known)
```java
ChartDescriptor chartDescriptor = ChartDescriptor.byChartUrl("http://storage.googleapis.com/kubernetes-charts/dask-1.1.0.tgz");
```
