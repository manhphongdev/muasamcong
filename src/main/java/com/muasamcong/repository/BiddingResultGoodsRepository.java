package com.muasamcong.repository;

import com.muasamcong.model.BiddingResultGoods;
import com.muasamcong.model.Contract;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiddingResultGoodsRepository extends JpaRepository<BiddingResultGoods, Long> {
    void deleteByContract(Contract contract);

    List<BiddingResultGoods> findByContractOrderBySortOrderAsc(Contract contract);

    List<BiddingResultGoods> findByNotifyNoOrderBySortOrderAsc(String notifyNo);
}
