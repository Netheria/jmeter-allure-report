package com.allure.allure_report_context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AllureContext holds everything needed to generate one Allure JSON report.
 *
 * @details Usage (in a JSR223 Sampler):
 *   - On the "start" step:
 *   @code
 *   AllureContext actx = new AllureContext();
 *   vars.putObject("allureContext", actx);
 *   actx.startCase(vars.get("allure.name"), vars.get("allure.description"));
 *   // … set labels/links/parameters on actx …
 *   @endcode
 *
 *   - On each step:
 *   @code
 *   AllureContext actx = vars.getObject("allureContext");
 *   AllureContext.AllureStep step = actx.startStep(sampler.getName());
 *   // … run sampler, attach request/response …
 *   actx.endStep(step, prev.isSuccessful(), failReason);
 *   @endcode
 *
 *   - On the "stop" step:
 *   @code
 *   actx.finishCase();
 *   String json = actx.toJson();  // serialize with Jackson
 *   Path p = Path.of(vars.get("ALLURE_REPORT_PATH"), actx.getUuid() + "-result.json");
 *   Files.createDirectories(p.getParent());
 *   Files.writeString(p, json, StandardCharsets.UTF_8);
 *   @endcode
 *
 * ## Nested Classes:
 * - {@link AllureContext.AllureStep}
 * - {@link AllureContext.StatusDetails}
 * - {@link AllureContext.Attachment}
 * - {@link AllureContext.Label}
 * - {@link AllureContext.Link}
 * - {@link AllureContext.Parameter}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllureContext {

    // Unique Identifiers
    /**
     * Required field.
     * Unique ID used both as "uuid" for case and "historyId" in Allure JSON.
     */
    private final String caseUuid = UUID.randomUUID().toString();
    /**
     * Required field.
     * Unique indentifier for Allure’s history‐tracking, set equal to `caseUuid`.
     */
    private final String historyId = caseUuid;

    // Case-Level Fields
    /**
     * Required field.
     * Test display name (e.g. "MESSAGE_CREATED event - Inbox collaborator").
     * "name" field in JSON on case level.
     */
    @JsonProperty("name")
    private String caseName;
    /**
     * Required field.
     * Long description of the test case. Can be empty string if none.
     * "description" field in JSON on case level.
     */
    @JsonProperty("description")
    private String caseDescription;
    /**
     * Required field.
     * Fully qualified test identifier (e.g. "org.jmeter.com.{epic}.{feature}.{story}.{testName}").
     */
    private String fullName;
    /**
     * Required field.
     * Test status = "passed", "failed", or "skipped"
     */
    private String status;
    /**
     * Required field.
     * Test lifecycle stage, usually "running" then "finished".
     * But as it is not a realtime reporting the value will usually be "finished".
     */
    private String stage;
    /**
     * Required field.
     * Epoch ms when this case started.
     */
    @JsonProperty("start")
    private long startMillis;
    /**
     * Required field.
     * Epoch ms when this case finished.
     */
    @JsonProperty("stop")
    private long stopMillis;
    /**
     * Optional field.
     * List of labels (e.g. story, feature, tag, host). May be empty.
     */
    private List<Label> labels = new ArrayList<>();
    /**
     * Optional field.
     * List of external links (e.g. issue tracker URLs). May be empty.
     */
    private List<Link> links = new ArrayList<>();
    /**
     * Optional field.
     * List of user‐defined parameters (e.g. username/password). May be empty.
     */
    private List<Parameter> parameters = new ArrayList<>();

    // Step-Level List
    /**
     * Required field.
     * Top-level "steps" array. Each entry captures one sampler/step.
     */
    private List<AllureStep> steps = new ArrayList<>();

    // Case counters (to print run progress info)
    /**
     * Total count of test cases.
     */
    private int summaryCountTests = 0;
    /**
     * How many test cases passed.
     */
    private int passedCountTests = 0;
    /**
     * How many test cases failed.
     */
    private int failedCountTests = 0;
    /**
     * How many test cases skipped.
     */
    private int skippedCountTests = 0;

    /**
     * Return the generated UUID (also used to name attachments).
     */
    public String getCaseUuid() { return caseUuid; }

    /** 
     * Generate a fresh UUID just for one attachment.
     * Concatenated with caseUuid to keep case and attachment grouped e.g. "caseUuid-<random>".
     */
    public String nextAttachmentSource() { return caseUuid + "-" + UUID.randomUUID().toString(); }

    /**
     * Return the generated historyId (identical to case uuid).
     */
    public String getHistoryId() { return historyId; }

    /**
     * Return total count of test cases.
     */
    public int getSummaryCount() { return summaryCountTests; }

    /**
     * Return total count of passed test cases.
     */
    public int getPassedCount() { return passedCountTests; }

    /**
     * Return total count of failed test cases.
     */
    public int getFailedCount() { return failedCountTests; }

    /**
     * Return total count of failed test cases.
     */
    public int getSkippedCount() { return skippedCountTests; }

    /**
     * Increment total count of test cases.
     */
    public void incrementSummaryCount() { summaryCountTests++; }

    /**
     * Increment total count of passed test cases.
     */
    public void incrementPassedCount() { passedCountTests++; }

    /**
     * Increment total count failed of test cases.
     */
    public void incrementFailedCount() { failedCountTests++; }

    /**
     * Increment total count skipped of test cases.
     */
    public void incrementSkippedCount() { skippedCountTests++; }

    /**
     * Convert AllureContext context to a JSON string via Jackson.
     * @throws IOException if Jackson serialization fails.
     */
    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper.writeValueAsString(this);
    }

    /**
     * Call once at the very start of test case.
     * - Sets the test case name.
     * - Sets test case description.
     * - Sets the start timestamp.
     * - Puts stage = "running".
     * @param name Test case name.
     * @param description Test case description.
     * @param startMillis Test case start timing in miliseconds.
     */
    public void startCase(String name, String description, long startMillis) {
        this.caseName = name;
        this.caseDescription = description;
        this.startMillis = startMillis;
        this.stage = "running";
    }

    /**
     * Creates a new step (sampler) in the "steps" list, sets it's name and start time,
     * and returns it so that the caller can later call endStep(...) on it.
     * @param stepName Test case step name.
     * @param startMillis Test case step start timing in miliseconds.
     * @return Allure step object.
     * @see #endStep(AllureStep, long, boolean, String).
     */
    public AllureStep startStep(String stepName, long startMillis) {
        AllureStep s = new AllureStep();
        s.setName(stepName);
        s.setStart(startMillis);
        s.setStage("running");
        steps.add(s);
        return s;
    }

    /**
     * Call after each sampler/step finishes:
     * - Sets it's stop timestamp
     * - Sets status ("passed" or "failed") and failure message (if any).
     * - Sets the step stage = "finished".
     * - Propagates a failure into the case‐level status if needed.
     * @param step Allure step object.
     * @param stopMillis Test case step stop timing in miliseconds.
     * @param passed Inficator if step passed or failed.
     * @param failReason Failure message.
     */
    public void endStep(AllureStep step, long stopMillis, boolean passed, String failReason) {
        step.setStop(stopMillis);
        step.setStatus(passed ? "passed" : "failed");
        step.getStatusDetails().setMessage(passed ? "" : failReason);
        step.setStage("finished");

        // If any step fails, mark the whole case as "failed" (if not already failed).
        if (!passed && (this.status == null || this.status.equals("passed"))) {
            this.status = "failed";
        }
    }

    /**
     * Call once after the very last "stop" step:
     * - Sets the case stop timestamp.
     * - Sets status as "passed" if status was never set previously (no failures).
     * - Sets the case stage = "finished".
     * @param stopMillis Test case stop timing in miliseconds.
     */
    public void finishCase(long stopMillis) {
        this.stopMillis = stopMillis;
        if (this.status == null) { this.status = "passed"; }
        this.stage = "finished";
    }

    /** Get the case's name. */
    public String getCaseName()            { return caseName; }
    /** Get the case's description. */
    public String getCaseDescription()     { return caseDescription; }
    /** Get the test indentifier. */
    public String getFullName()            { return fullName;}
    /** Get the case's status. */
    public String getStatus()              { return status; }
    /** Get the case's stage. */
    public String getStage()               { return stage; }
    /** Get the case's start timing in milliseconds. */
    public long getStartMillis()           { return startMillis; }
    /** Get the case's stop timing in milliseconds. */
    public long getStopMillis()            { return stopMillis; }
    /** Get the case's List of label objects. */
    public List<Label> getLabels()         { return labels; }
    /** Get the case's List of link objects. */
    public List<Link> getLinks()           { return links; }
    /** Get the case's List of parameter objects. */
    public List<Parameter> getParameters() { return parameters; }
    /** Get the case's List of Step objects. */
    public List<AllureStep> getSteps()     { return steps; }
    /** Get the current count of test cases. */
    public int getSummaryCountTests()      { return summaryCountTests; }
    /** Get the current count of passed test cases. */
    public int getPassedCountTests()       { return passedCountTests; }
    /** Get the current count of failed test cases. */
    public int getFailedCountTests()       { return failedCountTests; }
    /** Get the current count of skipped test cases. */
    public int getSkippedCountTests()      { return skippedCountTests; }

    /** Set the case's name. */
    public void setCaseName(String caseName)                { this.caseName = caseName; }
    /** Set the case's description. */
    public void setCaseDescription(String caseDescription)  { this.caseDescription = caseDescription; }
    /** Set the test indentifier. */
    public void setFullName(String fullName)                { this.fullName = fullName; }
    /** Set the case's status. */
    public void setStatus(String status)                    { this.status = status; }
    /** Set the case's stage. */
    public void setStage(String stage)                      { this.stage = stage; }
    /** Set the case's start timing in milliseconds. */
    public void setStartMillis(long startMillis)            { this.startMillis = startMillis; }
    /** Set he case's stop timing in milliseconds. */
    public void setStopMillis(long stopMillis)              { this.stopMillis = stopMillis; }
    /** Set the case's List of label objects. */
    public void setLabels(List<Label> labels)               { this.labels = labels; }
    /** Set the case's List of link objects. */
    public void setLinks(List<Link> links)                  { this.links = links; }
    /** Set the case's List of parameter objects. */
    public void setParameters(List<Parameter> parameters)   { this.parameters = parameters; }
    /** Setthe case's List of Step objects. */
    public void setSteps(List<AllureStep> steps)            { this.steps = steps; }
    /** Set the count of test cases. */
    public void setSummaryCountTests(int summaryCountTests) { this.summaryCountTests = summaryCountTests; }
    /** Set the count of passed test cases. */
    public void setPassedCountTests(int passedCountTests)   { this.passedCountTests = passedCountTests; }
    /** Set the count of failed test cases. */
    public void setFailedCountTests(int failedCountTests)   { this.failedCountTests = failedCountTests; }
    /** Set the count of skipped test cases. */
    public void setSkippedCountTests(int skippedCountTests) { this.skippedCountTests = skippedCountTests; }

    /**
     * Step structure for case in Allure.
     *
     * Required fields:
     * - name: the step display name (e.g. "HTTP REQ: login into the system").
     * - status: the step status ("passed"/"failed").
     * - stage: the step stage ("running" then "finished").
     * - start: the epoch ms when step started.
     * - stop: the epoch ms when step stopped.
     *
     * Optional fields:
     * - statusDetails: the step's failure message on failure.
     * - attachments: the step's List of Attachment objects (e.g. Requests, Responses).
     * - parameters: the step's List of Parameter objects (e.g. "username": "admin").
     * - steps: the step's nested steps objects (e.g. assertions).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AllureStep {

        /** Required field. Step display name (e.g. "HTTP REQ: login into the system"). */
        private String name;
        /** Required field. Step status ("passed"/"failed"). */
        private String status;
        /** Required field. Step stage ("running" then "finished"). */
        private String stage;
        /** Required field. Epoch ms when step started. */
        private long start;
        /** Required field. Epoch ms when step stopped. */
        private long stop;
        /** Optional field. Detailed failure message if this step failed. */
        private StatusDetails statusDetails = new StatusDetails();
        /** Optional field. Attachments (Request, Response) for this step. */
        private List<Attachment> attachments = new ArrayList<>();
        /** Optional field.  Parameters local to this step. */
        private List<Parameter> parameters = new ArrayList<>();
        /** Optional field. Nested sub‐steps (e.g. assertion steps). */
        private List<AllureStep> steps = new ArrayList<>();

        /** Get the step's name. */
        public String getName()                  { return name; }
        /** Get the step's status. */
        public String getStatus()                { return status; }
        /** Get the step's stage. */
        public String getStage()                 { return stage; }
        /** Get the step's start timing in milliseconds. */
        public long getStart()                   { return start; }
        /** Get the step's stop timing in milliseconds. */
        public long getStop()                    { return stop; }
        /** Get the step's status details message. */
        public StatusDetails getStatusDetails()  { return statusDetails; }
        /** Get the step's attachments. */
        public List<Attachment> getAttachments() { return attachments; }
        /** Get the step's parameters. */
        public List<Parameter> getParameters()   { return parameters; }
        /** Get the step's nested steps (e.g. assertions). */
        public List<AllureStep> getSteps()       { return steps; }

        /** Set the step's name. */
        public void setName(String name)                          { this.name = name; }
        /** Set the step's status. */
        public void setStatus(String status)                      { this.status = status; }
        /** Set the step's stage. */
        public void setStage(String stage)                        { this.stage = stage; }
        /** Set the step's start timing in milliseconds. */
        public void setStart(long start)                          { this.start = start; }
        /** Set the step's stop timing in milliseconds. */
        public void setStop(long stop)                            { this.stop = stop; }
        /** Set the step's status details message. */
        public void setStatusDetails(StatusDetails statusDetails) { this.statusDetails = statusDetails; }
        /** Set the step's attachments. */
        public void setAttachments(List<Attachment> attachments)  { this.attachments = attachments; }
        /** Set the step's parameters. */
        public void setParameters(List<Parameter> parameters)     { this.parameters = parameters; }
        /** Set the step's nested steps (e.g. assertions). */
        public void setSteps(List<AllureStep> steps)              { this.steps = steps; }

    }

    /**
     * StatusDetails structure for Allure.
     *
     * Optional field:
     * - message: a status message.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusDetails {

        /** Optional field. Message, e.g. "AssertionError: response code is 200" */
        private String message = "";
        /** Get the status message. */
        public String getMessage() { return message; }
        /** Set the status message. */
        public void setMessage(String message) { this.message = message; }

    }

    /**
     * Attachment structure for Allure.
     *
     * Required fields:
     * - name: "Request" or "Response"
     * - source: file with data
     * - type: MIME type (e.g. "application/json", "text/plain")
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Attachment {

        /** Required field. "Request" or "Response". */
        private String name;
        /** Required field. File source (e.g. "<case_uuid>-<uuid>-request-attachment"). */
        private String source;
        /** Required field. MIME type (e.g. "application/json", "text/plain"). */
        private String type;

        /** Create a new Attachment. */
        public Attachment(String name, String source, String type) {
            this.name = name;
            this.source = source;
            this.type = type;
        }

        /** Get the attachment's name. */
        public String getName()   { return name; }
        /** Get the attachment's source file. */
        public String getSource() { return source; }
        /** Get the attachment's MIME type. */
        public String getType()   { return type; }

        /** Set the attachment's name. */
        public void setName(String name)     { this.name = name; }
        /** Set the attachment's source file. */
        public void setSource(String source) { this.source = source; }
        /** Set the attachment's MIME type. */
        public void setType(String type)     { this.type = type; }

    }

    /**
     * Label structure for Allure (e.g. story=Positive, feature=Log In).
     *
     * Required fields:
     * - name: the label key (e.g. "story", "feature", "tag", "host", "issue")
     * - value: the label value (e.g. "Positive", "Log In")
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Label {

        /** Required field. e.g. "story", "feature", "tag", "host", "issue" */
        private String name;
        /** Required field. e.g. "Positive", "Permissions", "regress", "HTTP Worker", "XXX-001" */
        private String value;

        /** Create a new Label. */
        public Label(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /** Get the label’s key. */
        public String getName()  { return name; }
        /** Get the label’s value. */
        public String getValue() { return value; }

        /** Set the label’s key. */
        public void setName(String name)   { this.name = name; }
        /** Set the label’s value. */
        public void setValue(String value) { this.value = value; }

    }

    /**
     * Link structure with name and url.
     *
     * Required field:
     * - name: the link key/name (e.g. "issue")
     * - value: the link URL address (e.g. "https://tracker/issue/123")
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Link {

        /** Required field. Link name, e.g. "issue" or "docs". */
        private String name;
        /** Required field. Link URL, e.g. "https://tracker/issue/123". */
        private String url;

        /** Createe a new link. */
        public Link(String name, String url) {
            this.name = name;
            this.url = url;
        }

        /** Set the link’s key. */
        public String getName() { return name; }
        /** Set the link’s url. */
        public String getUrl()  { return url; }

        /** Get the link’s key. */
        public void setName(String name) { this.name = name; }
        /** Get the link’s url. */
        public void setUrl(String url)   { this.url = url; }

    }

    /**
     * Parameter structure with name and value.
     *
     * Required fields:
     * - name: the parameter key/name (e.g. "username")
     * - value: the parameter value (e.g. "admin")
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameter {

        /** Required field.  Parameter name (e.g. "username"). */
        private String name;
        /** Required field. Parameter value (e.g. "admin"). */
        private String value;

        /** Create a new parameter. */
        public Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /** Get the parameter’s key. */
        public String getName()  { return name; }
        /** Get the parameter’s value. */
        public String getValue() { return value; }

        /** Set the parameter’s key. */
        public void setName(String name)   { this.name = name; }
        /** Set the parameter’s value. */
        public void setValue(String value) { this.value = value; }

    }

}
