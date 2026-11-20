package com.safesupport.repository;

import com.safesupport.domain.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<ApplicationRecord, Long> {

    List<ApplicationRecord> findTop20ByOrderByCreatedAtDesc();
}
