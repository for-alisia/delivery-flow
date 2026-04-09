@smoke
Feature: Issue lifecycle create search delete

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: create search delete and confirm already deleted
    * def suffix = java.lang.System.currentTimeMillis()
    * def lifecycleLabel = 'karate-lifecycle-' + suffix
    * def title = 'Karate lifecycle issue ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)', description: 'Auto-created by Karate lifecycle test', labels: ['#(lifecycleLabel)', 'smoke'] }
    When method post
    Then status 201
    And match response.issueId == '#number'
    * def createdIssueId = response.issueId

    Given path 'api/issues/search'
    And request { filters: { labels: ['#(lifecycleLabel)'] } }
    When method post
    Then status 200
    And match response.items[*].issueId contains createdIssueId

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 404
