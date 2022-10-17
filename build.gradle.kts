import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.6.4"
val jacksonVersion = "2.13.4"
val jaxbApiVersion = "2.4.0-b180830.0359"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val kluentVersion = "1.68"
val ktorVersion = "2.1.2"
val logbackVersion = "1.4.3"
val logstashEncoderVersion = "7.2"
val prometheusVersion = "0.16.0"
val kotestVersion = "5.5.0"
val cxfVersion = "3.2.7"
val commonsTextVersion = "1.10.0"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsApiVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.2"
val javaxJaxwsApiVersion = "2.2.1"
val javaxActivationVersion = "1.1.1"
val smCommonVersion = "1.cbb3aed"
val jedisVersion = "4.2.3"
val testcontainersVersion = "1.17.4"
val mockkVersion = "1.13.2"
val nimbusdsVersion = "9.24.3"
val kotlinVersion = "1.7.20"
val jaxbImplVersion = "2.3.3"
val wsApiVersion = "2.3.3"
val jakartaAnnotationApiVersion = "1.3.5"

plugins {
    java
    id("io.mateo.cxf-codegen") version "1.0.1"
    kotlin("jvm") version "1.7.20"
    id("org.jmailen.kotlinter") version "3.10.0"
    id("com.diffplug.spotless") version "6.5.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
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

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

val navWsdl = configurations.create("navWsdl") {
    isTransitive = false
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

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("no.nav.helse:syfosm-common-ws:$smCommonVersion")

    implementation("org.apache.commons:commons-text:$commonsTextVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")

    implementation("javax.xml.ws:jaxws-api:$jaxwsApiVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    implementation("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    implementation("javax.activation:activation:$javaxActivationVersion")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    implementation("redis.clients:jedis:$jedisVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}


tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.BootstrapKt"
    }

    create("printVersion") {

        doLast {
            println(project.version)
        }
    }

    cxfCodegen {
        wsdl2java {
            register("helsepersonellregisteret") {
                wsdl.set(file("$projectDir/src/main/resources/wsdl/helsepersonellregisteret.wsdl"))
                bindingFiles.add("$projectDir/src/main/resources/xjb/binding.xml")
            }
        }
    }

    withType<KotlinCompile> {
        dependsOn("wsdl2javaHelsepersonellregisteret")
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    "check" {
        dependsOn("formatKotlin")
    }
}
