package de.imiji.jenkins

import de.imiji.jenkins.release.ReleaseRegistry
import de.imiji.jenkins.release.ReleaseLevel

class ReleaseProduct {

    private Object pipeline
    private ReleaseRegistry registry

    ReleaseProduct(Object pipeline) {
        this.pipeline = pipeline
        this.registry = new ReleaseRegistry(pipeline)
    }

    void build() {
        this.registry.build()
    }

    void releaseNPMJs(String version, String level) {
        this.registry.release("npx semver ${version} -c -i ${level}")
    }

    void tag(String branch, String moduleName) {
        this.pipeline.nvm("v" + ReleaseRegistry.NODE_VERSION) {
            this.pipeline.sshagent(credentials: ["github"]) {
                this.pipeline.sh("git config http.sslVerify false")
                this.pipeline.sh("git config user.name jenkins")
                this.pipeline.sh("git push --tags")

                def version = this.pipeline.sh(script: "npm view ${moduleName}@latest version", returnStdout: true).trim()
                this.pipeline.sh("npm version ${version}-SNAPSHOT")
                this.pipeline.sh("git push --set-upstream origin ${branch}")
            }
        }
    }
}
