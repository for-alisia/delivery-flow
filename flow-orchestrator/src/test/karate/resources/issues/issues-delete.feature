@smoke
Feature: Issues delete API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: delete existing issue returns 204
    * def title = 'Karate delete issue ' + java.lang.System.currentTimeMillis()

    Given path 'api/issues'
    And request { title: '#(title)' }
    When method post
    Then status 201
    * def createdIssueId = response.issueId

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

  Scenario: delete non-existent issue returns 404
    Given path 'api/issues', 999999999
    When method delete
    Then status 404

  Scenario: delete validation error for zero issueId
    Given path 'api/issues', 0
    When method delete
    Then status 400
    And match response.code == 'VALIDATION_ERROR'

  Scenario: delete validation error for negative issueId
    Given path 'api/issues', -1
    When method delete
    Then status 400
    And match response.code == 'VALIDATION_ERROR'
