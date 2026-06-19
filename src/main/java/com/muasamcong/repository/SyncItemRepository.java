package com.muasamcong.repository;

import com.muasamcong.model.SyncItem;
import com.muasamcong.enums.BidPackageSyncStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncItemRepository extends JpaRepository<SyncItem, Long> {
    Optional<SyncItem> findByNotifyNo(String notifyNo);

    List<SyncItem> findByNotifyNoIn(Collection<String> notifyNos);

    @Query("SELECT item FROM SyncItem item " +
           "WHERE item.syncStatus IN :syncStatuses " +
           "ORDER BY CASE WHEN item.sourceParentPath IS NULL THEN 1 ELSE 0 END ASC, " +
           "item.sourceParentPath ASC, " +
           "CASE WHEN item.sourceOrder IS NULL THEN 1 ELSE 0 END ASC, " +
           "item.sourceOrder ASC, item.createdAt ASC, item.id ASC")
    List<SyncItem> findSyncQueue(
            @Param("syncStatuses") Collection<BidPackageSyncStatus> syncStatuses,
            Pageable pageable
    );

    @Query("SELECT item FROM SyncItem item " +
           "WHERE item.syncStatus = :syncStatus " +
           "ORDER BY CASE WHEN item.sourceParentPath IS NULL THEN 1 ELSE 0 END ASC, " +
           "item.sourceParentPath ASC, " +
           "CASE WHEN item.sourceOrder IS NULL THEN 1 ELSE 0 END ASC, " +
           "item.sourceOrder ASC, item.lastSyncedAt ASC, item.id ASC")
    List<SyncItem> findRefreshQueue(
            @Param("syncStatus") BidPackageSyncStatus syncStatus,
            Pageable pageable
    );

    @Query("SELECT item FROM SyncItem item " +
           "LEFT JOIN Contract c ON item.notifyNo = c.notifyNo " +
           "LEFT JOIN ContractInfo ci ON c.id = ci.contract.id AND ci.status = com.muasamcong.enums.RecordStatus.ACTIVE " +
           "LEFT JOIN Investor inv ON ci.investor.id = inv.id " +
           "LEFT JOIN Bidding b ON b.contract = c " +
           "WHERE (:search IS NULL OR :search = '' " +
           "  OR LOWER(item.notifyNo) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(ci.bidName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(inv.investorName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR :status = '' " +
           "  OR (:status = 'Đang mời thầu' AND c.bidStatus = com.muasamcong.enums.BidStatus.INVITATION_OPEN) " +
           "  OR (:status = 'Đã mở thầu' AND c.bidStatus = com.muasamcong.enums.BidStatus.BID_OPENED) " +
           "  OR (:status = 'Đã đóng thầu' AND c.bidStatus = com.muasamcong.enums.BidStatus.BIDDING_CLOSED) " +
           "  OR (:status = 'Có KQLCNT' AND c.bidStatus = com.muasamcong.enums.BidStatus.CONTRACTOR_SELECTION_RESULT_AVAILABLE) " +
           "  OR (:status = 'Có thông tin hợp đồng' AND c.bidStatus = com.muasamcong.enums.BidStatus.CONTRACT_INFORMATION_AVAILABLE)) " +
           "AND (:kpiFilter IS NULL OR :kpiFilter = '' OR :kpiFilter = 'ALL' " +
           "  OR (:kpiFilter = 'SHIFT_24H' AND c.bidStatus = com.muasamcong.enums.BidStatus.INVITATION_OPEN AND b.bidCloseAt > CURRENT_TIMESTAMP AND b.bidCloseAt <= :oneDayAhead) " +
           "  OR (:kpiFilter = 'NO_THHD' AND (item.sourcePath IS NULL OR item.sourcePath = '')) " +
           "  OR (:kpiFilter = 'CLOSED_NO_RESULT' AND (c.bidStatus = com.muasamcong.enums.BidStatus.BIDDING_CLOSED OR c.bidStatus = com.muasamcong.enums.BidStatus.BID_OPENED) AND NOT EXISTS (SELECT br FROM BiddingResult br JOIN br.biddingContractor bc JOIN bc.bidOpening bo WHERE bo.contract = c AND br.bidResult = 1)) " +
           "  OR (:kpiFilter = 'HAS_RESULT' AND (c.bidStatus = com.muasamcong.enums.BidStatus.CONTRACTOR_SELECTION_RESULT_AVAILABLE OR c.bidStatus = com.muasamcong.enums.BidStatus.CONTRACT_INFORMATION_AVAILABLE))) " +
           "ORDER BY " +
           "CASE WHEN item.sourceParentPath IS NULL THEN 1 ELSE 0 END ASC, " +
           "item.sourceParentPath ASC, " +
           "CASE WHEN item.sourceOrder IS NULL THEN 1 ELSE 0 END ASC, " +
           "item.sourceOrder ASC, " +
           "CASE " +
           "  WHEN c.bidStatus = com.muasamcong.enums.BidStatus.INVITATION_OPEN AND b.bidCloseAt > CURRENT_TIMESTAMP AND b.bidCloseAt <= :oneDayAhead THEN 1 " +
           "  WHEN c.bidStatus = com.muasamcong.enums.BidStatus.INVITATION_OPEN THEN 2 " +
           "  WHEN c.bidStatus = com.muasamcong.enums.BidStatus.BID_OPENED THEN 3 " +
           "  WHEN c.bidStatus = com.muasamcong.enums.BidStatus.BIDDING_CLOSED THEN 4 " +
           "  WHEN c.bidStatus = com.muasamcong.enums.BidStatus.CONTRACTOR_SELECTION_RESULT_AVAILABLE THEN 5 " +
           "  WHEN c.bidStatus = com.muasamcong.enums.BidStatus.CONTRACT_INFORMATION_AVAILABLE THEN 6 " +
           "  ELSE 7 " +
           "END ASC, item.createdAt ASC, item.id ASC")
    Page<SyncItem> searchTracking(
            @Param("search") String search,
            @Param("status") String status,
            @Param("kpiFilter") String kpiFilter,
            @Param("oneDayAhead") OffsetDateTime oneDayAhead,
            Pageable pageable
    );
}
