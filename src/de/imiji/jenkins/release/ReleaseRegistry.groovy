package de.imiji.jenkins.release

class ReleaseRegistry {

    public static REGISTER_URL = "registry.npmjs.org"
    public static NPM_CRED_ID = "npm"
    public static NODE_VERSION = "18.0.0"

    private Object pipeline

    ReleaseRegistry(Object pipeline) {
        this.pipeline = pipeline
    }

    void cleanTags() {
        this.pipeline.sh("git tag | xargs git tag -d")
    }

    void build() {
        this.pipeline.echo("build")
        this.pipeline.nvm("v" + NODE_VERSION) {
            this.pipeline.sh("rm -rf node_modules")
            this.pipeline.sh("npm install")
            this.pipeline.sh("npm run build")
            this.cleanTags()
        }
    }

    void checkReleaseSnapshots(Map dependencies = [:]) {
        def versions = dependencies.values()
        for (String version in versions) {
            if (version.contains("SNAPSHOT")) {
                this.pipeline.currentBuild.result = "ABORTED"
                this.pipeline.error("contains snapshot versions")
            }
        }
    }

    void release(String nextVersionCMD) {
        this.pipeline.echo("upload to NPM")
        this.cleanTags()
        this.pipeline.withCredentials([this.pipeline.string(credentialsId: NPM_CRED_ID, variable: 'NPM_TOKEN')]) {
            this.pipeline.nvm("v" + NODE_VERSION) {
                this.pipeline.sh("echo //${REGISTER_URL}/:_authToken=${this.pipeline.NPM_TOKEN} > .npmrc")
                def nextVersion = this.pipeline.sh(script: nextVersionCMD, returnStdout: true).trim()
                this.pipeline.sh("npm version ${nextVersion}")
                this.pipeline.sh("npm publish --access public")
                this.pipeline.sh("rm .npmrc")
            }
        }
    }

    void upload(String moduleName) {
        this.pipeline.echo("upload to NPM")
        this.pipeline.withCredentials([this.pipeline.string(credentialsId: NPM_CRED_ID, variable: 'NPM_TOKEN')]) {
            this.pipeline.nvm("v" + NODE_VERSION) {
                this.pipeline.sh("npm config set prefix ${this.pipeline.WORKSPACE}")
                this.pipeline.sh("echo //${REGISTER_URL}/:_authToken=${this.pipeline.NPM_TOKEN} > .npmrc")
                String version = "1.0.0-SNAPSHOT"
                try {
                    version = this.pipeline.sh(script: "npm view ${moduleName}@latest version", returnStdout: true).trim()
                } catch (all) {
                    this.pipeline.echo("this module ${moduleName} not relesed yet")
                }
                def newVersion = version.contains("-SNAPSHOT") ? version : "${version}-SNAPSHOT"
                def nextVersion = this.pipeline.sh(script: "npx semver ${newVersion} -i prerelease", returnStdout: true)
                this.pipeline.sh("npm version --no-git-tag-version ${nextVersion}")
                this.pipeline.sh("npm publish --access public --force")
                this.pipeline.sh("rm .npmrc")
            }
        }
    }
}
