package com.muasamcong.repository;

import com.muasamcong.model.BiddingDocument;
import com.muasamcong.model.Contract;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingDocumentRepository extends JpaRepository<BiddingDocument, Long> {
    Optional<BiddingDocument> findByContractAndFileExternalId(Contract contract, String fileExternalId);

    List<BiddingDocument> findByContractOrderByCreatedAtAsc(Contract contract);

    List<BiddingDocument> findByContractAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
            Contract contract,
            String sourceType,
            String sourceRef
    );

    List<BiddingDocument> findByDownloadStatusOrderByCreatedAtAsc(String downloadStatus);

    List<BiddingDocument> findByContractAndDownloadStatusOrderByCreatedAtAsc(Contract contract, String downloadStatus);
}
