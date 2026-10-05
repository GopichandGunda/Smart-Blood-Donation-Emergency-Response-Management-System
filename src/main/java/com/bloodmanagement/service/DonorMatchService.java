package com.bloodmanagement.service;

import com.bloodmanagement.dao.AuditLogDAO;
import com.bloodmanagement.dao.DonorMatchDAO;
import java.util.List;

public final class DonorMatchService {
    private final DonorMatchDAO matches;
    private final AuditLogDAO audit;

    public DonorMatchService(DonorMatchDAO matches, AuditLogDAO audit) {
        this.matches = matches;
        this.audit = audit;
    }

    public void save(long requestId, List<DonorMatchingService.MatchResult> ranked, long actorId) {
        matches.saveMatches(requestId, ranked);
        audit.record(actorId, "DONORS_MATCHED", ranked.size() + " candidates matched to request #" + requestId);
    }

    public List<DonorMatchDAO.Offer> findOffers(long donorUserId) {
        return matches.findOffers(donorUserId);
    }

    public void respond(long matchId, long donorUserId, boolean accepted) {
        matches.respond(matchId, donorUserId, accepted);
        audit.record(donorUserId, accepted ? "DONOR_ACCEPTED_MATCH" : "DONOR_DECLINED_MATCH",
                "Donor response recorded for match #" + matchId);
    }
}
