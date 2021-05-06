plugins {
    `java-library`
    jacoco
    id("org.hypertrace.jacoco-report-plugin") version "0.2.0"
}

dependencies {
    compileOnly("org.apache.pinot:pinot-controller:0.7.1")
    compileOnly("org.apache.pinot:pinot-minion:0.7.1")
//    compileOnly("org.apache.pinot:pinot-minion-tasks:0.7.1")
//    compileOnly("org.apache.pinot:pinot-minion-builtin-tasks:0.7.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
    testImplementation("org.mockito:mockito-core:2.10.0")
    testImplementation("org.apache.pinot:pinot-core:0.7.1:tests")
    testImplementation("org.apache.pinot:pinot-controller:0.7.1:tests")
    testImplementation("org.apache.pinot:pinot-integration-tests:0.7.1:tests")
    testImplementation("org.apache.pinot:pinot-minion:0.7.1")
//    testImplementation("org.apache.pinot:pinot-minion-tasks:0.7.1")
//    testImplementation("org.apache.pinot:pinot-minion-builtin-tasks:0.7.1")
}

tasks.test {
    useJUnitPlatform()
}
