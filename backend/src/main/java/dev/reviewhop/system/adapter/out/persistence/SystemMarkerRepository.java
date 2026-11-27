package dev.reviewhop.system.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SystemMarkerRepository extends JpaRepository<SystemMarkerEntity, Short> {
}
