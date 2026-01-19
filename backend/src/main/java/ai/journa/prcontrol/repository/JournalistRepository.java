package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Journalist;
import ai.journa.prcontrol.domain.JournalistVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JournalistRepository extends JpaRepository<Journalist, Long> {
  Optional<Journalist> findByPublicationDomainIgnoreCaseAndFullNameIgnoreCase(String publicationDomain, String fullName);

  Optional<Journalist> findByLinkedinIgnoreCase(String linkedin);

  List<Journalist> findByVerificationStatus(JournalistVerificationStatus status);

  @Query(value = """
      select *
      from journalists
      where lower(publication_domain) = lower(:domain)
        and (
          lower(full_name) = lower(:name)
          or exists (
            select 1
            from unnest(aliases) alias_value
            where lower(alias_value) = lower(:name)
          )
        )
      limit 1
      """, nativeQuery = true)
  Optional<Journalist> findByPublicationDomainAndNameOrAlias(@Param("domain") String domain,
                                                             @Param("name") String name);

  @Query(value = """
      select *
      from journalists
      where lower(publication_name) = lower(:publication)
        and (
          lower(full_name) = lower(:name)
          or exists (
            select 1
            from unnest(aliases) alias_value
            where lower(alias_value) = lower(:name)
          )
        )
      limit 1
      """, nativeQuery = true)
  Optional<Journalist> findByPublicationNameAndNameOrAlias(@Param("publication") String publication,
                                                           @Param("name") String name);

  List<Journalist> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}
