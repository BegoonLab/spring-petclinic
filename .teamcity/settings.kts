import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.projectFeatures.awsConnection
import jetbrains.buildServer.configs.kotlin.projectFeatures.githubIssues
import jetbrains.buildServer.configs.kotlin.triggers.vcs

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

    buildType(Build)

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
    }
}

object Build : BuildType({
    name = "Build"

    params {
        param("env.JDK_17_0", "")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        maven {
            id = "Maven2"
            goals = "clean test"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            jdkHome = "%env.JDK_17_0_x64%"
        }
    }

    triggers {
        vcs {
            branchFilter = "+:refs/pull/*/merge"
        }
    }

    features {
        perfmon {
        }
    }
})
