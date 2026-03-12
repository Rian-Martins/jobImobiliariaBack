package com.job.job.infrastructure.persistence.ChatStatePersistance;

import com.job.job.domain.model.wahaEntities.AtendimentoWaha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtendimentoWahaRepository extends JpaRepository<AtendimentoWaha, Long> {
}

