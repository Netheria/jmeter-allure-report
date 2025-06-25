import com.allure.allure_report_context.AllureContext;
import com.allure.allure_report_context.AllureIO;

import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.samplers.SampleResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

AllureContext actx = vars.getObject("allureContext");
if (actx == null) { throw new IllegalStateException("AllureContext not initialized; did you forget a START step?"); }
String resultsPath = vars.get("ALLURE_RESULTS_PATH");

// Start a new step object
AllureContext.AllureStep step = actx.startStep(sampler.getName(), prev.getStartTime());

// Decide pass/fail and capture a failure message (if any)
boolean passed = prev.isSuccessful();
String failReason = "";
if (!passed) {
    if (prev.getAssertionResults().size() > 0) {
        failReason = prev.getAssertionResults()[0].getFailureMessage() ?: "";
    } else {
        failReason = "Unknown failure";
    }
}

// Add step's description(-s) in parameters
String commentText = sampler.getComment() ?: "";
String[] comments = commentText.split(";");

comments.each { rawParam ->
	String[] parts = rawParam.split(/\s*:\s*/, 2); // by design: only split into two pieces
	String paramName;
	String paramValue;

    if (parts.length == 1) { // all sigle-part comments will be treated as Actions
        paramName  = "Action";
        paramValue = parts[0].trim();
    } else {
        paramName  = parts[0].trim();
        paramValue = parts[1].trim();
    }

	if (paramName) { step.getParameters().add(new AllureContext.Parameter(paramName, paramValue)); }
}

// Attach request + response to this step
String reqType  = prev.getRequestHeaders()?.contains("/json") ? "application/json" : "text/plain";
String reqData  = prev.getSamplerData();
String respType = prev.getContentType()?.replaceAll(";.*","");
String respData = prev.getResponseDataAsString();
String attReqSource = actx.nextAttachmentSource();

step.getAttachments().add(new AllureContext.Attachment(
    "Request",  attReqSource + "-request-attachment", reqType));
step.getAttachments().add(new AllureContext.Attachment(
    "Response", attReqSource + "-response-attachment", respType));

// Write the actual files out
AllureIO.writeAttachment(
	resultsPath,
	attReqSource + "-request-attachment",
	reqData
);

AllureIO.writeAttachment(
	resultsPath,
	attReqSource + "-response-attachment",
	respData
);

// End the step, logging pass/fail
actx.endStep(step, prev.getEndTime(), passed, failReason);

// If this sampler has "stop" in its Parameters, finish the case and write JSON
if (Parameters.contains("stop")) {
    actx.finishCase(prev.getEndTime());
    String outJson = actx.toJson();

	AllureIO.writeAttachment(
		resultsPath,
		attReqSource + "-result.json",
		outJson
	);
}
