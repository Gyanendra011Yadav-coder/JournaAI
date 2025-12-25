package ai.journa.prcontrol.dto;

import java.util.List;

public class JournalistResponse {
  private Long id;
  private String fullName;
  private String name;
  private String publicationName;
  private String publicationDomain;
  private String designation;
  private String linkedin;
  private List<String> beats;
  private String country;
  private String city;
  private String journeySummary;
  private String verificationStatus;
  private int completenessScore;
  private List<JournalistArticleSummary> articles;
  private List<JournalistContactResponse> contacts;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPublicationName() {
    return publicationName;
  }

  public void setPublicationName(String publicationName) {
    this.publicationName = publicationName;
  }

  public String getPublicationDomain() {
    return publicationDomain;
  }

  public void setPublicationDomain(String publicationDomain) {
    this.publicationDomain = publicationDomain;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public String getLinkedin() {
    return linkedin;
  }

  public void setLinkedin(String linkedin) {
    this.linkedin = linkedin;
  }

  public List<String> getBeats() {
    return beats;
  }

  public void setBeats(List<String> beats) {
    this.beats = beats;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getJourneySummary() {
    return journeySummary;
  }

  public void setJourneySummary(String journeySummary) {
    this.journeySummary = journeySummary;
  }

  public String getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(String verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public int getCompletenessScore() {
    return completenessScore;
  }

  public void setCompletenessScore(int completenessScore) {
    this.completenessScore = completenessScore;
  }

  public List<JournalistArticleSummary> getArticles() {
    return articles;
  }

  public void setArticles(List<JournalistArticleSummary> articles) {
    this.articles = articles;
  }

  public List<JournalistContactResponse> getContacts() {
    return contacts;
  }

  public void setContacts(List<JournalistContactResponse> contacts) {
    this.contacts = contacts;
  }
}
