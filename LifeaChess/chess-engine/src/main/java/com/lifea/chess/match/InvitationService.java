package com.lifea.chess.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Pending challenges. Pure logic: the caller decides what "online" and "busy"
 * mean and supplies those checks. Thread-confined to the game thread.
 */
public final class InvitationService {
    private final LongSupplier now;
    private final long expiryMillis;
    private final Map<UUID, Invitation> byId = new LinkedHashMap<>();

    public InvitationService(long expiryMillis) {
        this(expiryMillis, System::currentTimeMillis);
    }

    public InvitationService(long expiryMillis, LongSupplier nowMillis) {
        this.expiryMillis = expiryMillis;
        this.now = nowMillis;
    }

    public enum Failure { SELF_INVITE, ALREADY_PENDING, NONE }

    public record SendResult(Invitation invitation, Failure failure) {
        public boolean ok() {
            return failure == Failure.NONE;
        }
    }

    public SendResult send(UUID from, String fromName, UUID to, String toName,
                           TimeControl control, boolean ranked) {
        purgeExpired();
        if (from.equals(to)) {
            return new SendResult(null, Failure.SELF_INVITE);
        }
        for (Invitation inv : byId.values()) {
            if (inv.from().equals(from) && inv.to().equals(to)) {
                return new SendResult(inv, Failure.ALREADY_PENDING);
            }
        }
        long t = now.getAsLong();
        Invitation inv = new Invitation(UUID.randomUUID(), from, fromName, to, toName, control, ranked, t, t + expiryMillis);
        byId.put(inv.id(), inv);
        return new SendResult(inv, Failure.NONE);
    }

    /** Most recent live invitation addressed to {@code player}. */
    public Optional<Invitation> latestFor(UUID player) {
        purgeExpired();
        Invitation found = null;
        for (Invitation inv : byId.values()) {
            if (inv.to().equals(player)) {
                found = inv;
            }
        }
        return Optional.ofNullable(found);
    }

    public Optional<Invitation> pendingFrom(UUID player) {
        purgeExpired();
        for (Invitation inv : byId.values()) {
            if (inv.from().equals(player)) {
                return Optional.of(inv);
            }
        }
        return Optional.empty();
    }

    /** Accepts the newest invitation for {@code player} and clears every other one involving either party. */
    public Optional<Invitation> accept(UUID player) {
        Optional<Invitation> inv = latestFor(player);
        inv.ifPresent(i -> {
            byId.remove(i.id());
            clearInvolving(i.from());
            clearInvolving(i.to());
        });
        return inv;
    }

    public Optional<Invitation> deny(UUID player) {
        Optional<Invitation> inv = latestFor(player);
        inv.ifPresent(i -> byId.remove(i.id()));
        return inv;
    }

    public Optional<Invitation> cancel(UUID from) {
        Optional<Invitation> inv = pendingFrom(from);
        inv.ifPresent(i -> byId.remove(i.id()));
        return inv;
    }

    public void clearInvolving(UUID player) {
        byId.values().removeIf(i -> i.from().equals(player) || i.to().equals(player));
    }

    /** Removes expired invitations and returns them so callers can notify players. */
    public List<Invitation> purgeExpired() {
        long t = now.getAsLong();
        List<Invitation> expired = new ArrayList<>();
        Iterator<Invitation> it = byId.values().iterator();
        while (it.hasNext()) {
            Invitation inv = it.next();
            if (inv.isExpired(t)) {
                expired.add(inv);
                it.remove();
            }
        }
        return expired;
    }

    public List<Invitation> all() {
        purgeExpired();
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    public int size() {
        purgeExpired();
        return byId.size();
    }
}
