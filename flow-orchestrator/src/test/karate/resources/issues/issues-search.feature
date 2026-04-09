@smoke
Feature: Issues search API

  Background:
    * url baseUrl
    * header Content-Type = 'application/json'

  Scenario: default search without body returns paged items
    Given path 'api/issues/search'
    And request {}
    When method post
    Then status 200
    And match response.items == '#[]'
    And match response.count == '#number'
    And match response.page == '#number'

  Scenario: filtered search by state returns opened issues only
    Given path 'api/issues/search'
    And request { filters: { state: 'opened' } }
    When method post
    Then status 200
    And match response.items == '#[]'
    And match each response.items contains { state: 'opened', issueId: '#number' }

  Scenario: search with pagination echoes the requested page
    Given path 'api/issues/search'
    And request { pagination: { page: 2, perPage: 5 } }
    When method post
    Then status 200
    And match response.page == 2
    And match response.count == '#number'

  Scenario: search results expose issueId as number
    * def suffix = java.lang.System.currentTimeMillis()
    * def uniqueLabel = 'karate-search-' + suffix
    * def title = 'Karate search seed ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)', description: 'Seed issue for search smoke test', labels: ['#(uniqueLabel)'] }
    When method post
    Then status 201
    And match response.issueId == '#number'
    * def createdIssueId = response.issueId

    Given path 'api/issues/search'
    And request { filters: { labels: ['#(uniqueLabel)'] } }
    When method post
    Then status 200
    And match response.items[*].issueId contains createdIssueId
    And match each response.items contains { issueId: '#number' }

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204
