plugins {
    `java-library`
    jacoco
    id("org.hypertrace.jacoco-report-plugin") version "0.2.0"
}

dependencies {
    compileOnly("org.apache.pinot:pinot-core:0.7.1")
    implementation ("org.apache.pinot:pinot-minion:0.7.1")
    implementation ("org.apache.pinot:pinot-controller:0.7.1")
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}
