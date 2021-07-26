package uk.co.solong.helmgap.kbld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KbldConfig {
    private String apiVersion;
    private String kind;
    private List<KbldOverride> overrides;

    public List<KbldOverride> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<KbldOverride> overrides) {
        this.overrides = overrides;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

}
