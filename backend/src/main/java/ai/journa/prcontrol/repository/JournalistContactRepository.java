package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.JournalistContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JournalistContactRepository extends JpaRepository<JournalistContact, Long> {
  List<JournalistContact> findByJournalistId(Long journalistId);

  JournalistContact findFirstByEmailIgnoreCase(String email);

  @Query("select distinct contact.journalist.id from JournalistContact contact " +
      "where lower(contact.email) like lower(concat('%', :needle, '%'))")
  List<Long> findJournalistIdsByEmailLike(@Param("needle") String needle);

  @Query("select distinct contact.journalist.id from JournalistContact contact " +
      "where lower(contact.phone) like lower(concat('%', :needle, '%'))")
  List<Long> findJournalistIdsByPhoneLike(@Param("needle") String needle);

  @Query("select distinct contact.journalist.id from JournalistContact contact")
  List<Long> findJournalistIdsWithContacts();
}
