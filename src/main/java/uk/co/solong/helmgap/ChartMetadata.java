package uk.co.solong.helmgap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartMetadata {
    private final String name;
    private final String version;

    public ChartMetadata(@JsonProperty("name") String name, @JsonProperty("version") String version) {
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
