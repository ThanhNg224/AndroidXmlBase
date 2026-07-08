# AGENTS.md

## Mission

This file defines how AI assistants should contribute to any Android project. Contributions must prioritize maintainability, consistency, adherence to Clean Architecture principles, and long-term code quality.

---

## Core Engineering Principles

- Understand before implementing.
- Reuse before creating.
- Consistency over perfection.
- Simplicity over cleverness.
- Readability over brevity.
- Refactor only when valuable.
- Prefer extending existing implementations.
- Never optimize without evidence.

---

## AI Workflow

- Understand requirements thoroughly.
- Explore the existing codebase.
- Find similar implementations.
- Follow existing patterns.
- Implement the smallest complete solution.
- Review the result carefully.
- Explain important decisions clearly.

AI must never immediately start coding without completing these steps.

---

## Decision Priority

1. Existing implementation  
2. Shared reusable component  
3. Extend existing implementation  
4. Small refactor  
5. New implementation  
6. New abstraction  

---

## Before Creating Anything

Before creating a new Activity, Fragment, Composable, ViewModel, UseCase, Repository, Mapper, Adapter, Helper, Extension, Utility, Dialog, or Custom View, AI must verify whether one already exists.

---

## Clean Architecture Rules

- Respect architectural boundaries.  
- Dependencies point inward.  
- UI contains presentation logic only.  
- Domain contains business rules.  
- Data handles persistence and networking.  
- Never bypass layers.  
- Never place business logic inside UI.  

Project-specific architecture details belong in ARCHITECTURE.md.

---

## Feature Development

- Match existing feature structure.  
- Reuse existing navigation and state management patterns.  
- Keep feature boundaries clear.  
- Avoid introducing project-wide abstractions unless multiple features require them.

---

## Code Quality

- Write small, focused functions.  
- Use meaningful names.  
- Prefer immutable state when practical.  
- Use early returns to reduce nesting.  
- Remove dead code.  
- Avoid magic numbers.  
- Avoid nested complexity.  
- Prefer composition over inheritance.  
- Keep code easy to review.

---

## Refactoring Guidelines

- Refactor with clear purpose.  
- Avoid unrelated cleanup.  
- Preserve existing behavior.  
- Keep commits focused.  
- Large rewrites require explicit request.

---

## Communication

- Ask instead of guessing.  
- Explain trade-offs.  
- Mention architectural impacts.  
- Highlight assumptions.

---

## Never Do

- Duplicate business logic.  
- Bypass architecture.  
- Mix UI and business logic.  
- Create Manager/Helper/Util dumping classes.  
- Add abstractions without real reuse.  
- Rewrite unrelated files.  
- Introduce breaking changes silently.  
- Ignore existing conventions.

---

## Definition of Done

- Builds successfully.  
- Existing patterns reused.  
- Architecture respected.  
- Naming consistent.  
- Minimal implementation.  
- No duplicated logic.  
- No dead code.  
- No debug leftovers.  
- Self-reviewed.

---

## Final Review Checklist

- [ ] Architecture boundaries respected  
- [ ] Existing patterns reused appropriately  
- [ ] Naming is clear and consistent  
- [ ] Code is readable and maintainable  
- [ ] No duplicated logic introduced  
- [ ] Implementation is as simple as possible  
- [ ] No unnecessary abstractions added  
- [ ] No negative impact on testing or testability  
- [ ] No performance regressions introduced  
- [ ] Null safety and error handling considered  
- [ ] Logging is appropriate and not excessive  
- [ ] Documentation and comments are sufficient and relevant  
- [ ] Code formatting follows project conventions  
- [ ] Changes limited to intended scope

---

## Repository Exploration

Before implementing any feature or bug fix, inspect the repository.

Recommended search order:

1. Existing feature with similar behavior.
2. Shared UI components.
3. Shared domain logic.
4. Existing utilities and extensions.
5. Existing architecture patterns.

Do not assume a component does not exist until it has been searched.

---

## Pattern Matching

Every new implementation should resemble the existing codebase.

Prefer matching:

- Project structure
- Naming convention
- Dependency injection pattern
- Error handling
- UI state management
- Navigation style
- Repository pattern
- Testing approach

Consistency is more valuable than introducing a different "better" solution.

