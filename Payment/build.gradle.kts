plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Payment"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // WebFlux + R2DBC + Kafka (기존)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.kafka:spring-kafka")

    // 🔐 Security + JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // (선택) Redis 사용해서 세션/토큰 상태 확인까지 하고 싶으면 추가
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Lombok & Devtools
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")

    // DB 드라이버 & R2DBC
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("io.asyncer:r2dbc-mysql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // 토스 JWE 암호화용 Nimbus JOSE + JWT
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")   // EncryptionMethod, JWEObject 등 [web:484][web:488][web:500]

    // JSON.simple
    implementation("com.googlecode.json-simple:json-simple:1.1.1") // JSONObject, JSONParser [web:489][web:492][web:498]
}

tasks.withType<Test> {
    useJUnitPlatform()
}
