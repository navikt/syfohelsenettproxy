import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import no.nils.wsdl2java.Wsdl2JavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val artemisVersion = "2.6.4"
val avroVersion = "1.8.2"
val confluentVersion = "5.0.0"
val syfooppgaveSchemasVersion = "1.2-SNAPSHOT"
val coroutinesVersion = "1.1.1"
val fellesformatVersion = "1.0"
val infotrygdForespVersion = "1.0.1-SNAPSHOT"
val ibmMqVersion = "9.1.0.0"
val javaxActivationVersion = "1.1.1"
val jacksonVersion = "2.9.7"
val jaxbApiVersion = "2.4.0-b180830.0359"
val jaxbVersion = "2.3.0.1"
val jedisVersion = "2.9.0"
val kafkaVersion = "2.0.0"
val kafkaEmbeddedVersion = "2.1.1"
val kluentVersion = "1.39"
val ktorVersion = "1.2.0"
val logbackVersion = "1.2.3"
val logstashEncoderVersion = "5.1"
val prometheusVersion = "0.5.0"
val spekVersion = "2.0.2"
val sykmeldingVersion = "1.1-SNAPSHOT"
val cxfVersion = "3.2.7"
val jaxwsApiVersion = "2.3.1"
val commonsTextVersion = "1.4"
val navArbeidsfordelingv1Version = "1.1.0"
val navPersonv3Version = "3.2.0"
val jaxbBasicAntVersion = "1.11.1"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsToolsVersion = "2.3.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val kithHodemeldingVersion = "1.1"
val smCommonVersion = "1.0.21"
val kontrollsystemblokk = "1.0.8-SNAPSHOT"
val javaxJaxwsApiVersion = "2.2.1"

plugins {
    java
    id("no.nils.wsdl2java") version "0.10"
    kotlin("jvm") version "1.3.31"
    id("org.jmailen.kotlinter") version "1.26.0"
    id("com.diffplug.gradle.spotless") version "3.14.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

buildscript {
    dependencies {
        classpath("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        classpath("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
        classpath("com.sun.activation:javax.activation:1.2.0")
        classpath("com.sun.xml.ws:jaxws-tools:2.3.1")
    }
}

repositories {
    maven (url= "https://repo.adeo.no/repository/maven-snapshots/")
    maven (url= "https://repo.adeo.no/repository/maven-releases/")
    maven (url= "https://dl.bintray.com/kotlin/ktor")
    maven (url= "https://dl.bintray.com/spekframework/spek-dev")
    maven (url= "http://packages.confluent.io/maven/")
    maven (url= "https://kotlin.bintray.com/kotlinx")
    mavenCentral()
    jcenter()
}

val navWsdl= configurations.create("navWsdl") {
    setTransitive(false)
}

dependencies {
    wsdl2java("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    wsdl2java("javax.activation:activation:$javaxActivationVersion")
    wsdl2java("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    wsdl2java("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    wsdl2java("javax.xml.ws:jaxws-api:$javaxJaxwsApiVersion")
    wsdl2java("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }

    implementation(kotlin("stdlib"))

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation ("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation ("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation ("io.ktor:ktor-server-netty:$ktorVersion")
    implementation ("io.ktor:ktor-client-apache:$ktorVersion")
    implementation ("io.ktor:ktor-client-auth-basic:$ktorVersion")
    implementation ("io.ktor:ktor-client-jackson:$ktorVersion")

    implementation ("ch.qos.logback:logback-classic:$logbackVersion")
    implementation ("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation ("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation ("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation ("no.nav.helse.xml:sm2013:$sykmeldingVersion")
    implementation ("no.nav.syfo.tjenester:fellesformat:$fellesformatVersion")
    implementation ("no.nav.helse.xml:kontrollsystemblokk:$kontrollsystemblokk")
    implementation("no.nav.tjenester:nav-arbeidsfordeling-v1-tjenestespesifikasjon:$navArbeidsfordelingv1Version:jaxws")
    implementation ("no.nav.tjenester:nav-person-v3-tjenestespesifikasjon:$navPersonv3Version")
    implementation ("no.nav.syfo.tjenester:kith-hodemelding:$kithHodemeldingVersion")

    implementation("no.nav.syfo.sm:syfosm-common-models:$smCommonVersion")
    implementation("no.nav.syfo.sm:syfosm-common-ws:$smCommonVersion")
    implementation("no.nav.syfo.sm:syfosm-common-networking:$smCommonVersion")

    implementation ("org.apache.commons:commons-text:$commonsTextVersion")
    implementation ("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation ("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation ("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation ("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")

    implementation ("javax.xml.ws:jaxws-api:$jaxwsApiVersion")
    implementation ("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    implementation ("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    implementation ("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    implementation ("javax.activation:activation:$javaxActivationVersion")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }

    testImplementation ("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation ("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
    }
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
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

    withType<KotlinCompile> {
        dependsOn("wsdl2java")
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Wsdl2JavaTask> {
        wsdlDir = file("$projectDir/src/main/resources/wsdl")
        wsdlsToGenerate = listOf(
            mutableListOf("-xjc", "-b", "$projectDir/src/main/resources/xjb/binding.xml", "$projectDir/src/main/resources/wsdl/helsepersonellregisteret.wsdl")
        )
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging {
            showStandardStreams = true
        }
    }

}
