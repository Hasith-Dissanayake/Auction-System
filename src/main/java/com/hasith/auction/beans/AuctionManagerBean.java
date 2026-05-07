package com.hasith.auction.beans;

import jakarta.ejb.Singleton;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class AuctionManagerBean {

    private static final double MIN_INCREMENT = 0.01;

    private final Map<Integer, Double> auctionState = new ConcurrentHashMap<>();
    private final Map<Integer, String> currentHighestUser = new ConcurrentHashMap<>();
    private final Map<String, Double> userMaxBids = new ConcurrentHashMap<>();
    private final Set<String> autoBidUsers = ConcurrentHashMap.newKeySet();
    private final Map<Integer, LocalDateTime> startTimes = new ConcurrentHashMap<>();
    private final Map<Integer, LocalDateTime> endTimes = new ConcurrentHashMap<>();

    private final List<String> bidHistory = Collections.synchronizedList(new LinkedList<>());

    public AuctionManagerBean() {
        int auctionId = 1;
        LocalDateTime now = LocalDateTime.now();
        startTimes.put(auctionId, now);
        endTimes.put(auctionId, now.plusMinutes(1));
        auctionState.put(auctionId, 0.0);
        currentHighestUser.put(auctionId, "");
    }

    @Lock(LockType.READ)
    public boolean isAuctionActive(int auctionId) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTimes.getOrDefault(auctionId, now)) &&
                now.isBefore(endTimes.getOrDefault(auctionId, now));
    }

    @Lock(LockType.READ)
    public double getHighestBid(int auctionId) {
        return auctionState.getOrDefault(auctionId, 0.0);
    }

    @Lock(LockType.READ)
    public String getHighestBidder(int auctionId) {
        return currentHighestUser.getOrDefault(auctionId, "");
    }

    @Lock(LockType.READ)
    public List<String> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }

    @Lock(LockType.WRITE)
    public void placeBid(int auctionId, String userId, double maxBid, boolean autoBidEnabled) {
        double currentBid = getHighestBid(auctionId);

        if (autoBidEnabled) {
            userMaxBids.put(userId, maxBid);
            autoBidUsers.add(userId);
            addToHistory(userId, maxBid, true);
        } else {
            if (maxBid > currentBid) {
                auctionState.put(auctionId, maxBid);
                currentHighestUser.put(auctionId, userId);
                addToHistory(userId, maxBid, false);
            } else {
                addToHistory(userId, maxBid, false);
                return;
            }
        }

        simulateAutoBidding(auctionId);
    }

    private void simulateAutoBidding(int auctionId) {
        boolean updated;

        do {
            updated = false;
            double currentBid = getHighestBid(auctionId);
            String currentLeader = getHighestBidder(auctionId);

            List<Map.Entry<String, Double>> eligible = autoBidUsers.stream()
                    .map(user -> Map.entry(user, userMaxBids.getOrDefault(user, 0.0)))
                    .filter(e -> e.getValue() >= currentBid + MIN_INCREMENT)
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toList());

            if (eligible.isEmpty()) return;

            String topUser = eligible.get(0).getKey();
            double topMax = eligible.get(0).getValue();

            if (eligible.size() == 1) {
                if (!topUser.equals(currentLeader)) {
                    double newBid = Math.min(topMax, currentBid + MIN_INCREMENT);
                    auctionState.put(auctionId, newBid);
                    currentHighestUser.put(auctionId, topUser);
                    addToHistory(topUser, newBid, true);
                    updated = true;
                }
            } else {
                double secondMax = eligible.get(1).getValue();
                double newBid = Math.min(topMax, secondMax + MIN_INCREMENT);

                if (newBid > currentBid || !topUser.equals(currentLeader)) {
                    auctionState.put(auctionId, newBid);
                    currentHighestUser.put(auctionId, topUser);
                    addToHistory(topUser, newBid, true);
                    updated = true;
                }
            }

        } while (updated);
    }

    private void addToHistory(String userId, double amount, boolean auto) {
        String log = String.format("[%s] %s placed RM %.2f %s",
                LocalDateTime.now().withNano(0),
                userId,
                amount,
                auto ? "(Auto-Bid)" : "");
        bidHistory.add(0, log);
    }

    @Lock(LockType.READ)
    public LocalDateTime getAuctionStartTime(int auctionId) {
        return startTimes.getOrDefault(auctionId, LocalDateTime.now());
    }

    @Lock(LockType.READ)
    public LocalDateTime getAuctionEndTime(int auctionId) {
        return endTimes.getOrDefault(auctionId, LocalDateTime.now());
    }




}
