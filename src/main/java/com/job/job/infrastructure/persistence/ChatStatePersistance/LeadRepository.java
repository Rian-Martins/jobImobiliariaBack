package com.job.job.infrastructure.persistence.ChatStatePersistance;


import com.job.job.application.dto.wahaDto.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {
}