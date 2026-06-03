# Loyalty Points Service Demo

This repo is a demo Spring Boot application which implements a simple loyalty
points service. The application supports the following operations on customer
accounts:

- Earning reward points for purchases.
- Redeeming reward points for rewards.
- Checking the balance of a customer account.
- Expiration of unspent points.
- Removing (clawing back) reward points from a customer account if a purchase
  is refunded.

Additionally, the application supports a set of "loyalty tiers"; a customer is
assigned to a tier based on the total customer spend over the past 12 months.
The current tier for each customer account is returned as part of the balance
information for the account.

## AI tools used
All of the code in this repository was written by hand; AI tools were not used
directly to write any code.

Copilot and ChatGPT were used to:
- research frameworks for hosting a web application like Spring Boot
- answer Java syntax and other Java language questions
- research options for sqlite integration such as JPA; and to answer questions
  about JPA (e.g., how to write SQL queries for repository methods)
- research options for how to write a simple CLI in Java and answer questions
  about how to integrate Spring Shell into the demo application.

## Running the code
To run the demo application, open a Developer Command Prompt or Powershell, 
and run `mvnw.cmd spring-boot:run`. The application will listen on a local port
(8080 by default). You can interact with the REST API using `curl` or another
HTTP client of your choice (see below for detailed API documentation).

The application comes pre-configured with a small set of demo purchase and 
reward data; please see the `data.sql` file for the demo dataset.

### Using the shell
The demo application includes a simple shell which can be used to perform
operations on customer accounts. After the application starts, you should see
a prompt like `shell:>` in your terminal. You can interact with the shell using
the following commands:

- `balance --accountId <accountId>`
- `earn --accountId <accountId> --purchaseId <purchaseId>`
- `redeem --accountId <accountId> --rewardId <rewardId>`
- `clawback --accountId <accountId> --purchaseId <purchaseId>`

### Exiting
To exit the application, simply type Ctrl+C in your terminal.

## Running tests
The demo application includes a full set of unit tests. To run them, open the
project in VS Code and use the _Java: Run Tests_ command.

## Design
### Code layout
The demo application is composed of the following layers:

- Controller
  The controller defines API routes and connects them to the service object.

- DTOs
  Data Type Objects used to model request and response types.

- Service
  The service object implements the application business logic, such as earning
  and redeeming points. Logic such as the expiry time for points, the conversion
  rate from dollar spend to points earned, etc. are located in this layer.

- Entities/Repositories
  Data access is primarily done via JPA, through a set of entity/repository
  objects. Each domain object (for example, a customer purchase) is modeled as
  an entity (Purchase.java) and an associated repository (PurchaseRepository.java).

### Data model
The most important tables in the data model are the tables used for tracking a
user's point balance: `points_lots`, and `points_transactions`. `points_lots`
tracks the unspent points earned by the user, by purchase, while `points_transactions`
is an append-only transaction log of all points transactions made by the system.

Initially it may seem that only the `points_transactions` are needed to track a
user's current balance, but this is not sufficient to support expiration of 
unspent points. To see why, consider a sequence of transactions like the 
following:

- EARN 100, expires Jan 2026
- REDEEM 50
- EARN 25, expires Jan 2027

If we simply filter out expired EARN transactions when computing the customer's
balance, then after the initial EARN transaction has expired, we will incorrectly
report a balance of -25 points for the user!

To address this issue, the system maintains a table of `points_lots`. Each row
in this table is a single "lot" of points earned from a single purchase. The
remaining unspent points for each lot are tracked, as well as the lot's expiration
time. Storing lots in this way simplifies the balance computation (we simply sum
the remaining points for any unexpired lots). New lots are created whenever points
are earned, and any spent or clawed back points are removed from the unexpired
lots for the account, in FIFO order (oldest lots are used first). Lots are stored
with an expiration time, instead of a creation time, to allow for updates to the
valid point lifetime that are not retroactive (e.g., the code can be updated to
change the lifetime of any new points earned without affecting the expiration
time of any already earned).

