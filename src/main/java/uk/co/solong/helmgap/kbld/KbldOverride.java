package uk.co.solong.helmgap.kbld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KbldOverride {
    private String image;
    private String newImage;
    private Boolean preresolved;

    public Boolean getPreresolved() {
        return preresolved;
    }

    public void setPreresolved(Boolean preresolved) {
        this.preresolved = preresolved;
    }

    public String getNewImage() {
        return newImage;
    }

    public void setNewImage(String newImage) {
        this.newImage = newImage;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
