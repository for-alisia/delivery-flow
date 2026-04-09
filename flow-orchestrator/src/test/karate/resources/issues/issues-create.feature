@smoke
Feature: Issues create API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: create issue with full payload
    * def suffix = java.lang.System.currentTimeMillis()
    * def title = 'Karate create issue ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)', description: 'Created by Karate smoke', labels: ['karate', 'smoke'] }
    When method post
    Then status 201
    And match response contains { id: '#number', issueId: '#number', title: '#(title)', state: '#string', labels: '#[]' }
    And match response.description == 'Created by Karate smoke'
    And match response.labels contains 'karate'

    * def createdIssueId = response.issueId
    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

  Scenario: create issue with title only
    * def title = 'Karate title only issue ' + java.lang.System.currentTimeMillis()

    Given path 'api/issues'
    And request { title: '#(title)' }
    When method post
    Then status 201
    And match response.issueId == '#number'
    And match response.title == title
    And match response.labels == []

    * def createdIssueId = response.issueId
    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

  Scenario: create validation error for blank title
    Given path 'api/issues'
    And request { title: '   ' }
    When method post
    Then status 400
    And match response.code == 'VALIDATION_ERROR'

  Scenario: create validation error for missing body
    Given path 'api/issues'
    When method post
    Then status 400
    And match response.code == 'VALIDATION_ERROR'
