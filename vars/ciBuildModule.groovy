import de.imiji.jenkins.CIBuild
import de.imiji.jenkins.CIPreconditions
import de.imiji.jenkins.constants.Stage

/**
 *
 * @param scmUrl
 * @param deployable
 * @return
 */
def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    CIBuild ciBuild = new CIBuild(this)
    CIPreconditions ciPreconditions = new CIPreconditions(this)

    String agentName = pipelineParams.deployable ? "swarm-dev" : ""

    pipeline {
        agent { label agentName }
        triggers {
            pollSCM('* * * * *')
        }
        environment {
            MODULE_VERSION = sh(script: "grep \"version\" package.json | cut -d '\"' -f4 | tr -d '[[:space:]]'", returnStdout: true)
            MODULE_NAME = sh(script: "grep \"name\" package.json | cut -d '\"' -f4 | tr -d '[[:space:]]'", returnStdout: true)
            DOCKERHUB_CREDENTIALS = credentials("docker-hub")
        }
        options {
            timestamps()
            disableConcurrentBuilds()
            buildDiscarder(logRotator(numToKeepStr: "5", artifactNumToKeepStr: "5"))
        }
        parameters {
            string(name: "BRANCH", defaultValue: "develop")
            string(name: "secretJwtKey", defaultValue: "jwt-priv-v1")
            string(name: "secretJwtPub", defaultValue: "jwt-pub-v1")
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
                    script {
                        sh "printenv"
                        git branch: params.BRANCH, credentialsId: "github", url: pipelineParams.scmUrl

                        echo sh(script: 'env|sort', returnStdout: true)
                        env.GIT_COMMITTER_EMAIL = sh(script: "git --no-pager show -s --format=" + "%ae", returnStdout: true).trim()
                        echo "env.GIT_COMMITTER_EMAIL: ${env.GIT_COMMITTER_EMAIL}"
                    }
                }
            }
            stage("Build") {
                steps {
                    script {
                        ciBuild.buildModule()
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
                        params.BRANCH == "develop"
                    }
                }
                steps {
                    echo "Upload report to sonarqube"
                }
            }
            stage("Publish NPM") {
                steps {
                    script {
                        ciBuild.uploadNPMJs(env.MODULE_NAME)
                    }
                }
            }
            stage("Docker") {
                stages {
                    stage("Docker build") {
                        when {
                            allOf {
                                expression { pipelineParams.deployable }
                            }
                        }
                        steps {
                            script {
                                try {
                                    withCredentials([file(credentialsId: params.secretJwtKey, variable: 'jwtKey')]) {
                                        sh "docker secret create jwt.key ${jwtKey}"
                                    }
                                } catch (all) {
                                    echo "already created"
                                }
                                try {
                                    withCredentials([file(credentialsId: params.secretJwtPub, variable: 'jwtPub')]) {
                                        sh "docker secret create jwt.pub ${jwtPub}"
                                    }
                                } catch (all) {
                                    echo "already created"
                                }
                                ciBuild.buildDocker(env.MODULE_NAME)
                            }
                        }

                    }
                    stage("Docker hub publish") {
                        when {
                            allOf {
                                expression { pipelineParams.deployable }
                            }
                        }
                        steps {
                            script {
                                ciBuild.loginDocker()
                                ciBuild.uploadDocker(env.MODULE_NAME)
                            }
                        }
                    }
                }
            }
            stage("Deploy DEV & run test") {
                stages {
                    stage("Deploy DEV") {
                        when {
                            allOf {
                                expression { pipelineParams.deployable }
                            }
                        }
                        steps {
                            script {
                                ciBuild.deployOnStage(Stage.DEV, env.MODULE_NAME, env.MODULE_VERSION)
                            }
                        }
                    }
                    stage("Smoke tests") {
                        when {
                            allOf {
                                expression { pipelineParams.deployable }
                            }
                        }
                        steps {
                            script {
                                ciBuild.smockTest(Stage.DEV, env.MODULE_NAME, MODULE_VERSION)
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
            always {
                script {
                    sh "docker logout"
                    if (pipelineParams.deployable) {
                        deleteDir()
                    }
                }
            }
        }
    }
}
