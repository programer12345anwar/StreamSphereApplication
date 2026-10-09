package com.streamsphere.central.repository;

import com.streamsphere.central.entity.VideoReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoReportRepo extends JpaRepository<VideoReport, Integer> {
    Page<VideoReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
