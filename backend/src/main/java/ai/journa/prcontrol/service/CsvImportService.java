package ai.journa.prcontrol.service;

import ai.journa.prcontrol.domain.ContactSourceType;
import ai.journa.prcontrol.domain.ContactVisibility;
import ai.journa.prcontrol.domain.ImportJob;
import ai.journa.prcontrol.domain.ImportJobRow;
import ai.journa.prcontrol.domain.ImportJobStatus;
import ai.journa.prcontrol.domain.ImportRowStatus;
import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistContact;
import ai.journa.prcontrol.domain.User;
import ai.journa.prcontrol.repository.ImportJobRepository;
import ai.journa.prcontrol.repository.ImportJobRowRepository;
import ai.journa.prcontrol.repository.JournalistContactRepository;
import ai.journa.prcontrol.repository.JournalistRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CsvImportService {
  private final ImportJobRepository importJobRepository;
  private final ImportJobRowRepository importJobRowRepository;
  private final JournalistRepository journalistRepository;
  private final JournalistContactRepository contactRepository;
  private final ObjectMapper objectMapper;

  public CsvImportService(ImportJobRepository importJobRepository,
                          ImportJobRowRepository importJobRowRepository,
                          JournalistRepository journalistRepository,
                          JournalistContactRepository contactRepository,
                          ObjectMapper objectMapper) {
    this.importJobRepository = importJobRepository;
    this.importJobRowRepository = importJobRowRepository;
    this.journalistRepository = journalistRepository;
    this.contactRepository = contactRepository;
    this.objectMapper = objectMapper;
  }

  public ImportJob importCsv(MultipartFile file, boolean dryRun, User actor) {
    ImportJob job = new ImportJob();
    job.setStatus(ImportJobStatus.RUNNING);
    job.setDryRun(dryRun);
    job.setCreatedBy(actor);
    job.setCreatedAt(Instant.now());
    job.setSourceName(file.getOriginalFilename());
    job = importJobRepository.save(job);

    int created = 0;
    int updated = 0;
    int conflicts = 0;
    int errors = 0;

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
         CSVParser parser = CSVFormat.DEFAULT
             .withFirstRecordAsHeader()
             .withIgnoreHeaderCase()
             .withTrim()
             .parse(reader)) {
      int rowNumber = 1;
      for (CSVRecord record : parser) {
        ImportJobRow row = new ImportJobRow();
        row.setJob(job);
        row.setRowNumber(rowNumber++);
        Map<String, String> payload = record.toMap();
        row.setPayloadJsonb(writeJson(payload));

        String email = record.get("email");
        String linkedin = record.isMapped("linkedin") ? record.get("linkedin") : null;
        String fullName = record.get("full_name");
        String publicationDomain = record.get("publication_domain");
        Optional<Journalist> match = Optional.empty();

        if (email != null && !email.isBlank()) {
          JournalistContact contact = contactRepository.findFirstByEmailIgnoreCase(email);
          if (contact != null) {
            match = Optional.of(contact.getJournalist());
          }
        }
        if (match.isEmpty() && linkedin != null && !linkedin.isBlank()) {
          match = journalistRepository.findByLinkedinIgnoreCase(linkedin);
        }
        if (match.isEmpty() && fullName != null && !fullName.isBlank()) {
          String domain = publicationDomain != null ? publicationDomain : "";
          match = journalistRepository.findByPublicationDomainIgnoreCaseAndFullNameIgnoreCase(domain, fullName);
        }

        if (match.isPresent()) {
          Journalist journalist = match.get();
          if (!dryRun) {
            applyJournalistFields(journalist, record);
            journalistRepository.save(journalist);
            applyContacts(journalist, record, actor);
          }
          row.setJournalist(journalist);
          row.setStatus(ImportRowStatus.UPDATED);
          updated++;
        } else if (fullName == null || fullName.isBlank()) {
          row.setStatus(ImportRowStatus.CONFLICT);
          row.setMessage("Missing full_name");
          conflicts++;
        } else {
          if (!dryRun) {
            Journalist journalist = new Journalist();
            journalist.setFullName(fullName.trim());
            applyJournalistFields(journalist, record);
            journalist = journalistRepository.save(journalist);
            applyContacts(journalist, record, actor);
            row.setJournalist(journalist);
          }
          row.setStatus(ImportRowStatus.CREATED);
          created++;
        }
        importJobRowRepository.save(row);
      }
      job.setStatus(ImportJobStatus.COMPLETED);
    } catch (Exception ex) {
      errors++;
      job.setStatus(ImportJobStatus.FAILED);
      job.setSummaryJsonb(writeJson(Map.of("error", ex.getMessage())));
    }

    Map<String, Object> summary = new HashMap<>();
    summary.put("created", created);
    summary.put("updated", updated);
    summary.put("conflicts", conflicts);
    summary.put("errors", errors);
    job.setSummaryJsonb(writeJson(summary));
    return importJobRepository.save(job);
  }

  private void applyJournalistFields(Journalist journalist, CSVRecord record) {
    String publicationName = record.get("publication_name");
    String publicationDomain = record.get("publication_domain");
    String designation = record.isMapped("designation") ? record.get("designation") : null;
    String beats = record.isMapped("beats") ? record.get("beats") : null;
    String aliases = record.isMapped("aliases") ? record.get("aliases") : null;
    String publicationAliases = record.isMapped("publication_aliases") ? record.get("publication_aliases") : null;
    String topicKeywords = record.isMapped("topic_keywords") ? record.get("topic_keywords") : null;
    String languages = record.isMapped("languages") ? record.get("languages") : null;
    String coverageRegions = record.isMapped("coverage_regions") ? record.get("coverage_regions") : null;
    String otherLinks = record.isMapped("other_links") ? record.get("other_links") : null;
    String country = record.isMapped("country") ? record.get("country") : null;
    String city = record.isMapped("city") ? record.get("city") : null;
    String linkedin = record.isMapped("linkedin") ? record.get("linkedin") : null;

    if (publicationName != null && !publicationName.isBlank()) {
      journalist.setPublicationName(publicationName.trim());
    }
    if (publicationDomain != null && !publicationDomain.isBlank()) {
      journalist.setPublicationDomain(publicationDomain.trim());
    }
    if (designation != null && !designation.isBlank()) {
      journalist.setDesignation(designation.trim());
    }
    if (beats != null && !beats.isBlank()) {
      journalist.setBeats(splitValues(beats));
    }
    if (aliases != null && !aliases.isBlank()) {
      journalist.setAliases(splitValues(aliases));
    }
    if (publicationAliases != null && !publicationAliases.isBlank()) {
      journalist.setPublicationAliases(splitValues(publicationAliases));
    }
    if (topicKeywords != null && !topicKeywords.isBlank()) {
      journalist.setTopicKeywords(splitValues(topicKeywords));
    }
    if (languages != null && !languages.isBlank()) {
      journalist.setLanguages(splitValues(languages));
    }
    if (coverageRegions != null && !coverageRegions.isBlank()) {
      journalist.setCoverageRegions(splitValues(coverageRegions));
    }
    if (otherLinks != null && !otherLinks.isBlank()) {
      journalist.setOtherLinks(splitValues(otherLinks));
    }
    if (country != null && !country.isBlank()) {
      journalist.setCountry(country.trim());
    }
    if (city != null && !city.isBlank()) {
      journalist.setCity(city.trim());
    }
    if (linkedin != null && !linkedin.isBlank()) {
      journalist.setLinkedin(linkedin.trim());
    }
  }

  private String[] splitValues(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return java.util.Arrays.stream(value.split("[;,]"))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .distinct()
        .toArray(String[]::new);
  }

  private void applyContacts(Journalist journalist, CSVRecord record, User actor) {
    String email = record.get("email");
    String phone = record.isMapped("phone") ? record.get("phone") : null;
    String sourceType = record.isMapped("source_type") ? record.get("source_type") : null;
    if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
      return;
    }
    JournalistContact contact = new JournalistContact();
    contact.setJournalist(journalist);
    contact.setEmail(email != null ? email.trim() : null);
    contact.setPhone(phone != null ? phone.trim() : null);
    contact.setVisibility(ContactVisibility.PUBLIC);
    contact.setSourceType(parseSourceType(sourceType));
    contact.setVerifiedBy(actor);
    contact.setVerifiedAt(Instant.now());
    contactRepository.save(contact);
  }

  private ContactSourceType parseSourceType(String value) {
    if (value == null || value.isBlank()) {
      return ContactSourceType.MANUAL;
    }
    try {
      return ContactSourceType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return ContactSourceType.MANUAL;
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return null;
    }
  }
}