---

## Kotlin Best Practices

- Prefer immutable objects.
- Prefer val over var.
- Keep extension functions focused.
- Avoid giant utility files.
- Use sealed classes where state is finite.
- Prefer composition over inheritance.
- Keep coroutine scopes explicit.
- Never use GlobalScope.
- Avoid nullable types unless necessary.
- Fail early whenever possible.

---

## Android Best Practices

- Keep Activities and Fragments lightweight.
- ViewModels own presentation logic.
- Business logic belongs in the Domain layer.
- Data layer owns networking and persistence.
- UI should react to state instead of controlling business logic.
- Avoid leaking Android framework classes into business logic.

---

## Performance Guidelines

Only optimize code when there is measurable evidence.

Do not sacrifice readability for micro-optimizations.

Avoid unnecessary allocations, excessive recomposition, repeated database queries, and duplicated network requests.

---

## Anti-Patterns

Avoid introducing:

- God classes
- Massive ViewModels
- Manager classes with unrelated responsibilities
- Helper or Util classes containing random logic
- Circular dependencies
- Deep inheritance hierarchies
- Copy-paste implementations
- Feature-specific code inside shared modules
- Shared modules depending on feature modules

---

## Definition of Good Code

Good code is:

- Easy to understand.
- Easy to modify.
- Easy to test.
- Easy to review.
- Predictable.
- Consistent.
- Loosely coupled.
- Highly cohesive.

Readable code is preferred over clever code.

---

## AI Response Expectations

When responding:

- Explain significant architectural decisions.
- Mention trade-offs when multiple solutions exist.
- State assumptions explicitly.
- Suggest reuse before proposing new implementations.
- Recommend refactoring only when it provides clear value.
- Keep explanations concise and actionable.

---


## Guiding Principle

The primary objective is not to produce code quickly.

The primary objective is to produce code that naturally fits into the existing project, is easy for other developers to understand, and remains maintainable over time.

When in doubt, choose the solution that minimizes long-term maintenance cost rather than the one that introduces the newest or most sophisticated technique.

---

## Code Review Mindset

Every implementation should be reviewed before being considered complete.

Review in the following order:

1. Correctness
2. Architecture
3. Reusability
4. Readability
5. Simplicity
6. Performance
7. Maintainability

If a problem can be prevented during review, fix it before delivering the final solution.

---

## Refactoring Triggers

Refactor only when one or more of the following is true:

- Business logic is duplicated.
- Responsibilities are unclear.
- The implementation is difficult to understand.
- Existing architecture is violated.
- Multiple files implement the same concept differently.
- A reusable pattern has clearly emerged.

Do not refactor simply because a different style is preferred.

---

## Architecture Smells

Watch for these warning signs:

- UI performing business decisions.
- ViewModels directly accessing APIs or databases.
- Domain depending on Android framework classes.
- Data layer exposing implementation details.
- Circular feature dependencies.
- Large classes with multiple unrelated responsibilities.
- Excessive inheritance.
- Utility classes becoming dumping grounds.

Whenever possible, improve these incrementally instead of rewriting entire features.

---

## Handling Existing Code

Existing code is part of the project history.

Unless explicitly requested:

- Preserve public APIs.
- Preserve behavior.
- Minimize breaking changes.
- Minimize file churn.
- Keep diffs focused.

Prefer evolutionary improvements over complete rewrites.

---

## Handling New Features

For new features:

- Follow the project's established feature structure.
- Keep dependencies isolated.
- Prefer existing UI components.
- Introduce new abstractions only after repeated need.
- Design for maintainability instead of maximum flexibility.

---

## Prompt Interpretation

Before implementing, determine whether the request is asking to:

- Fix an existing implementation.
- Extend existing behavior.
- Refactor existing code.
- Introduce a new feature.
- Improve performance.
- Improve readability.
- Explain existing code.

Do not assume a rewrite is expected when a small change satisfies the request.

---

## Engineering Mindset

Good engineering is measured by how easily future developers can understand and extend the code.

Every decision should reduce future maintenance cost, preserve architectural consistency, and improve long-term sustainability.

If multiple solutions are technically correct, choose the one that best fits the existing project rather than the most sophisticated implementation.
