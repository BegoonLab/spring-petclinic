import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.SSHUpload
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.sshExec
import jetbrains.buildServer.configs.kotlin.buildSteps.sshUpload
import jetbrains.buildServer.configs.kotlin.projectFeatures.awsConnection
import jetbrains.buildServer.configs.kotlin.projectFeatures.githubIssues
import jetbrains.buildServer.configs.kotlin.projectFeatures.slackConnection
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.11"

project {

    vcsRoot(HttpsGithubComBegoonLabSpringPetclinicRefsHeadsDev1)

    buildType(Build)
    buildType(DeployToAws)

    features {
        awsConnection {
            id = "AmazonWebServicesAws"
            name = "Amazon Web Services (AWS)"
            regionName = "eu-north-1"
            credentialsType = static {
                accessKeyId = "AKIA4MTWJQK6ANGXVSMN"
                secretAccessKey = "credentialsJSON:e266eaec-f32c-4bb5-820c-8147ad9567f3"
                stsEndpoint = "https://sts.eu-north-1.amazonaws.com"
            }
            allowInBuilds = false
        }
        githubIssues {
            id = "PROJECT_EXT_2"
            displayName = "BegoonLab/spring-petclinic"
            repositoryURL = "https://github.com/BegoonLab/spring-petclinic"
            authType = accessToken {
                accessToken = "credentialsJSON:c5d3dc2b-7ef8-436e-bc26-987765081818"
            }
            param("tokenId", "")
        }
        slackConnection {
            id = "PROJECT_EXT_4"
            displayName = "Slack"
            botToken = "credentialsJSON:c1365624-2587-4092-818a-dc2901de6974"
            clientId = "6518260057585.6491182076119"
            clientSecret = "credentialsJSON:e6ac3b80-38c9-4c14-8059-55c6a37278df"
        }
    }
}

object Build : BuildType({
    name = "Build"

    artifactRules = "+:target/*.jar"

    vcs {
        root(HttpsGithubComBegoonLabSpringPetclinicRefsHeadsDev1)
    }

    steps {
        maven {
            id = "Maven2"
            goals = "clean test package"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            jdkHome = "%env.JDK_21_0%"
        }
    }

    features {
        perfmon {
        }
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:3861c078-daa6-418f-8861-9d307fcc0159"
                }
            }
        }
        notifications {
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_4"
                sendTo = "#teamcity"
                messageFormat = simpleMessageFormat()
            }
            buildStarted = true
            buildFailedToStart = true
            buildFailed = true
            buildFinishedSuccessfully = true
            firstBuildErrorOccurs = true
            buildProbablyHanging = true
            queuedBuildRequiresApproval = true
        }
    }
})

object DeployToAws : BuildType({
    name = "Deploy to AWS"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        sshUpload {
            id = "ssh_deploy_runner"
            transportProtocol = SSHUpload.TransportProtocol.SCP
            sourcePath = "*.jar"
            targetUrl = "ec2-34-253-194-80.eu-west-1.compute.amazonaws.com:/home/ubuntu/pet-clinic"
            authMethod = uploadedKey {
                username = "ubuntu"
                key = "teamcity.pem"
            }
        }
        sshExec {
            name = "Deploy"
            id = "Deploy"
            commands = "sh /home/ubuntu/deploy.sh"
            targetUrl = "ec2-34-253-194-80.eu-west-1.compute.amazonaws.com"
            authMethod = uploadedKey {
                username = "ubuntu"
                key = "teamcity.pem"
            }
        }
    }

    triggers {
        vcs {
            branchFilter = """
                +:<default>
                +:prod
            """.trimIndent()
        }
    }

    features {
        notifications {
            notifierSettings = slackNotifier {
                connection = "PROJECT_EXT_4"
                sendTo = "#teamcity"
                messageFormat = simpleMessageFormat()
            }
            buildStarted = true
            buildFailedToStart = true
            buildFailed = true
            firstFailureAfterSuccess = true
            newBuildProblemOccurred = true
            buildFinishedSuccessfully = true
            firstSuccessAfterFailure = true
            firstBuildErrorOccurs = true
            buildProbablyHanging = true
            queuedBuildRequiresApproval = true
        }
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:3861c078-daa6-418f-8861-9d307fcc0159"
                }
            }
        }
    }

    dependencies {
        dependency(Build) {
            snapshot {
            }

            artifacts {
                artifactRules = "+:*.jar"
            }
        }
    }
})

object HttpsGithubComBegoonLabSpringPetclinicRefsHeadsDev1 : GitVcsRoot({
    name = "https://github.com/BegoonLab/spring-petclinic#refs/heads/dev (1)"
    url = "https://github.com/BegoonLab/spring-petclinic"
    branch = "refs/heads/dev"
    branchSpec = "refs/heads/*"
    authMethod = password {
        userName = "alexbegoon"
        password = "credentialsJSON:3861c078-daa6-418f-8861-9d307fcc0159"
    }
})
