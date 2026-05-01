@smoke
Feature: Create milestone API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: create milestone with title only returns milestone id and title
    Given path 'api/milestones'
    And request { title: 'Release v1.0' }
    When method post
    Then status 201
    And match response == { milestoneId: '#number', title: 'Release v1.0' }

  Scenario: create milestone with optional fields returns minimal response
    Given path 'api/milestones'
    And request
      """
      {
        "title": "Q2 2026 Delivery",
        "description": "Second quarter release cycle",
        "startDate": "2026-04-01",
        "dueDate": "2026-06-30"
      }
      """
    When method post
    Then status 201
    And match response == { milestoneId: '#number', title: 'Q2 2026 Delivery' }

  Scenario: create milestone rejects non-increasing date range
    Given path 'api/milestones'
    And request { title: 'Q2 2026 Delivery', startDate: '2026-06-30', dueDate: '2026-06-30' }
    When method post
    Then status 400
    And match response.code == 'VALIDATION_ERROR'
    And match response.message == 'Request validation failed'
    And match response.details == '#[]'
    And match response.details[0] contains 'dueDate'
    And match response.details[0] contains 'startDate'