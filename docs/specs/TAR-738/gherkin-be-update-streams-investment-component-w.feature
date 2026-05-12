@TAR-738
Feature: BE: update Streams investment component with investment-service-api 1.6 or latest
  As a integration developer / bank implementer consuming Streams APIs
  I want to use new features of the investment service for portfolio-model ingestion
  So that I can use new fields and capabilities introduced in investment-service-api 1.6.0 in portfolio ingestion

  Background:
    Given the investment module is configured to use investment-service-api 1.6.0

  # --- Functional Scenarios (from Acceptance Criteria) ---

  @functional @AC-1
  Scenario: Investment module builds successfully after API version upgrade
    When a developer runs the investment module build
    Then the build completes successfully
    And no tests are skipped
    And no test failures are reported

  # --- Edge Case Scenarios (from Edge Cases table) ---

  @edge-case @AC-1
  Scenario: Build fails when test fixtures reference model fields removed in investment-service-api 1.6.0
    Given existing test fixtures use model fields that are no longer present in investment-service-api 1.6.0
    When a developer runs the investment module build
    Then the build fails with compilation errors referencing the outdated model fields
