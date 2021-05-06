plugins {
    `java-library`
    jacoco
    id("org.hypertrace.jacoco-report-plugin") version "0.2.0"
}

dependencies {
    compileOnly("org.apache.pinot:pinot-core:0.6.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}
