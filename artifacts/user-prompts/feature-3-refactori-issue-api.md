Hi Team,

Add issueId field to the issue response. This maps to GitLab's iid (project-scoped issue number). The client-facing field name must be issueId, not iid. Both search and create responses must include it. Update all affected tests and smoke checks. This refactoring should affect both of our endpoints and all returned isses to our clients should have this field.