package com.hasith.auction.web;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Topic;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import com.hasith.auction.beans.AuctionManagerBean;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/bid")
public class BidServlet extends HttpServlet {

    @EJB
    private AuctionManagerBean auctionManager;

    @Resource(lookup = "jms/auctionTopic")
    private Topic auctionTopic;

    @Inject
    private JMSContext jmsContext;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String userId = req.getParameter("userId");
            if (userId == null || userId.isBlank()) {
                userId = "Anonymous";
            }

            double bidAmount = Double.parseDouble(req.getParameter("bidAmount"));
            boolean autoBidEnabled = req.getParameter("autoBid") != null;
            double currentBid = auctionManager.getHighestBid(1);

            if (!auctionManager.isAuctionActive(1)) {
                req.setAttribute("message", "Auction is not active. Please try again later.");
            } else if (bidAmount > currentBid) {
                auctionManager.placeBid(1, userId, bidAmount, autoBidEnabled);

                if (autoBidEnabled) {
                    req.setAttribute("message", "Your maximum bid has been recorded. Auto-bidding is enabled.");
                } else {
                    req.setAttribute("message", "Your manual bid was placed successfully.");
                }

                String message = "Bid by " + userId + ": $" + bidAmount + (autoBidEnabled ? " (Auto-Bid ON)" : "");
                jmsContext.createProducer().send(auctionTopic, message);

            } else {
                req.setAttribute("message", "Bid too low. Please enter a higher amount.");
            }

        } catch (NumberFormatException e) {
            req.setAttribute("message", "Invalid bid amount.");
        }

        setAuctionAttributes(req);
        req.getRequestDispatcher("/index.jsp").forward(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {


        if ("true".equals(req.getParameter("ajax")) && "true".equals(req.getParameter("details"))) {
            double bid = auctionManager.getHighestBid(1);
            String bidder = auctionManager.getHighestBidder(1);

            String json = String.format("{\"highestBid\":\"%.2f\", \"highestBidder\":\"%s\"}", bid, bidder);
            resp.setContentType("application/json");
            resp.getWriter().write(json);
            return;
        }


        if ("true".equals(req.getParameter("ajax")) && "true".equals(req.getParameter("history"))) {
            List<String> history = auctionManager.getBidHistory();
            String json = history.stream()
                    .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                    .collect(Collectors.joining(",", "[", "]"));

            resp.setContentType("application/json");
            resp.getWriter().write(json);
            return;
        }


        setAuctionAttributes(req);
        req.getRequestDispatcher("/index.jsp").forward(req, resp);
    }

    private void setAuctionAttributes(HttpServletRequest req) {
        req.setAttribute("highestBid", auctionManager.getHighestBid(1));
        req.setAttribute("highestBidder", auctionManager.getHighestBidder(1));
        req.setAttribute("auctionStart", auctionManager.getAuctionStartTime(1).toString());
        req.setAttribute("auctionEnd", auctionManager.getAuctionEndTime(1).toString());
        req.setAttribute("bidHistory", auctionManager.getBidHistory());
    }
}
