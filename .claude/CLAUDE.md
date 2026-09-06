# Android project instructions

This repository is an Android multi-module app built with Kotlin, Compose, Retrofit, and Gradle version catalogs.

## Agent selection

Use the `android` agent for changes related to:
- app and data module work
- Compose UI and navigation
- repository/data layer and persistence
- Gradle, dependency, and module structure updates
- tests, debugging, and architecture decisions

## Relevant skills

- [android-module-structure](./my-skill/android-module-structure/SKILL.md)
- [android-data-layer](./my-skill/android-data-layer/SKILL.md)
- [android-presentation-mvi](./my-skill/android-presentation-mvi/SKILL.md)
- [android-navigation](./my-skill/android-navigation/SKILL.md)
- [android-di-koin](./my-skill/android-di-koin/SKILL.md)
- [android-testing](./my-skill/android-testing/SKILL.md)
- [android-compose-ui](./my-skill/android-compose-ui/SKILL.md)
- [android-error-handling](./my-skill/android-error-handling/SKILL.md)

## Project rules

- Keep the multi-module structure: `:app` and `:data`.
- Prefer Gradle version catalogs over hardcoded versions.
- Favor Compose and ViewModel patterns over ad hoc imperative UI.
- Keep data access behind repositories and avoid leaking network models directly into UI.
- Validate changes with the smallest relevant Gradle target (`./gradlew test` or module-specific checks).
- Preserve offline-capable behavior and local persistence requirements from the assignment.
- Add or update tests for bug fixes and new behavior.
