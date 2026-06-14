package com.muasamcong.repository;

import com.muasamcong.model.BidClarification;
import com.muasamcong.model.BidClarificationContent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidClarificationContentRepository extends JpaRepository<BidClarificationContent, Long> {
    Optional<BidClarificationContent> findByClarificationAndExternalId(BidClarification clarification, String externalId);

    List<BidClarificationContent> findByClarificationOrderBySortOrderAsc(BidClarification clarification);
}
