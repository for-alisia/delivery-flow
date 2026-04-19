### 3 MEDIUM — `IssuesController` Duplicates Logging That Belongs to the Service Layer

**What is happening now:**  
[IssuesController.java](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/IssuesController.java) logs both request-received and response-returned for each endpoint. Meanwhile, `IssuesService` already logs operation start and completion with richer business context.

**Why it is a problem:**  
Constitution Principle 1 states entry points must stay thin. The controller's logging is redundant with the service layer's more detailed logging. Each request generates 4 log lines (2 controller + 2 service) for the same operation, which dilutes log signal. The Logging Gate says: *"Orchestration service methods: log operation start with business context, log operation completion with result summary."* The controller should not duplicate this.

**What to prefer:**  
Keep minimal request-entry logging in the controller (one line with the HTTP-specific context that the service layer does not have, such as `hasBody`). Remove the response-returned log lines from the controller — they add no information beyond what the service already logs.

**Expected benefit:**  
Reduced log noise; clearer separation of responsibility between transport and orchestration logging.

---
