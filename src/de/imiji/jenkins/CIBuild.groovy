package de.imiji.jenkins

import de.imiji.jenkins.release.ReleaseRegistry
import de.imiji.jenkins.constants.Stage
import de.imiji.jenkins.release.ReleaseLevel
import de.imiji.jenkins.util.SmockTest

class CIBuild {

    public static NODE_VERSION = "18.0.0"
    public static NPM_CRED_ID = "npm"
    public static SSH_CRED = ""

    private Object pipeline
    private SmockTest smock
    private ReleaseRegistry registry

    CIBuild(Object pipeline) {
        this.pipeline = pipeline
        this.smock = new SmockTest(pipeline)
        this.registry = new ReleaseRegistry(pipeline)
    }

    void buildModule() {
        this.registry.build()
    }

    void uploadNPMJs(String moduleName, String version) {
        this.registry.upload(moduleName, version)
    }

    void deployOnStage(Stage stage, String moduleName, String version) {
        this.pipeline.echo("deploy ${moduleName} with version ${version}")
//        this.pipeline.sshagent(credentials: [SSH_CRED]) {
//            ApplicationServerCommand applicationServerCommand = new ApplicationServerCommand(this.pipeline, stage)
//            applicationServerCommand.stopServer()
//            applicationServerCommand.deployModule(moduleName, version)
//            applicationServerCommand.startServer()
//        }
    }

    void smockTest(Stage stage, String moduleName, String version) {
        this.pipeline.echo("smoke tests of ${moduleName} of ${version}")
//        this.smock.runCI("", "", "", moduleName, version)
    }

    void emailBuildStatus(String result, String name, String url) {
        this.pipeline.emailext(
                subject: "Imiji jenkins Build ${result}: ${name}",
                body: "Siehe ${url}",
                recipientProviders: [
                        [$class: 'DevelopersRecipientProvider'],
                        [$class: 'RequesterRecipientProvider']
                ]
        )
    }
}
