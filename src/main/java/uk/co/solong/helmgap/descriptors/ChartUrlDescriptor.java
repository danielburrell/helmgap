package uk.co.solong.helmgap.descriptors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChartUrlDescriptor extends ChartDescriptor {
    private final String chartUrl;
    private final String friendlyName;
    private final String version;

    public ChartUrlDescriptor(String chartUrl, String friendlyName, String version) {
        this.chartUrl = chartUrl;
        this.friendlyName = friendlyName;
        this.version = version;
    }

    public String getChartUrl() {
        return chartUrl;
    }

    @Override
    public List<String> getDescriptor() {
        return Collections.singletonList(chartUrl);
    }

    @Override
    public String getName() {
        return friendlyName;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
