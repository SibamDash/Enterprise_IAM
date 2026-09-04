# Lessons Learned

## Technical Lessons
- **Spring Authorization Server:** Integrating an OAuth2 server into an existing generic API requires careful ordering of SecurityFilterChain beans and exception handling (e.g., redirecting to a custom SPA login page rather than the default Spring Security login page).

## Debugging Lessons
- **Mocking Final Classes:** When enforcing JDK 21 compatibility with mock-maker-subclass, Mockito completely loses the ability to mock final classes. It is much safer to rely on framework-provided Builders (like JwtEncodingContext.with()) in test suites to prevent JDK upgrades from breaking the test infrastructure.
