import com.allure.allure_report_context.AllureContext;
import org.apache.jmeter.threads.JMeterVariables;

// Create and store the shared context
AllureContext actx = new AllureContext();
vars.putObject("allureContext", actx);

// Initialize case‐level fields
actx.startCase(vars.get("allure.name"), vars.get("allure.description"), System.currentTimeMillis());

// Build unique test full name
String epic    = vars.get("allure.label.epic")    ?: "unnamed_epic";
String feature = vars.get("allure.label.feature") ?: "unnamed_feature";
String story   = vars.get("allure.label.story")   ?: "unnamed_story";
String test    = vars.get("allure.name")          ?: "unnamed_test";
String noSpaceEpic    = epic.toLowerCase().replace(" ","_");
String noSpaceFeature = feature.toLowerCase().replace(" ","_");
String noSpaceStory   = story.toLowerCase().replace(" ","_");
String noSpaceTest    = test.toLowerCase().replace(" ","_");
String fullName = "org.jmeter.com." + noSpaceEpic + "." + noSpaceFeature + "." + noSpaceStory + "." + noSpaceTest;
actx.setFullName(fullName);

// Parse labels from vars into actx.labels
vars.entrySet().each { entry ->
    String key = entry.key;

    if (!key.startsWith("allure.label.")) { return; }

    String lblName = key.replaceFirst("allure\\.label\\.", "").toLowerCase();

	// Special case: tags can be multiple
	if (lblName.equals("tag")) {
		entry.value.split(/\s*,\s*/).each { tval ->
			tval = tval.trim();
			if (!tval.isEmpty()) { actx.getLabels().add(new AllureContext.Label(lblName, tval)); }
		}
		return;
	}

	actx.getLabels().add(new AllureContext.Label(lblName, entry.value));
}

// Parse links (flat: name,url,name,url,...)
String rawLinks = vars.get("allure.links") ?: "";
if (!rawLinks.trim().isEmpty()) {
    String[] parts = rawLinks.split(",");
    for (int i = 0; i + 1 < parts.length; i += 2) {
        String nm = parts[i].trim();
        String u  = parts[i + 1].trim();
        actx.getLinks().add(new AllureContext.Link(nm, u));
    }
}

// Parse case parameters (“allure.parameters” contains key1:value1;key2: value2;…)
String rawParams = vars.get("allure.parameters") ?: "";
if (!rawParams.trim().isEmpty()) {
    rawParams.split(";").each { pair ->
        String[] parts = pair.split(/\s*:\s*/, 2);
        if (parts.size() == 2) {
            String name  = parts[0].trim();
            String value = parts[1].trim();
            actx.getParameters().add(new AllureContext.Parameter(name, value));
        }
    }
}

SampleResult.setIgnore();