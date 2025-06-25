final String CATALOGUE_PATH      = vars.get("CATALOGUE_PATH");
final String REPOSITORY_PATH     = vars.get("REPDATA_PATH");
final String ALLURE_RESULTS_PATH = vars.get("ALLURE_RESULTS_PATH");
final String ALLURE_BAT_PATH     = vars.get("ALLURE_BAT_PATH");
final String GENERATE_COMMAND    = CATALOGUE_PATH + REPOSITORY_PATH + ALLURE_BAT_PATH + " generate " + ALLURE_RESULTS_PATH + " --clean -o " + ALLURE_RESULTS_PATH + "/../allure-report";
final String DELETE_COMMAND      = "cmd /c del /q \"" + ALLURE_RESULTS_PATH + "/\"";

try {
    Process generateProcess = Runtime.getRuntime().exec(GENERATE_COMMAND);
    int exitCode = generateProcess.waitFor();

    BufferedReader reader = new BufferedReader(new InputStreamReader(generateProcess.getInputStream()));
    StringBuilder output = new StringBuilder();
    String line = null;

    while ((line = reader.readLine()) != null) {
		output.append(line).append(System.lineSeparator());
    }

    if (exitCode != 0) {
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(generateProcess.getErrorStream()));
        StringBuilder errorOutput = new StringBuilder();

        while ((line = errorReader.readLine()) != null) {
			errorOutput.append(line).append(System.lineSeparator());
        }

        log.error("Allure generation failed: " + errorOutput.toString());
    } else {
        log.info("Allure report generated successfully: " + output.toString());

        Process deleteProcess = Runtime.getRuntime().exec(DELETE_COMMAND);
        exitCode = deleteProcess.waitFor();

        reader = new BufferedReader(new InputStreamReader(generateProcess.getInputStream()));
        output = new StringBuilder();
        line = null;

        while ((line = reader.readLine()) != null) {
			output.append(line).append(System.lineSeparator());
        }

        if (exitCode != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(generateProcess.getErrorStream()));
			StringBuilder errorOutput = new StringBuilder();

			while ((line = errorReader.readLine()) != null) {
					errorOutput.append(line).append(System.lineSeparator());
			}

			log.error("Allure report data clean up failed: " + errorOutput.toString());
        } else {
			log.info("Allure report data cleaned up successfully: " + output.toString());
        }
    }
} catch (Exception e) {
    log.error("Error while generating Allure report", e);
}
