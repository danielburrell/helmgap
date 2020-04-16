package uk.co.solong.helmgap.descriptors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChartUrlDescriptor extends ChartDescriptor {
    private final String chartUrl;

    public ChartUrlDescriptor(String chartUrl) {
        this.chartUrl = chartUrl;
    }

    @Override
    public List<String> getDescriptor() {
        return Collections.singletonList(chartUrl);
    }

}
