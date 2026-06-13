package com.muasamcong.service.monitor;

import com.muasamcong.enums.BidStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidStatusResolver {
    private final Clock clock;

    public BidStatus resolveStatus(
            boolean hasContractInformation,
            boolean hasContractorSelectionResult,
            OffsetDateTime bidClosingTime,
            OffsetDateTime bidOpenTime
    ) {
        if (hasContractInformation) {
            return BidStatus.CONTRACT_INFORMATION_AVAILABLE;
        }

        if (hasContractorSelectionResult) {
            return BidStatus.CONTRACTOR_SELECTION_RESULT_AVAILABLE;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (bidOpenTime != null && !bidOpenTime.isAfter(now)) {
            return BidStatus.BID_OPENED;
        }

        if (bidClosingTime != null) {
            return bidClosingTime.isAfter(now)
                    ? BidStatus.INVITATION_OPEN
                    : BidStatus.BIDDING_CLOSED;
        }

        return BidStatus.INVITATION_OPEN;
    }
}
