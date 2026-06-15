plugins {
	kotlin("jvm")
}

group = "com.terasxgod"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Jackson для сериализации JSON
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
	// Validation аннотации
	implementation("jakarta.validation:jakarta.validation-api:3.0.2")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}