Note that this requires scanning the lots table each time a balance needs to be
computed. In a real system, this may become prohibitively expensive as the number
of users and purchases grows. To mitigate this issue, several possible options
could be considered:

- expired lots, or lots with a 0 balance remaining, could be garbage collected
  from the table periodically, as these have no effect on balance computations

- since a user's balance can only change with points are earned, expire, or are
  spent/clawed back, a pre-computed user balance could be maintained (in the
  form of a cache) when these events occur.

While the `points_lots` table is already sufficient to determine user balances
and process transactions, the system also maintains a separate `points_transactions`
table. This table acts as durable, append-only log of points transactions which
have been processed. In the event that the `points_lots` table is lost or otherwise
damaged, the current state of all lots can be rebuilt by replaying the transaction
log. The `points_transactions` table is also used to ensure that duplicate 
transactions (such as earning points for the same purchase multiple times) are
not possible. Finally note that `points_transactions` intentionally does not 
attempt to model the transaction -> purchase relationship using a foreign key; 
this is to ensure that the transaction log remains valid, even if purchases are
somehow removed from the db after a transaction is performed.

The data model for the application includes several other tables: the `purchases`,
`rewards`, and `loyalty_tiers` tables. These are used to store purchase, reward,
and loyalty tier information, respectively:

- The `purchases` table stores data about each purchase made by a customer, such
  as the accountId, purchaseId, and dollar amount of the purchase. The system
  uses this data to validate that purchase IDs are valid, that the purchase was
  made by the user attempting to earn the points for it, and to look up the 
  purchase amount for conversion to reward points. In a real application, this
  data would likely come from an external system, such as another service or
  an external db.

- The `rewards` table stores data about the available rewards, such as the ID of
  each reward ("free-coffee") and their point costs.

- The `loyalty_tiers` table stores data about the available loyalty tiers, such
  as the tier name ("GOLD") and the required customer spend over the past 12
  months for each tier. This makes it easy to add/remove tiers and to update
  the spending threshold for each tier.

### Tradeoffs

- Initially, I tried to model the points balance using a simple EARN/REDEEM
  transaction log, as noted above. This is simpler then maintaining lots, however
  it cannot handle expiration, so the concept of lots was introduced, even though
  this adds additional complexity.

- When points are clawed back, we intentionally reduce the points remaining in
  the customer's unexpired lots, in FIFO order, but we do not allow the balance
  to become negative. If the user doesn't have enough points remaining to repay
  all the clawed back points, then there balance simply becomes 0. This 
  simplifies the code somewhat (we never need to worry about negative balances)
  but it does open up a possibility that a customer can spent more points then
  they've earned. For example, if a user makes a purchase, spends the reward
  points, and then refunds the purchase, their final balance will be 0. In the 
  future, if the same customer makes another purchase, they will be able to 
  spend the resulting reward points. If we instead allowed negative balances, 
  this customer would need to "pay back" the refunded reward points before they
  could redeem additional rewards.

### What's missing
If I'd had more time, I would have added the following:
- integration tests
- an improved CLI and/or web front end
- added user accounts as an entity (to allow using usernames in API, map to 
  accountId on backend)
- probably lots more stuff I'm not thining of... :)

## REST API

All endpoints are located under the `/api/points/<accountId>` prefix.

### `balance`
Returns the account balance.

#### Request
```
GET /api/points/account-100/balance
```

#### Response
```
{
  "accountId": "account-100",
  "points": 0,
  "loyaltyTierName": "Silver"
}
```

### `earn`
Earns points for a purchase ID.

#### Request
```
POST /api/points/account-100/earn
Content-Type: application/json

{
  "purchaseId": "order-122"
}
```

#### Response
No response.

### `redeem`
#### Request
```
POST /api/points/account-100/redeem
Content-Type: application/json

{
  "rewardId": "free-coffee"
}
```

#### Response
No response.

### `clawback`
#### Request
```
POST /api/points/account-100/clawback
Content-Type: application/json

{
  "purchaseId": "order-124"
}
```

#### Response
No response.