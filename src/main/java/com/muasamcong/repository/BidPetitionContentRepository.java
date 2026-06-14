package com.muasamcong.repository;

import com.muasamcong.model.BidPetition;
import com.muasamcong.model.BidPetitionContent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidPetitionContentRepository extends JpaRepository<BidPetitionContent, Long> {
    List<BidPetitionContent> findByPetitionOrderBySortOrderAsc(BidPetition petition);

    void deleteByPetition(BidPetition petition);
}
