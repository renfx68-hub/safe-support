package com.safesupport.repository;

import com.safesupport.domain.ApplicationRecord;
import com.safesupport.domain.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<ApplicationRecord, Long> {

    List<ApplicationRecord> findTop20ByOrderByCreatedAtDesc();

    /** 兼容升级前 status 为空的历史数据 */
    @Query("""
            select a from ApplicationRecord a
            where a.effectiveDate <= :date and (a.status is null or a.status = :status)
            order by a.effectiveDate asc
            """)
    List<ApplicationRecord> findDue(@Param("status") RecordStatus status, @Param("date") LocalDate date);
}
