package com.streamsphere.central.repository;

import com.streamsphere.central.entity.VideoReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoReportRepo extends JpaRepository<VideoReport, Integer> {
    Page<VideoReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select report from VideoReport report
            where (:status is null or upper(report.status) = :status)
              and (:search is null
                   or lower(report.reason) like lower(concat('%', :search, '%'))
                   or lower(coalesce(report.description, '')) like lower(concat('%', :search, '%'))
                   or lower(report.video.name) like lower(concat('%', :search, '%'))
                   or (:reportId is not null and report.id = :reportId))
            order by report.createdAt desc
            """)
    Page<VideoReport> searchReports(@Param("status") String status,
                                    @Param("search") String search,
                                    @Param("reportId") Integer reportId,
                                    Pageable pageable);
}
