package uk.co.solong.helmgap.descriptors;

import java.util.List;

public abstract class ChartDescriptor {

    public static UrlDescriptor byChartUrl(String url, String friendlyName, String version) {
        return new UrlDescriptor(url, friendlyName, version);
    }

    public static RepoUrlDescriptor byRepoUrl(String repoUrl, String chartName, String version) {
        return new RepoUrlDescriptor(repoUrl, chartName, version);
    }

    public static ShortDescriptor byShortName(String repoName, String chartName, String version) {
        return new ShortDescriptor(repoName, chartName, version);
    }

    public abstract List<String> getDescriptor();
    public abstract String getName();

    public abstract String getVersion();
}
