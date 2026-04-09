@smoke
Feature: Get single issue API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  @smoke
  Scenario: returns full issue detail for a valid issueId
    * def suffix = java.lang.System.currentTimeMillis()
    * def title = 'Karate get-single seed ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)', description: 'Seed for get-single smoke test' }
    When method post
    Then status 201
    And match response.issueId == '#number'
    * def createdIssueId = response.issueId

    Given path 'api/issues', createdIssueId
    When method get
    Then status 200
    And match response.issueId == createdIssueId
    And match response.title == title
    And match response.state == '#string'
    And match response.labels == '#[]'
    And match response.assignees == '#[]'
    And match response.changeSets == '#[]'
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

  @smoke
  Scenario: returns 400 for non-positive issueId zero
    Given path 'api/issues/0'
    When method get
    Then status 400
    And match response.code == 'VALIDATION_ERROR'

  @smoke
  Scenario: returns error response for non-existent issue
    Given path 'api/issues/999999999'
    When method get
    Then status 404
    And match response.code == 'RESOURCE_NOT_FOUND'
