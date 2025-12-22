package ai.journa.prcontrol.repository;

import ai.journa.prcontrol.domain.Journalist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JournalistRepository extends JpaRepository<Journalist, Long> {
    @Query("select distinct j from Journalist j left join j.beats b " +
            "where (:beat is null or lower(b.name) = lower(:beat)) " +
            "and (:outlet is null or lower(j.outlet) like lower(concat('%', :outlet, '%'))) " +
            "and (:location is null or lower(j.location) like lower(concat('%', :location, '%')))")
    List<Journalist> search(@Param("beat") String beat,
                            @Param("outlet") String outlet,
                            @Param("location") String location);
}
