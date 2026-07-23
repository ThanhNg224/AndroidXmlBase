# STANDARD.md

## Purpose
This document defines coding conventions for Android projects using Kotlin, MVVM, and Clean Architecture.

## Core Principles
- Consistency over personal preference.  
- Readability over cleverness.  
- Reuse over duplication.  
- Simplicity over over-engineering.  
- Maintainability first.

Base-building phase exception: see `AGENTS.md` → "Current Project Phase" — proactively hardening `core/` infrastructure before a feature needs it is expected while this repo is a base, not over-engineering.

## Project Structure
- Organize by feature instead of layer when possible.  
- Keep feature boundaries clear.  
- Shared code belongs in core/shared only when reused.
- Treat a feature as a capability, not as a synonym for a screen.
- Keep screens inside their owning feature; never create a root-level `screens/` package.

## Clean Architecture
- Presentation: UI only.  
- Domain: Business logic only.  
- Data: Data access only.  
- Dependencies point inward.  
- Never bypass layers.  
- Repository interfaces belong in Domain.  
- Repository implementations belong in Data.

## MVVM
- View renders state.  
- ViewModel owns presentation logic.  
- ViewModel never references View.  
- Use UseCases for business logic.  
- UI reacts to state instead of imperative updates.

## Naming Convention
- Classes: PascalCase (e.g., UserRepository).  
- Functions: camelCase (e.g., fetchUserData).  
- Variables: camelCase (e.g., userName).  
- Constants: UPPER_SNAKE_CASE (e.g., MAX_RETRY_COUNT).  
- Resources: lowercase_with_underscores (e.g., ic_user_profile).  
- Layouts: lowercase_with_underscores (e.g., activity_main).  
- Drawables: lowercase_with_underscores (e.g., bg_button).  
- Colors: lowercase_with_underscores (e.g., color_primary).  
- Dimensions: lowercase_with_underscores (e.g., margin_small).

## Kotlin Guidelines
- Prefer val over var.  
- Favor immutable data structures.  
- Use data classes for simple data holders.  
- Use sealed classes to represent UI state.  
- Avoid nullable types when possible.  
- Use early return to reduce nesting.  
- Keep functions small and focused.  
- Use expression body syntax when it improves readability.  
- Avoid Boolean parameters; prefer sealed classes or enums.

## Coroutines & Flow
- Avoid GlobalScope; use structured concurrency.  
- Use StateFlow for UI state representation.  
- Use SharedFlow for one-time events.  
- Launch coroutines from ViewModel scope.  
- Handle exceptions explicitly and gracefully.

## UI Guidelines
- UI layers should only render state.  
- No business logic in UI components.  
- Create reusable UI components.  
- Ensure accessibility compliance.  
- Load strings from resources only.

## Recycler/List Guidelines
- Use stable data models.  
- Use DiffUtil for efficient updates when applicable.  
- Adapter should only bind UI elements.

## Error Handling
- Fail fast on unexpected errors.  
- Handle expected errors gracefully.  
- Surface user-friendly error messages.  
- Avoid swallowing exceptions silently.

## Logging
- Write meaningful and concise logs.  
- Remove temporary debug logs before release.  
- Never log sensitive or personal data.

## Comments
- Explain why code exists, not what it does.  
- Remove outdated or redundant comments.

## TODO
- Make TODOs actionable.  
- Include context and rationale.

## Code Smells
- God classes that do too much.  
- Long methods with multiple responsibilities.  
- Deeply nested code blocks.  
- Duplicate logic scattered across codebase.  
- Feature leakage across modules.  
- Large ViewModels with excessive logic.  
- Utility classes dumping unrelated functions.

## Definition of Good Code
Good code is:  
- Readable.  
- Testable.  
- Predictable.  
- Consistent.  
- Modular.  
- Maintainable.


## Final Checklist
- [ ] Follow naming conventions consistently.  
- [ ] Adhere to Clean Architecture layering.  
- [ ] Avoid code duplication.  
- [ ] Prefer immutability and null safety.  
- [ ] Use Kotlin idioms and best practices.  
- [ ] Implement structured concurrency with coroutines.  
- [ ] Handle errors explicitly and user-friendly.  
- [ ] Write meaningful and secure logs.  
- [ ] Keep UI free of business logic.  
- [ ] Use resource files for strings and assets.  
- [ ] Ensure accessibility in UI components.  
- [ ] Keep functions small and focused.  
- [ ] Remove outdated comments and TODOs.  
- [ ] Avoid common code smells.  
- [ ] Maintain clear and modular project structure.

---

## Package Organization

Organize code by feature rather than by technical layer whenever practical.

A feature is a vertical slice for one capability or bounded business context. It contains everything required for that capability while respecting architectural boundaries.

A screen is one presentation destination owned by a feature. With one screen, keep `presentation/ui`, `presentation/viewmodel`, and `presentation/state` flat. When a feature has multiple screens, colocate each screen's UI host, ViewModel, and UI state under `presentation/<screen-name>/`; keep shared domain/data code at the feature root.

Do not add screen folders, feature-level presentation components, or empty layers before a real second consumer exists. If two screens do not share a capability or domain/data ownership, they are separate features rather than siblings grouped for navigation convenience.

Shared modules should only contain code that is genuinely reusable across multiple features.

---

## Dependency Injection

- Constructor injection is the default.
- Avoid service locators.
- Inject abstractions rather than implementations.
- Keep dependency graphs simple.
- Do not inject dependencies that are unused.

---

## Repository Guidelines

Repositories coordinate data sources.

Repositories should not contain UI logic.

Repositories should expose domain-friendly models instead of API or database models.

Avoid placing business rules inside repositories unless they are directly related to data coordination.

---

## UseCase Guidelines

Each UseCase should represent a single business capability.

Rules:
- One responsibility.
- Independent from Android framework.
- Easy to test.
- Prefer small composable UseCases.
- Avoid creating UseCases that simply forward repository calls without adding value.

---

## Mapper Guidelines

Separate mapping responsibility from business logic.

Typical mapping flow:

ApiModel → Entity → UiModel

Keep mapping deterministic and side-effect free.

---

## UI State

Represent UI with explicit state objects.

Recommended pattern:
- UiState for persistent screen state.
- UiEvent for user interactions.
- UiEffect for one-time events such as navigation or toast messages.

Avoid exposing mutable state outside the ViewModel.

---

## Visibility

- Default to private.
- Use internal within the same module.
- Use public only when part of the module's public API.
- Reduce visibility whenever possible.

---

## Resource Guidelines

- All user-facing text belongs in string resources.
- Reuse existing resources before creating new ones.
- Use descriptive resource names.
- Avoid duplicate colors, dimensions and styles.

---

## Extension Functions

Extension functions should:
- Improve readability.
- Be small and focused.
- Have no hidden side effects.
- Not replace proper abstractions.

Avoid creating large extension files with unrelated functionality.

---

## Testing Conventions

When writing tests:
- Test behavior rather than implementation.
- Keep tests independent.
- Use descriptive test names.
- One logical scenario per test.
- Prefer deterministic tests.

---

## Definition of Excellent Code

Excellent code is not the shortest or the most clever.

Excellent code is:
- Easy to understand.
- Easy to extend.
- Easy to debug.
- Easy to review.
- Easy to maintain.
- Consistent with the rest of the project.

Future maintainability always has higher priority than short-term implementation speed.
