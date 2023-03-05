package de.imiji.jenkins.release

import de.imiji.jenkins.constants.Stage

class ApplicationServerCommand {
    private Object pipeline
    private Stage stage

    ApplicationServerCommand(Object pipeline, Stage stage) {
        this.pipeline = pipeline
        this.stage = stage
    }

    private String sshCall() {
        return "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null "
    }

    private String buildSSHCallPrefix() {
        return "${stage.getNodeUser()}@${stage.getNodeHost()} "
    }

    void stopServer() {
        this.pipeline.sh(sshCall() + buildSSHCallPrefix() + "stop_server_" + stage.getEnv())
    }

    void deployModule(String moduleName, String version) {
        this.pipeline.sh(sshCall() + buildSSHCallPrefix() + "transfer_module_" + stage.getEnv() + " ${moduleName}@${version}")
    }

    void startServer() {
        this.pipeline.sh(sshCall() + buildSSHCallPrefix() + "start_server_" + stage.getEnv())
    }
}
