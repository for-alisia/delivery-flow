@smoke
Feature: Issues update API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: update issue title description and labels
    * def suffix = java.lang.System.currentTimeMillis()
    * def createdTitle = 'Karate update source ' + suffix
    * def updatedTitle = 'Karate updated issue ' + suffix
    * def addedLabel = 'karate-update-' + suffix

    Given path 'api/issues'
    And request { title: '#(createdTitle)', description: 'Original description', labels: ['smoke'] }
    When method post
    Then status 201
    * def issueId = response.issueId

    Given path 'api/issues', issueId
    And request { title: '#(updatedTitle)', description: '', addLabels: ['#(addedLabel)'], removeLabels: ['smoke'] }
    When method patch
    Then status 200
    And match response.issueId == issueId
    And match response.title == updatedTitle
    And match response.description == ''
    And match response.labels contains only ['#(addedLabel)']

    Given path 'api/issues', issueId
    When method delete
    Then status 204

  Scenario: update validation error for empty patch body
    Given path 'api/issues', 1
    And request { }
    When method patch
    Then status 400
    And match response.code == 'VALIDATION_ERROR'
