package com.job.job.infrastructure.persistence.ChatStatePersistance;

import com.job.job.application.dto.wahaDto.ChatState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatStateRepository extends JpaRepository<ChatState, String> {
}