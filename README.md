# RAFM Analytics Platform

**Revenue Assurance & Fraud Management** — a Spring Boot backend that ingests
real telecom customer data, detects six classes of revenue and fraud anomalies
across billing/payment/usage streams, and routes flagged cases to an agentic
AI investigator that produces structured, auditable analyst reports.

> **Status:** MVP complete. Local dev only. All transactional data is synthetic
> or derived; customer dimension is loaded from the public IBM Telco Customer
> Churn dataset.

---

## What it does

- Ingests **7,043 real customer records** from the IBM Telco Customer Churn
  dataset (public, no auth required).
- Generates derived billing and payment streams from each customer's real
  `MonthlyCharges` and `tenure` fields (~68K billing rows, ~68K payment rows).
- Runs **six rule-based anomaly detectors** across the data:
  - `DUPLICATE_CHARGE` — two SUCCESS payments for the same billing within 10 minutes
  - `BILLING_DISCREPANCY` — sum of payments differs from billed amount
  - `UNUSUAL_USAGE_SPIKE` — usage record more than 3× a customer's mean
  - `PAYMENT_FAILURE_PATTERN` — 3+ FAILED payments in 30 days
  - `REVENUE_LEAKAGE` — heavy usage with no billing record in 30 days
  - `HIGH_RISK_CHURN_SIGNAL` — HIGH-tier customer with one or more failed payments
- Sends any flagged anomaly through an **agentic AI investigation workflow**
  that gathers customer/billing/payment/usage context, classifies the issue,
  scores risk, recommends an action, and emits a multi-line analyst report.
- Persists every investigation as a structured audit record with a full
  step-by-step trace.

## Sample output (real data)

After ingesting the IBM dataset and running detection on ~7,100 customers
(7,043 real + 50 synthetic with planted anomalies):

```
By type:
  HIGH_RISK_CHURN_SIGNAL: ~737   (real findings on real customer data)
  UNUSUAL_USAGE_SPIKE: 26
  BILLING_DISCREPANCY: 11
  DUPLICATE_CHARGE: 7

By severity:
  HIGH: 744
  MEDIUM: 37
```

Investigation output for one HIGH_RISK_CHURN_SIGNAL anomaly:

```json
{
  "anomalyId": 45,
  "anomalyType": "HIGH_RISK_CHURN_SIGNAL",
  "severity": "HIGH",
  "classification": "Credit & retention risk - high-tier customer with payment failures",
  "explanation": "HIGH-tier customer with 4 failed payment(s); strong churn / collections risk",
  "evidence": {
    "customerId": 9,
    "customerRiskTier": "HIGH",
    "billingRecordCount": 12,
    "paymentRecordCount": 12,
    "trace": [
      "step=receive id=45 type=HIGH_RISK_CHURN_SIGNAL",
      "step=customer_lookup found=true",
      "step=context_gather bills=12 pays=12 usage=0",
      "step=classify result=Credit & retention risk - high-tier customer with payment failures",
      "step=risk_score value=1.00",
      "step=recommend action=Escalate to fraud ops; freeze related transactions pending review.",
      "step=report_generated len=609"
    ]
  },
  "recommendedAction": "Escalate to fraud ops; freeze related transactions pending review.",
  "confidenceScore": 1.0,
  "generatedReport": "RAFM Investigation Report\n========================\n..."
}
```

---

## Architecture

```
                                 ┌──────────────────────┐
   IBM Telco CSV ───────────►    │  Ingestion Service   │
                                 │  (CSV → Customer +   │
                                 │   derived bills/pays)│
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
   Synthetic Generator ────►     │   PostgreSQL / H2    │
   (planted anomalies)           │   6 entity tables    │
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
                                 │ AnomalyDetection     │
                                 │ Service (6 rules)    │
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
   POST /api/investigations  ──► │ AnomalyInvestigation │
                                 │ Agent + AgentTools   │
                                 │  • customerLookup    │
                                 │  • billingLookup     │
                                 │  • paymentLookup     │
                                 │  • usageLookup       │
                                 │  • classify          │
                                 │  • riskScore         │
                                 │  • recommend         │
                                 │  • generateReport    │
                                 └──────────┬───────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
                                 │ investigation_reports│
                                 │ (JSON + narrative)   │
                                 └──────────────────────┘
```

