package de.imiji.jenkins

import de.imiji.jenkins.constants.Stage
import de.imiji.jenkins.release.ApplicationServerCommand
import de.imiji.jenkins.util.SmockTest

class CIBuild {

    private Object pipeline;
    private SmockTest smock;
    public static NODE_VERSION = "18.0.0"
    public static NPM_CRED_ID = "npm"
    public static SSH_CRED = ""

    CIBuild(Object pipeline) {
        this.pipeline = pipeline
        this.smock = new SmockTest(pipeline)
    }

    void buildModule() {
        this.pipeline.echo("build")
        this.pipeline.nvm("v" + NODE_VERSION) {
            this.pipeline.sh("npm install")
            this.pipeline.sh("npm run build")
            this.pipeline.sh("git tag | xargs git tag -d")
        }
    }

    void uploadNPMJs(String moduleName, String version) {
        this.pipeline.echo("upload to NPM")
        this.pipeline.withCredentials([this.pipeline.string(credentialsId: NPM_CRED_ID, variable: 'NPM_TOKEN')]) {
            this.pipeline.nvm("v" + NODE_VERSION) {
                this.pipeline.sh("echo //registry.npmjs.org/:_authToken=${this.pipeline.NPM_TOKEN} > .npmrc")
                try {
                    this.pipeline.sh("npm unpublish ${moduleName}@${version} --force")
                } catch (all) {
                    this.pipeline.echo("can't unpublish ${moduleName} with version ${version}")
                }
                this.pipeline.sh("npm publish --access public")
                this.pipeline.sh("rm .npmrc")
            }
        }
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
