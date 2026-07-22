package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionCourseOffering;
import com.secretariapay.api.entity.enums.admission.AdmissionShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AdmissionCourseOfferingRepository extends JpaRepository<AdmissionCourseOffering, UUID> {

    @Query("""
            select offering
            from AdmissionCourseOffering offering
            join fetch offering.course course
            where offering.campaign.id = :campaignId
              and offering.active = true
              and course.active = true
            order by offering.departmentCode asc, course.name asc, offering.shift asc
            """)
    List<AdmissionCourseOffering> findActiveCatalog(@Param("campaignId") UUID campaignId);

    boolean existsByCampaignIdAndCourseIdAndShiftAndActiveTrue(
            UUID campaignId,
            UUID courseId,
            AdmissionShift shift
    );
}
