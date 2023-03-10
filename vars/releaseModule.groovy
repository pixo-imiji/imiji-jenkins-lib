import groovy.json.JsonSlurper
import de.imiji.jenkins.ReleaseProduct
import de.imiji.jenkins.release.ReleaseRegistry

/**
 *
 * @param scmUrl
 * @param moduleName
 * @return
 */
def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    ReleaseProduct releaseProduct = new ReleaseProduct(this)
    ReleaseRegistry registry = new ReleaseRegistry(this)

    pipeline {
        agent any
        environment {
            MODULE_VERSION = sh(script: "grep \"version\" package.json | cut -d '\"' -f4 | tr -d '[[:space:]]'", returnStdout: true)
            MODULE_NAME = sh(script: "grep \"name\" package.json | cut -d '\"' -f4 | tr -d '[[:space:]]'", returnStdout: true)
        }
        options {
            timestamps()
            buildDiscarder(logRotator(numToKeepStr: "5", artifactNumToKeepStr: "5"))
        }
        parameters {
            string(name: "BRANCH", defaultValue: "develop")
            choice(
                    name: "releaseLevel",
                    choices: "major\n minor\n patch",
                    description: "select next release incremented version of module"
            )
        }
        stages {
            stage("Checkout Git") {
                steps {
                    script {
                        sh "printenv"
                        git branch: params.BRANCH, credentialsId: "github", url: pipelineParams.scmUrl

                        echo sh(script: 'env|sort', returnStdout: true)
                        env.GIT_COMMITTER_EMAIL = sh(script: "git --no-pager show -s --format=" + "%ae", returnStdout: true).trim()
                        echo "env.GIT_COMMITTER_EMAIL: ${env.GIT_COMMITTER_EMAIL}"
                    }
                }
            }
            stage("Check Snapshot") {
                steps {
                    script {
                        def dependencies = readJSON(file: "package.json").dependencies
                        Map map = dependencies.collectEntries { String entry ->
                            def arr = entry.split(":")
                            return [arr[0].trim(), arr[1].trim()]
                        }
                        registry.checkReleaseSnapshots(map)
                    }
                }
            }
            stage("Build") {
                steps {
                    script {
                        releaseProduct.build()
                    }
                }
            }
            stage("Upload npm") {
                steps {
                    script {
                        releaseProduct.releaseNPMJs(env.MODULE_VERSION, params.releaseLevel)
                    }
                }
            }
            stage("Tag Version") {
                steps {
                    script {
                        releaseProduct.tag(params.BRANCH, env.MODULE_NAME)
                    }
                }
            }
        }
    }

}