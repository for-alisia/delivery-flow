@smoke
Feature: Milestones search API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: default search without body returns milestone array
    Given path 'api/milestones/search'
    And request {}
    When method post
    Then status 200
    And match response.milestones == '#[]'
    * def firstMilestone = response.milestones.length > 0 ? response.milestones[0] : null
    * if (firstMilestone != null) karate.match(firstMilestone, { id: '#number', milestoneId: '#number', title: '#string', state: '#string', description: '##string', startDate: '##string', dueDate: '##string' })

  Scenario: search rejects unsupported state values
    Given path 'api/milestones/search'
    And request { filters: { state: 'opened' } }
    When method post
    Then status 400
    And match response.code == 'VALIDATION_ERROR'

  Scenario: search rejects duplicate milestone ids
    Given path 'api/milestones/search'
    And request { filters: { milestoneIds: [3, 3] } }
    When method post
    Then status 400
    And match response.code == 'VALIDATION_ERROR'