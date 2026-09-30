package com.example.urlshortener.repository;

import com.example.urlshortener.entity.UrlEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UrlEntity u WHERE u.shortCode = :shortCode")
    Optional<UrlEntity> findByShortCodeForUpdate(@Param("shortCode") String shortCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UrlEntity u WHERE u.normalizedOriginalUrl = :normalizedUrl")
    Optional<UrlEntity> findByNormalizedOriginalUrlForUpdate(@Param("normalizedUrl") String normalizedUrl);

    Optional<UrlEntity> findByNormalizedOriginalUrl(String normalizedUrl);

    long countByNormalizedOriginalUrl(String normalizedUrl);

    @Modifying
    @Transactional
    @Query("UPDATE UrlEntity u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = :accessedAt WHERE u.shortCode = :shortCode")
    int incrementClickStats(@Param("shortCode") String shortCode, @Param("accessedAt") Instant accessedAt);

    boolean existsByShortCode(String shortCode);
}
