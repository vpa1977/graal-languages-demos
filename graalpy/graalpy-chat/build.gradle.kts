import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot") version "3.4.5"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.graalvm.python") version "24.2.1"
	id ("org.graalvm.buildtools.native") version "0.10.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.graalvm.polyglot:polyglot:24.2.1")
	implementation("org.graalvm.polyglot:python:24.2.1")
	implementation("org.graalvm.tools:dap-tool:24.2.1")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<Zip> {
	isZip64 = true
}

tasks.withType<BootRun> {
   jvmArgs( "-Xss8M" )
}

graalPy {
	/*packages = setOf("accelerate==1.6.0",
		"bitsandbytes==0.45.5",
		"certifi==2025.4.26",
		"charset-normalizer==3.4.2",
		"diffusers==0.30",
		"filelock==3.18.0",
		"fsspec==2025.3.2",
		"huggingface-hub==0.30.2",
		"idna==3.10",
		"importlib_metadata==8.7.0",
		"Jinja2==3.1.6",
		"MarkupSafe==3.0.2",
		"mpmath==1.3.0",
		"networkx==3.4.2",
//		"numpy==2.0.2",
		"packaging==25.0",
		"pillow==11.2.1",
		"pip==23.2.1",
		"protobuf==6.30.2",
		"psutil==7.0.0",
		"PyYAML==6.0.2",
		"regex==2024.11.6",
		"requests==2.32.3",
		"safetensors==0.5.3",
		"sentencepiece==0.2.0",
		"setuptools==65.5.0",
		"sympy==1.14.0",
		"tokenizers==0.13.3",
//		"torch==2.4.1",
		"tqdm==4.67.1",
		"transformers==4.33.3",
		"typing_extensions==4.13.2",
		"urllib3==2.4.0",
		"zipp==3.21.0",
	)*/
}
