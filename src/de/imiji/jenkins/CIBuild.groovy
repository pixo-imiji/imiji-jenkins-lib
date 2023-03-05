package de.imiji.jenkins

import de.imiji.jenkins.constants.Stage
import de.imiji.jenkins.release.ApplicationServerCommand
import de.imiji.jenkins.util.SmockTest

class CIBuild {

    private Object pipeline;
    private SmockTest smockTest;
    public static NODE_VERSION = "18.0.0"
    public static NPM_CRED_ID = "NPM_PUBLISHER"
    public static SSH_CRED = ""

    CIBuild(Object pipeline) {
        this.pipeline = pipeline
        this.smockTest = new SmockTest(pipeline)
    }

    void buildModule(String workspace, String branch) {
        this.pipeline.echo("build")
        this.pipeline.nvm("v" + NODE_VERSION) {
            this.pipeline.sh("npm install")
            this.pipeline.sh("npm run build")
            this.pipeline.sh("node deploy/releaseSnapshot.js")
            this.pipeline.sh("git push --tags")
        }
    }

    void uploadNPMJs() {
        this.pipeline.sh("upload to NPM")
//        this.pipeline.withCredentials([string(credentialsId: NPM_CRED_ID, variable: 'NPM_TOKEN')]) {
        this.pipeline.sh("node -v")
//        }
    }

    void deployOnStage(Stage stage, String moduleName, String version) {
        this.pipeline.sh("deploy ${moduleName} with version ${version}")
//        this.pipeline.sshagent(credentials: [SSH_CRED]) {
//            ApplicationServerCommand applicationServerCommand = new ApplicationServerCommand(this.pipeline, stage)
//            applicationServerCommand.stopServer()
//            applicationServerCommand.deployModule(moduleName, version)
//            applicationServerCommand.startServer()
//        }
    }

    void smockTest(Stage stage, String moduleName, String version) {
        this.pipeline.echo("smoke tests of ${moduleName} of ${version}")
//        this.smockTest.runCI("", "", "", moduleName, version)
    }

    void emailBuildStatus(String result, String name, String url) {
        this.pipeline.emailext(
                subject: "Imiji jenkins Build ${this.pipeline.currentBuild.result}: ${name}",
                body: "Siehe ${url}",
                recipientProviders: [
                        [$class: 'DevelopersRecipientProvider'],
                        [$class: 'RequesterRecipientProvider']
                ]
        )
    }
}
