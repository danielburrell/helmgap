package uk.co.solong.helmgap.descriptors;

import java.util.Arrays;
import java.util.List;

public class ShortDescriptor extends ChartDescriptor{
    private final String repoName;
    private final String chartName;
    private final String version;

    public ShortDescriptor(String repoName, String chartName, String version) {
        this.repoName = repoName;
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

    public String getRepoName() {
        return repoName;
    }

    @Override
    public List<String> getDescriptor() {
        return Arrays.asList("--version", version, repoName+"/"+chartName);
    }

    @Override
    public String getName() {
        return chartName;
    }
}
