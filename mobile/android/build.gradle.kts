plugins {
    id("org.sonarqube") version "7.0.1.6134"
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

sonar {
    properties {
        property("sonar.projectKey", "Aincrad-Flux_AREA-Mobile_a69c10cd-189c-430e-b4a7-4719b173cf11")
        property("sonar.projectName", "AREA-Mobile")
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
