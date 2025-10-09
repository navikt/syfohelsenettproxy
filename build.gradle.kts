import io.mateo.cxf.codegen.wsdl2java.Wsdl2Java
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"

val javaVersion = JvmTarget.JVM_21


val coroutinesVersion = "1.10.2"
val jacksonVersion = "2.20.0"
val jaxbApiVersion = "2.4.0-b180830.0359"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val kluentVersion = "1.73"
val ktorVersion = "3.3.1"
val logbackVersion = "1.5.19"
val logstashEncoderVersion = "8.1"
val prometheusVersion = "0.16.0"
val cxfVersion = "4.0.6"
val commonsTextVersion = "1.14.0"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsApiVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.2"
val javaxJaxwsApiVersion = "2.2.1"
val javaxActivationVersion = "1.1.1"
val valkeyVersion = "5.4.0"
val testcontainersVersion = "1.21.3"
val mockkVersion = "1.14.6"
val nimbusdsVersion = "10.5"
val kotlinVersion = "2.2.20"
val jaxbImplVersion = "2.3.3"
val wsApiVersion = "2.3.3"
val jakartaAnnotationApiVersion = "1.3.5"
val ktfmtVersion = "0.44"
val junitJupiterVersion = "6.0.0"
val koinVersion = "4.1.1"

///Due to vulnerabilities
val bcprovJdk18onVersion = "1.82"
val guavaVersion = "33.5.0-jre"
val commonsCompressVersion = "1.28.0"
val nettyHandlerVersion = "4.2.6.Final"

plugins {
    id("application")
    id("io.mateo.cxf-codegen") version "2.4.1"
    kotlin("jvm") version "2.2.20"
    id("com.diffplug.spotless") version "8.0.0"
}

application {
    mainClass.set("no.nav.syfo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

buildscript {
    dependencies {
        classpath("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        classpath("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
        classpath("com.sun.activation:javax.activation:1.2.0")
        classpath("com.sun.xml.ws:jaxws-tools:2.3.1") {
            exclude(group = "com.sun.xml.ws", module = "policy")
        }
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    maven {
        url = uri("https://repo.jenkins-ci.org/public")
    }
}

dependencies {
    cxfCodegen("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    cxfCodegen("javax.activation:activation:$javaxActivationVersion")
    cxfCodegen("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    cxfCodegen("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    cxfCodegen("javax.xml.ws:jaxws-api:$javaxJaxwsApiVersion")
    cxfCodegen("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    cxfCodegen("com.sun.xml.bind:jaxb-impl:$jaxbImplVersion")
    cxfCodegen("jakarta.xml.ws:jakarta.xml.ws-api:$wsApiVersion")
    cxfCodegen("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationApiVersion")
    cxfCodegen("org.apache.commons:commons-text:$commonsTextVersion")
    cxfCodegen("org.apache.cxf:cxf-core:$cxfVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    {
        constraints {
            implementation("io.netty:netty-handler:$nettyHandlerVersion") {
                because("Due to vulnerabilities in io.ktor:ktor-server-netty")
            }
        }
    }
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")


    implementation("org.apache.commons:commons-text:$commonsTextVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion") {
        exclude(group = "org.apache.velocity", module = "velocity")
    }
    constraints {
        implementation("org.bouncycastle:bcprov-jdk18on:$bcprovJdk18onVersion") {
            because("override transient from org.apache.cxf:cxf-rt-ws-security")
        }
    }
    constraints {
        implementation("com.google.guava:guava:$guavaVersion") {
            because("override transient from org.apache.cxf:cxf-rt-ws-security")
        }
    }


    implementation("javax.xml.ws:jaxws-api:$jaxwsApiVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    implementation("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    implementation("javax.activation:activation:$javaxActivationVersion")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    implementation("io.valkey:valkey-java:$valkeyVersion")

    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    constraints {
        implementation("org.apache.commons:commons-compress:$commonsCompressVersion") {
            because("Due to vulnerabilities, see CVE-2024-26308")
        }
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        jvmTarget = javaVersion
    }
}

tasks {

    build {
        dependsOn("installDist")
    }

    cxfCodegen {
        register("wsdl2javaHelsepersonellregisteret", Wsdl2Java::class) {
            toolOptions {
                wsdl.set(layout.projectDirectory.file("src/main/resources/wsdl/helsepersonellregisteret.wsdl").asFile.toPath().toAbsolutePath().toString())
                bindingFiles.add(layout.projectDirectory.file("src/main/resources/xjb/binding.xml").asFile.absolutePath)
            }
        }

        register("wsdl2javaFastlegeinformasjonEksport", Wsdl2Java::class) {
            toolOptions {
                wsdl.set(layout.projectDirectory.file("src/main/resources/wsdl/fastlegeinformasjonregisteret.wsdl").asFile.toPath().toAbsolutePath().toString())
            }
        }
    }

    compileKotlin {
        dependsOn("wsdl2javaHelsepersonellregisteret")
        dependsOn("wsdl2javaFastlegeinformasjonEksport")
    }


        withType<Test> {

        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
