package de.imiji.jenkins.release

class ReleaseRegistry {

    public static REGISTER_URL = "registry.npmjs.org"
    public static NPM_CRED_ID = "npm"
    public static NODE_VERSION = "18.0.0"

    private Object pipeline

    ReleaseRegistry(Object pipeline) {
        this.pipeline = pipeline
    }

    void build() {
        this.pipeline.echo("build")
        this.pipeline.nvm("v" + NODE_VERSION) {
            this.pipeline.sh("npm install")
            this.pipeline.sh("npm run build")
            this.pipeline.sh("git tag | xargs git tag -d")
        }
    }

    void release(String nextVersionCMD) {
        this.pipeline.echo("upload to NPM")
        this.pipeline.withCredentials([this.pipeline.string(credentialsId: NPM_CRED_ID, variable: 'NPM_TOKEN')]) {
            this.pipeline.nvm("v" + NODE_VERSION) {
                this.pipeline.sh("echo //${REGISTER_URL}/:_authToken=${this.pipeline.NPM_TOKEN} > .npmrc")
                def nextVersion = this.pipeline.sh(nextVersionCMD)
                this.pipeline.sh("npm version ${nextVersion}")
                this.pipeline.sh("npm publish --access public")
                this.pipeline.sh("rm .npmrc")
            }
        }
    }

    void upload(String moduleName, String version) {
        this.pipeline.echo("upload to NPM")
        this.pipeline.withCredentials([this.pipeline.string(credentialsId: NPM_CRED_ID, variable: 'NPM_TOKEN')]) {
            this.pipeline.nvm("v" + NODE_VERSION) {
                this.pipeline.sh("echo //${REGISTER_URL}/:_authToken=${this.pipeline.NPM_TOKEN} > .npmrc")
                def removeModule = "${moduleName}@${version}"
                try {
                    this.pipeline.sh("npm unpublish ${removeModule}")
                } catch (all) {
                    this.pipeline.echo("try to remove ${removeModule}")
                }
                this.pipeline.sleep(60)
                this.pipeline.sh("npm publish --access public")
                this.pipeline.sh("rm .npmrc")
            }
        }
    }
}
