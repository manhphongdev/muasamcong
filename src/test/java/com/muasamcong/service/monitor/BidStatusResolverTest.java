package com.muasamcong.service.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import com.muasamcong.enums.BidStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BidStatusResolverTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    private final BidStatusResolver resolver = new BidStatusResolver(CLOCK);

    @Test
    void resolvesContractInformationWithHighestPriority() {
        BidStatus status = resolver.resolveStatus(
                true,
                true,
                NOW.plusDays(1),
                NOW.minusHours(1)
        );
        assertThat(status).isEqualTo(BidStatus.CONTRACT_INFORMATION_AVAILABLE);
    }

    @Test
    void resolvesContractorSelectionResultBeforeBidOpening() {
        BidStatus status = resolver.resolveStatus(
                false,
                true,
                NOW.plusDays(1),
                NOW.minusHours(1)
        );

        assertThat(status).isEqualTo(BidStatus.CONTRACTOR_SELECTION_RESULT_AVAILABLE);
    }

    @Test
    void resolvesBidOpenedWhenOpenTimePassed() {
        BidStatus status = resolver.resolveStatus(
                false,
                false,
                NOW.plusDays(1),
                NOW
        );

        assertThat(status).isEqualTo(BidStatus.BID_OPENED);
    }

    @Test
    void resolvesInvitationOpenWhenClosingTimeIsFuture() {
        BidStatus status = resolver.resolveStatus(
                false,
                false,
                NOW.plusMinutes(1),
                null
        );

        assertThat(status).isEqualTo(BidStatus.INVITATION_OPEN);
    }

    @Test
    void resolvesBiddingClosedWhenClosingTimePassed() {
        BidStatus status = resolver.resolveStatus(
                false,
                false,
                NOW,
                null
        );

        assertThat(status).isEqualTo(BidStatus.BIDDING_CLOSED);
    }

    @Test
    void defaultsToInvitationOpenWhenNoDataMatches() {
        BidStatus status = resolver.resolveStatus(false, false, null, null);

        assertThat(status).isEqualTo(BidStatus.INVITATION_OPEN);
    }
}