**Tech stack:** Java 17, Spring Boot 3.2, Spring Data JPA, Hibernate, H2
(local dev) / PostgreSQL (Docker), Apache Commons CSV, Lombok, Maven.

---

## Quick start

### Requirements
- Java 17
- Maven 3.9+
- The IBM Telco CSV (downloaded in Step 1 below)

### Run locally with H2 in-memory DB

```bash
git clone https://github.com/<your-username>/rafm-analytics
cd rafm-analytics

# Step 1 - download the IBM dataset (~950 KB)
mkdir -p data/raw
curl -L -o data/raw/telco-churn.csv \
  "https://raw.githubusercontent.com/IBM/telco-customer-churn-on-icp4d/master/data/Telco-Customer-Churn.csv"

# Step 2 - start the app
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. H2 web console at
`http://localhost:8080/h2` (JDBC URL `jdbc:h2:mem:rafm`, user `sa`, no password).

### Run in Docker with Postgres

```bash
docker compose up --build
```

---

## Demo flow

```bash
# 1) Ingest the full IBM dataset (~7,043 customers, ~68K derived rows)
curl -s -X POST 'http://localhost:8080/api/admin/ingest-telco' \
  --data-urlencode 'maxRows=0' | python -m json.tool

# 2) Layer 50 synthetic customers with planted anomalies on top
curl -s -X POST 'http://localhost:8080/api/admin/seed?customers=50' | python -m json.tool

# 3) Run all six anomaly detectors
curl -s -X POST http://localhost:8080/api/admin/detect | python -m json.tool | head -40

# 4) See what was found, grouped by type
curl -s "http://localhost:8080/api/anomalies?status=OPEN" | python -c "
import sys,json,collections
d=json.load(sys.stdin)
print(f'{len(d)} open anomalies')
[print(f'  {t}: {n}') for t,n in collections.Counter(a['type'] for a in d).most_common()]
"

# 5) Investigate the first HIGH_RISK_CHURN_SIGNAL with the agent
ID=$(curl -s "http://localhost:8080/api/anomalies?status=OPEN" \
  | python -c "import sys,json;d=json.load(sys.stdin);h=[a for a in d if a['type']=='HIGH_RISK_CHURN_SIGNAL'];print(h[0]['id'])")
curl -s -X POST "http://localhost:8080/api/investigations/$ID" | python -m json.tool
```

---

