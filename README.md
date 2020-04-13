# helmgap

Helmgap allows you to deploy a Helm Chart into airgap environments by generating a container image registry archive for a given chart. Under the hood it wraps a combination of Helm and kbld (k14s) to firstly generate the manifest with images and then derive the chart images.

## Requirements
You will need helm (`brew install helm`) and kbld (`brew tap k14s/tap && brew install kbld`)
Java 9+

## Usage

1.  Add the helmgap dependency to your maven pom.xml
```xml
<dependency>
    <groupId>uk.co.solong</groupId>
    <artifactId>helmgap</artifactId>
    <version>1.0</version>
</dependency>
```

1.  A
