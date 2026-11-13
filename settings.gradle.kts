/*
 * Round The Corner 2 - multi-module Android build.
 *
 * Module map (dependencies only point "down" this list):
 *   :app                  - entry point; wires features, DI graph, navigation
 *   :feature:*            - one module per user-facing area; never depend on each other
 *   :core:data            - repositories: the only API features use for data
 *   :core:database        - Room entities, DAOs, schema migrations
 *   :core:domain          - pure-Kotlin business logic (geodesy, trigger state machine)
 *   :core:designsystem    - theme, colors, typography, shared composables (model-agnostic)
 *   :core:ui              - shared composables that know the domain model (place icons, ...)
 *   :core:maps            - MapLibre map wrapped for Compose (OpenFreeMap tiles)
 *   :core:network         - OkHttp client + API clients (Nominatim geocoding)
 *   :core:messaging       - message senders: Slack/Discord webhooks, SMS (auto or tap-to-send)
 *   :core:model           - pure-Kotlin domain types (no Android imports)
 *   :core:testing         - fakes + screenshot helpers (test-only)
 *
 *   :core:location        - device location (Fused provider, platform fallback)
 *   :core:monitoring      - the foreground service, live notification, firing + retries
 */
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Fail the build if a module declares its own repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RoundTheCorner2"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:designsystem")
include(":core:database")
include(":core:network")
include(":core:messaging")
include(":core:location")
include(":core:monitoring")
include(":core:maps")
include(":core:ui")
include(":core:data")
include(":core:testing")
include(":feature:triggers")
include(":feature:places")
include(":feature:recipients")
include(":feature:settings")
