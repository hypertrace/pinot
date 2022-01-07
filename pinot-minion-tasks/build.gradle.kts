plugins {
    `java-library`
    jacoco
    id("org.hypertrace.jacoco-report-plugin") version "0.2.0"
}

dependencies {
    compileOnly("org.apache.pinot:pinot-core:0.7.1")
    implementation ("org.apache.pinot:pinot-minion:0.7.1")
    implementation ("org.apache.pinot:pinot-controller:0.7.1")
    testImplementation("org.mockito:mockito-core:2.10.0")
    testImplementation("junit:junit:4.13.1")
    testImplementation ("org.apache.pinot:pinot-integration-tests:0.7.1:tests")
}

tasks.test {
    useJUnit()
}
