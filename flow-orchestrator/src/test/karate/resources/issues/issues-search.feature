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

  Scenario: search without audit omits changeSets on returned items
    * def suffix = java.lang.System.currentTimeMillis()
    * def uniqueLabel = 'karate-no-audit-' + suffix
    * def title = 'Karate no audit seed ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)', description: 'Seed issue for non-audit search smoke test', labels: ['#(uniqueLabel)'] }
    When method post
    Then status 201
    And match response.issueId == '#number'
    * def createdIssueId = response.issueId

    Given path 'api/issues/search'
    And request { filters: { labels: ['#(uniqueLabel)'] } }
    When method post
    Then status 200
    * def matched = karate.filter(response.items, function(x){ return x.issueId == createdIssueId })[0]
    And match matched.issueId == createdIssueId
    * eval if (matched.changeSets !== undefined) karate.fail('changeSets should be omitted when audit is absent')

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

  Scenario: search with label audit includes changeSets arrays on returned items
    * def suffix = java.lang.System.currentTimeMillis()
    * def uniqueLabel = 'karate-audit-' + suffix
    * def title = 'Karate audit seed ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)', description: 'Seed issue for audit search smoke test', labels: ['#(uniqueLabel)'] }
    When method post
    Then status 201
    And match response.issueId == '#number'
    * def createdIssueId = response.issueId

    Given path 'api/issues/search'
    And request { filters: { labels: ['#(uniqueLabel)'], audit: ['label'] } }
    When method post
    Then status 200
    And match response.items[*].issueId contains createdIssueId
    * def matched = karate.filter(response.items, function(x){ return x.issueId == createdIssueId })[0]
    And match matched.issueId == createdIssueId
    And match matched.changeSets == '#[]'
    And match matched.changeSets[0].change.field == 'label'
    And match matched.changeSets[0].changeType == 'add'

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204

  Scenario: search rejects unsupported audit values
    Given path 'api/issues/search'
    And request { filters: { audit: ['milestone'] } }
    When method post
    Then status 400
    And match response.code == 'VALIDATION_ERROR'

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
