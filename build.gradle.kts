import dev.cheat.gradle.MicrosoftAuth
import dev.cheat.gradle.MinecraftSetup
import dev.cheat.gradle.patch.GamePatches
import dev.cheat.gradle.task.CreateVersionTask
import dev.cheat.gradle.task.InstallClientTask
import dev.cheat.gradle.task.MergeJarTask
import dev.cheat.gradle.task.PatchVanillaTask
import java.util.UUID

plugins {
    java
}

val clientName = providers.gradleProperty("client_name").get()
val clientVersion = providers.gradleProperty("client_version").get()
val mcVersion = providers.gradleProperty("minecraft_version").get()
val clientVersionId = "$clientName-$mcVersion"

group = "dev.cheat"
version = clientVersion

// Downloads (once, cached in the Gradle user home) the vanilla client jar and its libraries.
val mc = MinecraftSetup.resolve(gradle.gradleUserHomeDir.toPath(), mcVersion)

val launcherDir: File = providers.gradleProperty("minecraft_dir").orNull?.takeIf { it.isNotBlank() }?.let(::File) ?: run {
    val home = System.getProperty("user.home")
    when (MinecraftSetup.Rules.osName()) {
        "windows" -> File(System.getenv("APPDATA") ?: "$home/AppData/Roaming", ".minecraft")
        "osx" -> File(home, "Library/Application Support/minecraft")
        else -> File(home, ".minecraft")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
}

val decompiler: Configuration by configurations.creating

dependencies {
    compileOnly(files(mc.clientJar))
    compileOnly(files(mc.libraries))
    decompiler("org.vineflower:vineflower:1.12.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.processResources {
    val props = mapOf("name" to clientName, "version" to clientVersion, "minecraft" to mcVersion)
    inputs.properties(props)
    filesMatching("client.properties") { expand(props) }
}

// The plain jar is not useful on its own; the patched game jar is the real artifact.
tasks.jar { enabled = false }

val gameLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }

val patchVanilla = tasks.register<PatchVanillaTask>("patchVanilla") {
    group = "client"
    description = "Applies the bytecode hooks to the vanilla jar and removes Mojang's signature."
    vanillaJar = mc.clientJar.toFile()
    hooksClass.from(sourceSets.main.flatMap { set -> set.java.classesDirectory.map { it.file("${GamePatches.HOOKS}.class") } })
    libraries.from(mc.libraries)
    jdkHome = gameLauncher.map { it.metadata.installationPath }
    jdkVersion = gameLauncher.map { "${it.metadata.vendor} ${it.metadata.javaRuntimeVersion}" }
    outputJar = layout.buildDirectory.file("libs/patched-vanilla-$mcVersion.jar")
}

val patchJar = tasks.register<MergeJarTask>("patchJar") {
    group = "client"
    description = "Merges the client classes and resources into the patched vanilla jar."
    patchedJar = patchVanilla.flatMap { it.outputJar }
    clientFiles.from(sourceSets.main.map { it.output })
    outputJar = layout.buildDirectory.file("libs/$clientVersionId.jar")
}

val createVersion = tasks.register<CreateVersionTask>("createVersion") {
    group = "client"
    description = "Creates a launcher version folder (json + jar) in build/version."
    vanillaVersionJson = mc.versionJsonFile.toFile()
    clientJar = patchJar.flatMap { it.outputJar }
    this.versionId = clientVersionId
    outputDir = layout.buildDirectory.dir("version")
}

tasks.assemble { dependsOn(createVersion) }

tasks.register<InstallClientTask>("installClient") {
    group = "client"
    description = "Installs the client into the official launcher and adds an installation for it."
    versionDir = createVersion.flatMap { it.outputDir }
    this.versionId = clientVersionId
    profileName = "$clientName $clientVersion ($mcVersion)"
    this.minecraftDir = launcherDir
}

/**
 * Shared setup for the run tasks: patched jar + client classes on the classpath, run/ as game directory.
 * [session] is resolved when the task executes and returns (username, uuid, accessToken, userType).
 */
fun JavaExec.configureRun(session: () -> List<String>) {
    group = "client"
    javaLauncher = gameLauncher
    mainClass = mc.mainClass()
    classpath(patchVanilla.flatMap { it.outputJar }, sourceSets.main.map { it.output }, mc.libraries)

    val runDir = layout.projectDirectory.dir("run").asFile
    workingDir = runDir
    // ./gradlew runClient -PquickWorld="New World" joins that singleplayer world (in run/saves) directly.
    val quickWorld = providers.gradleProperty("quickWorld").orNull
    // ./gradlew runClientOnline -PquickServer=play.example.com:25565 joins that server directly.
    val quickServer = providers.gradleProperty("quickServer").orNull
    doFirst {
        runDir.mkdirs()
        val natives = File(runDir, "natives").absolutePath.replace('\\', '/')
        jvmArgs(mc.jvmArguments()
            .filter { it != "-cp" && it != "\${classpath}" }
            .map {
                it.replace("\${natives_directory}", natives)
                    .replace("\${launcher_name}", clientName)
                    .replace("\${launcher_version}", clientVersion)
            } + listOf("-Xmx4G", "-Dclient.dev=true"))
        val (username, uuid, accessToken, userType) = session()
        args(
            "--username", username,
            "--uuid", uuid,
            "--accessToken", accessToken,
            "--userType", userType,
            "--version", clientVersionId,
            "--versionType", "release",
            "--gameDir", runDir.absolutePath,
            "--assetsDir", mc.assetsDir(launcherDir.toPath()).toString(),
            "--assetIndex", mc.assetIndexId(),
        )
        if (!quickWorld.isNullOrBlank()) args("--quickPlaySingleplayer", quickWorld)
        if (!quickServer.isNullOrBlank()) args("--quickPlayMultiplayer", quickServer)
    }
}

tasks.register<JavaExec>("runClient") {
    description = "Starts the patched client in an offline dev session with run/ as game directory."
    val username = providers.gradleProperty("dev_username").getOrElse("Dev")
    configureRun {
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString().replace("-", "")
        listOf(username, uuid, "0", "legacy")
    }
}

tasks.register<JavaExec>("runClientOnline") {
    description = "Starts the patched client logged in with a Microsoft account, so it can join online-mode servers."
    // Holds the Microsoft refresh token. Delete it to log out or switch accounts.
    val sessionFile = gradle.gradleUserHomeDir.toPath().resolve("caches/client-template/auth/microsoft.json")
    configureRun {
        val session = MicrosoftAuth.login(sessionFile) { logger.lifecycle(it) }
        listOf(session.username(), session.uuid(), session.accessToken(), "msa")
    }
}

tasks.register<JavaExec>("genSources") {
    group = "client"
    description = "Decompiles vanilla Minecraft into build/minecraft-sources for reading (not compiled)."
    classpath = decompiler
    mainClass = "org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
    val out = layout.buildDirectory.dir("minecraft-sources").get().asFile
    outputs.dir(out)
    doFirst { out.mkdirs() }
    args(listOf("-dgs=1", "-rsy=1", "-log=WARN", "--only=net/minecraft", "--only=com/mojang") +
        mc.libraries.map { "-e=$it" } + listOf(mc.clientJar.toString(), out.absolutePath))
    maxHeapSize = "4G"
}
