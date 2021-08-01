package uk.co.solong.helmgap.descriptors;

import uk.co.solong.helmgap.chartstrategy.ChartStrategy;
import uk.co.solong.helmgap.chartstrategy.HelmStrategy;

import java.io.File;
import java.util.List;

public abstract class ChartDescriptor {

    /**
     * Tell helmgap where the chart is located by supplying a URL
     * @param url - the url where the chart is e.g.
     *      https://charts.bitnami.com/bitnami/mychart-0.1.0.tgz
     * @return A ChartDescriptor which can be passed to HelmGap::buildAirgap
     */
    public static ChartDescriptor byChartUrl(String url) {
        return new ChartUrlDescriptor(url);
    }

    /**
     * Tell helmgap where the chart is located by supplying a URL
     * @param repoUrl - the repoUrl where the chart is e.g.
     *      https://charts.bitnami.com/bitnami.
     * @param chartName - the name of the chart e.g. nginx.
     * @param version - the version of the chart e.g. 0.1.0
     *
     * @return A ChartDescriptor which can be passed to HelmGap::buildAirgap
     */
    public static ChartDescriptor byRepoUrl(String repoUrl, String chartName, String version) {
        return new RepoUrlDescriptor(repoUrl, chartName, version);
    }

    /**
     * Tell helmgap where the chart is located by supplying a URL
     * @param repoName - the repoName as given when doing helm repo add xyz. e.g. 'stable'
     * @param chartName - the name of the chart e.g. nginx.
     * @param version - the version of the chart e.g. 0.1.0
     *
     * @return A ChartDescriptor which can be passed to HelmGap::buildAirgap
     */
    public static ChartDescriptor byShortName(String repoName, String chartName, String version) {
        return new ShortDescriptor(repoName, chartName, version);
    }

    /**
     * Tell helmgap where the chart is located by pointing to an archive on disk
     * @param archive - the archive file.
     *
     * @return A ChartDescriptor which can be passed to HelmGap::buildAirgap
     */
    public static ChartDescriptor byArchive(File archive) {
        return new ArchiveDescriptor(archive);
    }

    public abstract List<String> getDescriptor();

    public ChartStrategy getChartStrategy() {
        return new HelmStrategy();
    }

}
