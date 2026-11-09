/*
 * Round The Corner 2 - multi-module Android build.
 *
 * Module map (dependencies only point "down" this list):
 *   :app                  - entry point; wires features, DI graph, navigation
 *   :feature:*            - one module per user-facing area; never depend on each other
 *   :core:data            - repositories (DataStore today; Room in milestone 2)
 *   :core:designsystem    - theme, colors, typography, shared composables
 *   :core:model           - pure-Kotlin domain types (no Android imports)
 *   :core:testing         - fakes + screenshot helpers (test-only)
 *
 * Later milestones add :core:location, :core:messaging and :feature:monitor.
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
include(":core:designsystem")
include(":core:data")
include(":core:testing")
include(":feature:triggers")
include(":feature:places")
include(":feature:settings")
