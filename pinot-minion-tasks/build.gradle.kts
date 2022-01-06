plugins {
    `java-library`
    jacoco
    id("org.hypertrace.jacoco-report-plugin") version "0.2.0"
}

dependencies {
    compileOnly("org.apache.pinot:pinot-core:0.7.1")
    implementation("junit:junit:4.13.1")
    implementation ("org.apache.pinot:pinot-minion:0.7.1")
    implementation ("org.apache.pinot:pinot-controller:0.7.1")
    implementation ("org.apache.pinot:pinot-broker:0.7.1")
    implementation ("org.apache.pinot:pinot-server:0.7.1")
    implementation ("org.apache.pinot:pinot-avro-base:0.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}
