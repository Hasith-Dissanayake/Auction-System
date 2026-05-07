<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Live Auction</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>

<body class="bg-light">

<div class="container py-5">
    <div class="card shadow mb-4">
        <div class="card-body">
            <h1 class="card-title">Live Auction</h1>
            <h4 class="text-muted">Item: <strong>Smartwatch</strong></h4>

            <p><strong>Auction Ends In:</strong> <span id="countdown">loading...</span></p>

            <p class="mb-1">
                <strong>Current Highest Bid:</strong>
                $<span class="text-primary" id="highestBid">${highestBid}</span>
            </p>
            <p class="mb-3">
                <strong>Current Highest Bidder:</strong>
                <span class="text-success" id="highestBidder">${highestBidder}</span>
            </p>

            <form action="bid" method="post" class="row gy-3 gx-3 align-items-center">
                <div class="col-auto">
                    <input type="text" name="userId" id="userId" class="form-control" placeholder="Your Name"
                           value="${param.userId}" required>
                </div>
                <div class="col-auto">
                    <input type="number" step="0.01" min="0.01" name="bidAmount" id="bidAmount" class="form-control"
                           placeholder="Enter your max bid" required>
                </div>
                <div class="col-auto">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" name="autoBid" id="autoBid" checked>
                        <label class="form-check-label" for="autoBid">Enable Auto-Bid</label>
                    </div>
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-success" id="submitBtn">Place Bid</button>
                </div>
            </form>

            <p class="text-muted fst-italic small mt-2">
                When enabled, the system will automatically outbid others up to your maximum.
                Bids are only accepted during the auction time.
            </p>

            <c:if test="${not empty message}">
                <div class="alert alert-success mt-3" role="alert">
                        ${message}
                </div>
            </c:if>
        </div>
    </div>


    <div class="card shadow">
        <div class="card-header bg-dark text-white">
            Bid History (Latest on Top)
        </div>
        <div class="card-body" style="max-height: 300px; overflow-y: auto;">
            <ul class="list-group" id="bidHistoryList">
                <c:forEach var="log" items="${bidHistory}">
                    <li class="list-group-item">${log}</li>
                </c:forEach>
            </ul>
        </div>
    </div>
</div>


<script>
    function fetchLatestAuctionData() {
        fetch('bid?ajax=true&details=true')
            .then(response => response.json())
            .then(data => {
                document.getElementById('highestBid').textContent = data.highestBid;
                document.getElementById('highestBidder').textContent = data.highestBidder;
            })
            .catch(err => console.error('Error fetching auction data:', err));
    }

    setInterval(fetchLatestAuctionData, 5000);
</script>


<script>
    function fetchBidHistory() {
        fetch('bid?ajax=true&history=true')
            .then(res => res.json())
            .then(data => {
                const historyContainer = document.getElementById('bidHistoryList');
                historyContainer.innerHTML = '';
                data.forEach(entry => {
                    const li = document.createElement('li');
                    li.className = 'list-group-item';
                    li.textContent = entry;
                    historyContainer.appendChild(li);
                });
            })
            .catch(err => console.error('Bid history fetch failed:', err));
    }

    setInterval(fetchBidHistory, 5000);
</script>


<script>
    const endTime = new Date('${auctionEnd}').getTime();

    function updateCountdown() {
        const now = new Date().getTime();
        const distance = endTime - now;

        if (distance <= 0) {
            document.getElementById("countdown").innerText = "Auction ended";


            document.getElementById("bidAmount").disabled = true;
            document.getElementById("submitBtn").disabled = true;
            document.getElementById("autoBid").disabled = true;
            document.getElementById("userId").disabled = true;


            const winner = document.getElementById("highestBidder").textContent;
            const finalBid = document.getElementById("highestBid").textContent;

            const resultMsg = document.createElement("div");
            resultMsg.className = "alert alert-info mt-3";
            resultMsg.innerHTML = `<strong> Auction Ended!</strong> Winner.`;

            document.querySelector(".card-body").appendChild(resultMsg);


            clearInterval(countdownInterval);
            return;
        }

        const hours = Math.floor(distance / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);

        document.getElementById("countdown").innerText =
            hours.toString().padStart(2, '0') + "h " +
            minutes.toString().padStart(2, '0') + "m " +
            seconds.toString().padStart(2, '0') + "s";
    }

    updateCountdown();
    const countdownInterval = setInterval(updateCountdown, 1000);
</script>


<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
