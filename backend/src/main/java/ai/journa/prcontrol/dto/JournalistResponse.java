package ai.journa.prcontrol.dto;

public class JournalistResponse {
    private Long id;
    private String name;
    private String outlet;
    private String beatTags;
    private String location;
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutlet() {
        return outlet;
    }

    public void setOutlet(String outlet) {
        this.outlet = outlet;
    }

    public String getBeatTags() {
        return beatTags;
    }

    public void setBeatTags(String beatTags) {
        this.beatTags = beatTags;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
