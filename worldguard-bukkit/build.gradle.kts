import buildlogic.internalVersion
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.platform")
}

dependencies {
    "api"(project(":worldguard-core"))
    "api"(libs.worldedit.bukkit) { isTransitive = false }
    "compileOnly"(libs.commandbook) { isTransitive = false }

    "compileOnly"(libs.jetbrains.annotations) {
        because("Resolving Spigot annotations")
    }
    "testCompileOnly"(libs.jetbrains.annotations) {
        because("Resolving Spigot annotations")
    }
    "compileOnly"(libs.paperApi) {
        exclude("org.slf4j", "slf4j-api")
        exclude("junit", "junit")
    }

    "implementation"(libs.paperLib)
    "implementation"(libs.bstats.bukkit)
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.internalVersion
    inputs.property("internalVersion", internalVersion)
    filesMatching("plugin.yml") {
        expand("internalVersion" to internalVersion.get())
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency(":worldguard-core"))
        include(dependency("org.bstats:bstats-bukkit"))
        include(dependency("org.bstats:bstats-base"))
        include(dependency("io.papermc:paperlib"))

        relocate("org.bstats", "com.sk89q.worldguard.bukkit.bstats")
        relocate("io.papermc.lib", "com.sk89q.worldguard.bukkit.paperlib")
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
    }
}

// XMine start - публикация в свой Reposilite
//
// Своя координата ru.xmine.thirdparty:worldguard в разделе-кандидате пары форков
// fork-snapshot (вики, ADR-0056) - оттуда jar берёт XMineNode (plugins/common.yml).
// Публикуется ровно тот файл, что едет на ноду: shadowJar (*-dist.jar). Версия -
// адрес сборки <апстрим>-<ветка>-<дата>-<хеш>, её считает
// .github/workflows/xmine-publish.yml и передаёт через -PxmineVersion. Значение по
// умолчанию - только чтобы локальная сборка без -P не падала.
//
// Апстримная публикация "maven" с появлением репозитория тоже получает задачу
// публикации в него, но workflow зовёт только задачу своей координаты.
configure<PublishingExtension> {
    publications.create<MavenPublication>("xmineFork") {
        groupId = "ru.xmine.thirdparty"
        artifactId = "worldguard"
        version = providers.gradleProperty("xmineVersion").getOrElse("0.0.0-xmine-local")
        artifact(tasks.named("shadowJar")) {
            // Классификатор задачи (dist) уехал бы в координату - обнуляем.
            classifier = null
        }
    }
    repositories {
        maven {
            name = "xmine"
            url = uri(providers.environmentVariable("XMINE_MAVEN_URL").getOrElse("https://maven.xmine.world/fork-snapshot"))
            credentials {
                username = providers.environmentVariable("XMINE_MAVEN_USERNAME").orNull
                password = providers.environmentVariable("XMINE_MAVEN_PASSWORD").orNull
            }
        }
    }
}
// XMine end - публикация в свой Reposilite
