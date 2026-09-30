package com.example.urlshortener.repository;

import com.example.urlshortener.entity.ReleasedShortCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReleasedShortCodeRepository extends JpaRepository<ReleasedShortCodeEntity, Long> {

    @Query(value = "SELECT * FROM released_short_codes ORDER BY released_at, id LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<ReleasedShortCodeEntity> claimNextForUpdate();
}