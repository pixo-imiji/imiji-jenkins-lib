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

    void releaseNPMJs(String version, ReleaseLevel level) {
        this.registry.release("npx semver ${version} -c -i ${level.name()}")
    }

    void tag(String branch) {
        this.pipeline.sshagent(credentials: ["github"]) {
            this.pipeline.sh("git push --tags")
            this.pipeline.sh("git push --set-upstream origin ${branch}")
        }
    }
}
