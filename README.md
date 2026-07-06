Real-Time Fraud Detection System


Overview
In today's distributed fintech ecosystem, rapid transaction processing across various services demands a robust fraud detection system. This README outlines the design and implementation of a prototype for real-time fraud detection without relying on external libraries specifically for anomaly detection.

Algorithm Description
The system processes a stream of transactions, each containing a timestamp, transaction amount, user ID, and service ID. Fraudulent patterns such as rapid transactions across services, unusually high amounts, and irregular sequences are identified. Key algorithmic 
components include:
Tracking users' activities within a time window to detect patterns.
Calculating users' average transaction amounts over 24 hours.
Analyzing sequences of transactions to detect ping-pong activities.


Implementation Details

Language: Java(SpringBoot)
Simulation: Utilizing in-memory queues for simulating real-time data processing.
Handling Out-of-Order Events: Employing event timestamp tracking to handle network latencies and out-of-order events efficiently.


Setup and Running Instructions:
Clone the repository and navigate to the project directory.
Compile the Java files.
Run the main program with the provided test dataset.
View the generated alerts and flagged users.

Assumptions
Transactions arrive in chronological order but may experience network delays.
Each user has a unique user ID across all services.

Expected Results
Flagged Users:
user1: Conducting transactions in more than 3 distinct services within a 5-minute window.
user2 and user3: Transactions significantly higher than typical amounts.a
The Fraud Detection Controller: is responsible for handling HTTP requests related to fraud detection operations. It utilizes the Fraud Detector Engine Service to perform fraud checks on transaction events.

Explanation
The controller class is annotated with @RestController and @RequestMapping to specify the base URL for all endpoints.
Dependency injection is used to inject the FraudDetectorEngineService into the controller.

Two endpoints are defined:
/fraud/all: Endpoint to check all fraud transactions.
/fraud/download: Endpoint to export transaction events to CSV.
The generateTransactionEvents() method generates sample transaction events for testing purposes.
In the /fraud/download endpoint, transaction events are exported to a CSV file using the ListToCsv.exportCSV() method.

Note
Update the generateTransactionEvents() method with actual logic to generate transaction events.
Ensure the CSV file path (CSV_FILE_PATH) is correct and accessible.

---

Fraud & Financial Crime Demo
A demo package for first-pass alert triage plus a Pega -> Camunda migration slice lives
under demo/ (see demo/README.md). It adds an auditable triage engine (GET /fraud/triage)
to this application and a migrated Camunda 7 + React human-approval-gate workflow.

