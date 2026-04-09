@smoke
Feature: Health check endpoint

  Scenario: service health returns UP
    Given url baseUrl
    And path 'actuator/health'
    When method get
    Then status 200
    And match response contains { status: 'UP' }
