package com.miabogado.domain.lawyer.repository;

import com.miabogado.domain.lawyer.entity.PracticeArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PracticeAreaRepository extends JpaRepository<PracticeArea, UUID> {

    List<PracticeArea> findByActiveTrueOrderByNameAsc();

    Set<PracticeArea> findByIdIn(Set<UUID> ids);
}
