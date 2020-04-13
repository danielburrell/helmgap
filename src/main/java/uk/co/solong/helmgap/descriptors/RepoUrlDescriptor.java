package uk.co.solong.helmgap.descriptors;

import java.util.Arrays;
import java.util.List;

public final class RepoUrlDescriptor extends ChartDescriptor{
    private final String repoUrl;
    private final String chartName;
    private final String version;

    public RepoUrlDescriptor(String repoUrl, String chartName, String version) {
        this.repoUrl = repoUrl;
        this.chartName = chartName;
        this.version = version;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public String getChartName() {
        return chartName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    @Override
    public List<String> getDescriptor() {
        return Arrays.asList("--repo", repoUrl, "--version", version, chartName);
    }

    @Override
    public String getName() {
        return chartName;
    }
}