## API reference

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/admin/ingest-telco?path=&maxRows=` | Ingest IBM CSV (path defaults to `data/raw/telco-churn.csv`, `maxRows=0` means all 7,043) |
| POST | `/api/admin/seed?customers=N` | Add synthetic customers with planted anomalies |
| POST | `/api/admin/detect` | Run all six rules now |
| GET  | `/api/customers` | List customers |
| GET  | `/api/customers/{id}` | Fetch customer |
| POST | `/api/customers` | Create customer |
| GET  | `/api/billing?customerId=` | List billing records |
| POST | `/api/billing` | Create billing record |
| GET  | `/api/usage?customerId=` | List usage records |
| POST | `/api/usage` | Create usage record |
| GET  | `/api/payments?customerId=&billingId=` | List payments |
| POST | `/api/payments` | Create payment |
| GET  | `/api/anomalies?status=OPEN` | List anomalies |
| GET  | `/api/anomalies/{id}` | Fetch anomaly |
| POST | `/api/investigations/{anomalyId}` | Run agent on an anomaly |
| GET  | `/api/investigations/{anomalyId}` | Fetch saved investigation report |

---

## Honest data disclosure

This is a portfolio project. Recruiters reading this should know exactly
what's real and what's not:

- **Real:** the 7,043 customer records (from IBM's public Telco Customer
  Churn dataset). Fields used include `customerID`, `gender`, `tenure`,
  `Contract`, `PaymentMethod`, `MonthlyCharges`, `Churn`.
- **Derived from real fields:** monthly billing records (one per month of
  tenure, capped at 12, amount = `MonthlyCharges`) and matching payments
  (status varies based on the `Churn` flag to seed realistic failure patterns).
- **Synthetic:** the 50 supplemental customers added by the seeder, with
  intentionally planted duplicate charges, billing/payment mismatches, and
  usage spikes. Used purely so the detection rules have something to find
  in categories the IBM dataset doesn't cover.
- **Not real:** any throughput claims, any production deployment claims,
  any auth/multi-tenant features. The app runs locally only.

---

## What's NOT in this MVP (and is on the roadmap)

| Capability | Status | Next milestone |
|---|---|---|
| Real LLM reasoning in the agent | rule-based today | swap `AgentTools` for tool-calling local LLM via Ollama |
| Persistent DB across restarts | H2 in-memory | Postgres profile via `docker compose up` |
| Authentication / authorization | none | Spring Security + JWT |
| Web dashboard | curl/H2 console only | React + Vite frontend |
| Tests | smoke test only | JUnit 5 + Testcontainers Postgres |
| Anomaly resolution endpoint | status only flips to INVESTIGATING | `PATCH /api/anomalies/{id}` to mark RESOLVED |
| Detection at scale | in-memory `findAll()` loops | move rules into SQL queries with indexes |

---

## Project framing

### Inspired by the McKinsey Forward Program

The project's framing — start from the business problem, structure the
investigation, communicate to a stakeholder — reflects Forward Program
principles:

- **Structured problem solving** — the agent's pipeline (anomaly → evidence →
  classification → risk score → recommendation → report) mirrors a hypothesis-
  driven analytical approach.
- **Stakeholder thinking** — every investigation produces a report formatted
  for a finance analyst persona, not a developer.
- **Digital transformation** — replaces a hypothetical spreadsheet-driven
  reconciliation workflow with an API-first, auditable pipeline.

### Designed with AMD AI Developer Program principles in mind

The agent layer is intentionally built as a deterministic, tool-using workflow
so it can later be swapped for a local LLM (e.g., a quantized model served via
Ollama) without changing the API contract or downstream consumers. The current
implementation is rule-based and runs on commodity hardware; it is **designed
to support** local AI inference (an AMD Developer Program area of focus) in a
future phase, not claim to require AMD hardware today. Every agent decision
emits a confidence score and a full step trace for auditability — a
deliberate responsible-AI design choice.

---

## Project structure

```
rafm-analytics/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── data/
│   └── raw/
│       └── telco-churn.csv          # downloaded from IBM
└── src/
    └── main/
        ├── java/com/rafm/analytics/
        │   ├── RafmAnalyticsApplication.java
        │   ├── controller/
        │   │   ├── AdminController.java
        │   │   ├── AnomalyController.java
        │   │   ├── BillingController.java
        │   │   ├── CustomerController.java
        │   │   ├── InvestigationController.java
        │   │   ├── PaymentController.java
        │   │   └── UsageController.java
        │   ├── model/
        │   │   ├── Anomaly.java
        │   │   ├── BillingRecord.java
        │   │   ├── Customer.java
        │   │   ├── InvestigationReport.java
        │   │   ├── PaymentRecord.java
        │   │   └── UsageRecord.java
        │   ├── repository/           # 6 Spring Data JPA repositories
        │   └── service/
        │       ├── AnomalyDetectionService.java
        │       ├── SyntheticDataService.java
        │       ├── TelcoCsvIngestionService.java
        │       └── agent/
        │           ├── AgentTools.java
        │           └── AnomalyInvestigationAgent.java
        └── resources/
            └── application.yml
```

---

## License

MIT (or whichever you prefer — pick one when you publish).

## Acknowledgments

- IBM for publishing the Telco Customer Churn dataset
- The Spring Boot, Hibernate, and Lombok project teams
