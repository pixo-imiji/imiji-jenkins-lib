import de.imiji.jenkins.CIBuild
import de.imiji.jenkins.CIPreconditions
import de.imiji.jenkins.constants.Stage

/**
 *
 * @param scmUrl
 * @param moduleName
 * @param deployable
 * @return
 */
def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams

    CIBuild ciBuild = new CIBuild(this);
    CIPreconditions ciPreconditions = new CIPreconditions(this)

    body()

    pipeline {
        agent any
        triggers {
            cron('H 22 * * *')
        }
        environment {
            MODULE_VERSION = sh(script: "grep \"version\" package.json | cut -d '\"' -f4 | tr -d '[[:space:]]'", returnStdout: true)
        }
        parameters {
            string(name: "BRANCH", defaultValue: "develop")
        }
        stages {
            stage("Check Preconditions") {
                steps {
                    script {
                        ciPreconditions.check()
                    }
                }
            }
            stage("Checkout Git") {
                steps {
                    sh "printenv"
                    git branch: params.BRNACH, credentialsId: "github", url: pipelineParams.scmUrl

                    echo sh(script: 'env|sort', returnStdout: true)
                    env.GIT_COMMITTER_EMAIL = sh(script: "git --no-pager show -s --format=" + "%ae", returnStdout: true).trim()
                    echo "env.GIT_COMMITTER_EMAIL: ${env.GIT_COMMITTER_EMAIL}"
                    script {
                        if (!env.GIT_COMMITTER_EMAIL.conatins("@")) {
                            env.GIT_COMMITTER_EMAIL = ''
                        }
                    }
                }
            }
            stage("Build") {
                steps {
                    script {
                        ciBuild.buildModule(env.WORKSPACE, params.BRNACH)
                    }
                }
            }
            stage("OWASP") {
                steps {
                    echo "vulnerabilities check"
                }
            }
            stage("Sonargraph") {
                steps {
                    echo "Static check with sonargraph"
                }
            }
            stage("Sonarqube") {
                when {
                    expression {
                        params.BRNACH === "develop"
                    }
                }
                steps {
                    echo "Upload report to sonarqube"
                }
            }
            stage("Publish NPM") {
                steps {
                    script {
                        ciBuild.uploadNPMJs()
                    }
                }
            }
            stage("Deploy DEV & run test") {
                stages {
                    stage("Deploy DEV") {
                        when {
                            allOf {
                                expression { pipelineParams.deployable }
                                expression { params.BRNACH == "develop" }
                            }
                        }
                        steps {
                            script {
                                ciBuild.deployOnStage(Stage.DEV, pipelineParams.moduleName, MODULE_VERSION)
                            }
                        }
                    }
                    stage("Smoke tests") {
                        when {
                            allOf {
                                expression { pipelineParams.deployable }
                                expression { params.BRNACH == "develop" }
                            }
                        }
                        steps {
                            script {
                                ciBuild.smockTest.runCI(Stage.DEV, pipelineParams.moduleName, MODULE_VERSION)
                            }
                        }
                    }
                }
            }
            stage("Run integration/ui/usecase test") {
                steps {
                    echo "run tests"
                }
            }
        }

        post {
            failure {
                script {
                    ciBuild.emailBuildStatus(currentBuild.result, currentBuild.fullDisplayName, currentBuild.absoluteUrl)
                }
            }
            unstable {
                script {
                    ciBuild.emailBuildStatus(currentBuild.result, currentBuild.fullDisplayName, currentBuild.absoluteUrl)
                }
            }
            fixed {
                script {
                    ciBuild.emailBuildStatus(currentBuild.result, currentBuild.fullDisplayName, currentBuild.absoluteUrl)
                }
            }
        }
    }
}
