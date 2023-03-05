package de.imiji.jenkins.util

import de.imiji.jenkins.constants.Stage

class SmockTest {

    private Object pipeline

    public SmockTest(Object pipeline) {
        this.pipeline = pipeline
    }

    String run(Stage stage) {
        String url = "${stage.getHost()}/imiji/status"
        String result = ""
        this.pipeline.retry(count: 6) {
            this.pipeline.sleep(time: 5, unit: "SECONDS")
            result = this.pipeline.sh(script: "curl -k -s -u ${stage.getUser()}:${stage.getPassword()} -X GET ${url} | jq '.state' | grep -e RUNNING -e CREATED", returnStdout: true)
        }
        return result
    }

    String runCI(Stage stage, String moduleName, String version) {
        String url = "${stage.getHost()}/imiji/status"
        this.pipeline.echo("Smoke test for: " + moduleName)
        String deployedVersion = this.pipeline
                .sh(script: "curl -k -s -u ${stage.getUser()}:${stage.getPassword()} -X GET ${url} | jq '.moduleStates[] | select(.moduleName == \"" + moduleName + "\") | .version'", returnStdout: true)
        this.pipeline.echo("deployed version: " + deployedVersion)
        this.pipeline.echo("module version: " + version)
        return deployedVersion
    }
}
