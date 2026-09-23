package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.InterestRateTierEntity;

public interface InterestRateTierJpaRepository extends JpaRepository<InterestRateTierEntity, Long> {

    List<InterestRateTierEntity> findByActiveTrueOrderByMinTermMonthsAsc();

    @Query("""
            select t from InterestRateTierEntity t
            where t.active = true and :term between t.minTermMonths and t.maxTermMonths
            order by t.minTermMonths asc
            limit 1
            """)
    Optional<InterestRateTierEntity> findActiveForTerm(@Param("term") int term);
}
