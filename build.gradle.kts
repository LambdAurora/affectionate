import com.modrinth.minotaur.dependencies.ModDependency
import dev.lambdaurora.mcdev.api.McVersionLookup
import dev.lambdaurora.mcdev.api.ModUtils
import dev.lambdaurora.mcdev.api.ModVersionDependency
import dev.lambdaurora.mcdev.task.packaging.PackageModrinthTask

plugins {
	alias(libs.plugins.loom)
	alias(libs.plugins.lambdamcdev)
	alias(libs.plugins.licenser)
	`java-library`
	`maven-publish`
	id("com.modrinth.minotaur").version("2.+")
}

base.archivesName.set(project.property("archives_base_name") as String)

val mcVersion = libs.versions.minecraft.get()
val compatibleMcVersions: Set<String> = setOf("1.21", "1.21.1")
val VERSION = project.property("mod_version") as String
version = "$VERSION+$mcVersion"

// This field defines the Java version your mod target.
val targetJavaVersion = Integer.parseInt(project.property("java_version").toString())

repositories {
	mavenCentral()
	maven {
		name = "Gegy"
		url = uri("https://maven.gegy.dev/releases/")
		content {
			includeGroup("dev.lambdaurora")
		}
	}
	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/")
		content {
			includeGroup("com.terraformersmc")
		}
	}
	maven {
		name = "Modrinth"
		url = uri("https://api.modrinth.com/maven")
		content {
			includeGroup("maven.modrinth")
		}
	}
}

dependencies {
	minecraft(libs.minecraft)
	@Suppress("UnstableApiUsage")
	mappings(loom.layered {
		officialMojangMappings()
		mappings("dev.lambdaurora:yalmm:${mcVersion}+build.${libs.versions.mappings.yalmm.get()}")
	})
	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.api)

	modImplementation(libs.yumi.mc.foundation)
	modImplementation(libs.ears)

	// Bundling
	include(libs.yumi.mc.foundation)
}

java {
	sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
	targetCompatibility = JavaVersion.toVersion(targetJavaVersion)

	withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.isDeprecation = true
	options.isIncremental = true
	options.release.set(targetJavaVersion)
}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand("version" to (inputs.properties["version"] as String))
	}

	exclude(".cache/**")
}

tasks.jar {
	inputs.property("archivesName", base.archivesName)

	from("LICENSE") {
		rename { "${it}_${inputs.properties["archivesName"]}" }
	}
}

license {
	rule(file("metadata/HEADER"))
}

val README = ModUtils.parseReadme(
	project, "https://raw.githubusercontent.com/LambdAurora/affectionate/1.21/\$2"
)
val CHANGELOG_CONTENT = ModUtils.fetchChangelog(project, VERSION)

val packageModrinth by tasks.registering(PackageModrinthTask::class) {
	this.group = "publishing"
	this.versionType.set(ModUtils.getVersionType(VERSION, mcVersion))
	this.versionName.set("Affectionate $VERSION (${McVersionLookup.getVersionTag(mcVersion)})")
	this.gameVersions.set(listOf(mcVersion) + compatibleMcVersions)
	this.loaders.set(listOf("fabric", "quilt"))
	this.dependencies.set(
		listOf(
			ModVersionDependency("P7dR8mSH", ModVersionDependency.Type.REQUIRED), // Fabric API
		)
	)
	this.changelog.set(CHANGELOG_CONTENT)
	this.readme.set(README)
	this.files.setFrom(tasks.remapJar.get())
}

modrinth {
	projectId.set(project.property("modrinth_id") as String)
	versionName.set("Affectionate $VERSION (${McVersionLookup.getVersionTag(mcVersion)})")
	versionType.set(ModUtils.fetchVersionType(VERSION, mcVersion))
	uploadFile.set(tasks.remapJar)
	loaders.set(listOf("fabric", "quilt"))
	gameVersions.set(listOf(mcVersion) + compatibleMcVersions)
	dependencies.set(
		listOf(
			ModDependency("P7dR8mSH", "required") // Fabric API
		)
	)
	syncBodyFrom.set(README)

	// Changelog fetching
	if (CHANGELOG_CONTENT != null) {
		changelog.set(CHANGELOG_CONTENT)
	} else {
		afterEvaluate {
			tasks.modrinth.get().isEnabled = false
		}
	}

	// If we don't have a MODRINTH_TOKEN, don't run the modrinth publish tasks.
	if (System.getenv("MODRINTH_TOKEN") == null) {
		project.logger.debug("MODRINTH_TOKEN is not set! Disabled modrinth and modrinthSyncBody tasks.")
		tasks.modrinth.get().isEnabled = false
		tasks.modrinthSyncBody.get().isEnabled = false
	}
}

// Configure the maven publication.
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])

			pom {
				name.set("Affectionate")
				description.set("A mod about affectionate player interactions, made for Modfest: Singularity.")
			}
		}
	}

	repositories {
		mavenLocal()
		maven {
			name = "BuildDirLocal"
			url = uri("${rootProject.layout.buildDirectory.get()}/repo")
		}
	}
}