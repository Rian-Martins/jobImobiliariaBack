package com.job.job.infrastructure.persistence;

import com.job.job.domain.repository.DemoRepository;
import com.job.job.domain.model.Demo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DemoRepositoryImpl implements DemoRepository {

    private final DemoJpaRepository jpaRepository;

    @Override
    public List<Demo> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Demo> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Demo save(Demo entity) {
        DemoEntity jpaEntity = toJpaEntity(entity);
        DemoEntity saved = jpaRepository.save(jpaEntity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private Demo toDomain(DemoEntity entity) {
        return Demo.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    private DemoEntity toJpaEntity(Demo domain) {
        DemoEntity entity = new DemoEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        return entity;
    }
}
