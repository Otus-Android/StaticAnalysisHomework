plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

lint {
    htmlReport = true
    htmlOutput = file("lint-report.html")
    textReport = true
    absolutePaths = false
    ignoreTestSources = true
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.kotlin.stdlib)
    testImplementation(libs.lint.tests)
    testImplementation(libs.lint.cli)
    testImplementation(libs.junit)
}
