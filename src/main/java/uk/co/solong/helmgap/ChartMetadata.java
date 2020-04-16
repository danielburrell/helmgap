package uk.co.solong.helmgap;

public class ChartMetadata {
    private final String name;
    private final String version;

    public ChartMetadata(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }
}
