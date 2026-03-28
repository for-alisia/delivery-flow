# GitLab REST API Review

Reviewed on: 2026-03-27

## Scope

This document is a working summary of the official GitLab REST API resource index and core REST API guidance.

It is intentionally structured for this project:

- short operational notes first
- then a resource inventory by context
- every resource row includes a `Used in FLOW` column

Important:

- this is a resource-level inventory, not a full per-method inventory
- endpoint patterns below are the published resource paths from the GitLab docs
- detailed request/response fields, filters, and error cases still belong in the official resource pages
- when a GitLab Feign endpoint is added to FLOW, update the matching `Used in FLOW` cell from `NO` to `TRUE`
- if a new GitLab resource family is introduced in code and is missing here, add it in the same change

## Official Sources

- Resource index: https://docs.gitlab.com/api/api_resources/
- REST API overview: https://docs.gitlab.com/api/rest/
- REST API authentication: https://docs.gitlab.com/api/rest/authentication/
- Dockerfile templates API: https://docs.gitlab.com/api/templates/dockerfiles/
- .gitignore API: https://docs.gitlab.com/api/templates/gitignores/
- Licenses API: https://docs.gitlab.com/api/templates/licenses/
- CI/CD templates API: https://docs.gitlab.com/ee/api/templates/gitlab_ci_ymls.html

## Important Short Notes

- Base path: GitLab REST endpoints live under `/api/v4`.
- Protocol and format: the API uses standard HTTP methods and typically returns JSON.
- Authentication: many endpoints require authentication; common patterns are `PRIVATE-TOKEN` and `Authorization: Bearer`.
- Pagination: list endpoints usually support `page` and `per_page`; keyset pagination exists only for selected resources.
- URL encoding: project paths, branch names, tag names, and file paths that contain `/` must be URL-encoded.
- Resource organization: the official index groups resources into `Project`, `Group`, `Standalone`, and `Template` contexts.
- Cross-context resources: several resource families exist in more than one context, for example issues, merge requests, search, events, notes, and notification settings.
- Privileged resources: some standalone APIs are clearly admin-oriented, for example parts of `/admin/...`, token inspection, usage data, and instance settings.
- FLOW note: resource rows should be updated from `NO` to `TRUE` as integrations become active. The project-scoped `Issues` resource is currently marked `TRUE`.
- Maintenance rule: this file should stay in sync with `flow-orchestrator` GitLab Feign integrations.

## Recommended Usage Notes For FLOW

- Start integration from resource families that align with orchestration use cases, not from raw GitLab endpoint sprawl.
- Prefer project-scoped APIs first unless the orchestration case truly needs group-wide or instance-wide visibility.
- Keep GitLab-specific filtering, pagination, auth headers, and endpoint quirks inside the integration layer.
- Map GitLab responses into internal models before domain logic uses them.

## Project Resources

| Resource | Context | Endpoint pattern(s) | Characteristics | Used in FLOW |
|---|---|---|---|---|
| Access requests | Project | `/projects/:id/access_requests` | Also available for groups | NO |
| Access tokens | Project | `/projects/:id/access_tokens` | Also available for groups | NO |
| Agents | Project | `/projects/:id/cluster_agents` | Cluster agent management | NO |
| Branches | Project | `/projects/:id/repository/branches`, `/projects/:id/repository/merged_branches` | Repository branch operations | NO |
| Commits | Project | `/projects/:id/repository/commits`, `/projects/:id/statuses` | Commit data and commit statuses | NO |
| Container registry | Project | `/projects/:id/registry/repositories` | Registry repository access | NO |
| Container repository protection rules | Project | `/projects/:id/registry/protection/repository/rules` | Registry protection rules | NO |
| Container registry protection tag rules | Project | `/projects/:id/registry/protection/tag/rules` | Tag protection for registry | NO |
| Custom attributes | Project | `/projects/:id/custom_attributes` | Also available for groups and users | NO |
| Composer distributions | Project | `/projects/:id/packages/composer` | Also available for groups | NO |
| Conan v1 distributions | Project | `/projects/:id/packages/conan` | Also available standalone | NO |
| Conan v2 distributions | Project | `/projects/:id/packages/conan` | Also available standalone | NO |
| Debian distributions | Project | `/projects/:id/debian_distributions` | Also available for groups | NO |
| Debian packages | Project | `/projects/:id/packages/debian` | Also available for groups | NO |
| Dependencies | Project | `/projects/:id/dependencies` | Dependency inventory | NO |
| Deploy keys | Project | `/projects/:id/deploy_keys` | Also available standalone | NO |
| Deploy tokens | Project | `/projects/:id/deploy_tokens` | Also available for groups and standalone | NO |
| Deployments | Project | `/projects/:id/deployments` | Deployment records | NO |
| Discussions | Project | `/projects/:id/issues/.../discussions`, `/projects/:id/snippets/.../discussions`, `/projects/:id/merge_requests/.../discussions`, `/projects/:id/commits/.../discussions` | Threaded comments; also available for groups | NO |
| Draft Notes | Project | `/projects/:id/merge_requests/.../draft_notes` | MR draft comments | NO |
| Emoji reactions | Project | `/projects/:id/issues/.../award_emoji`, `/projects/:id/merge_requests/.../award_emoji`, `/projects/:id/snippets/.../award_emoji` | Award emoji endpoints | NO |
| Environments | Project | `/projects/:id/environments` | Environment management | NO |
| Error Tracking | Project | `/projects/:id/error_tracking/settings` | Error tracking settings | NO |
| Events | Project | `/projects/:id/events` | Also available for users and standalone | NO |
| External status checks | Project | `/projects/:id/external_status_checks` | External MR status integrations | NO |
| Feature flag User Lists | Project | `/projects/:id/feature_flags_user_lists` | Feature flag audience lists | NO |
| Feature flags | Project | `/projects/:id/feature_flags` | Project feature flags | NO |
| Freeze Periods | Project | `/projects/:id/freeze_periods` | Deployment freeze windows | NO |
| Go Proxy | Project | `/projects/:id/packages/go` | Go package proxy | NO |
| Helm repository | Project | `/projects/:id/packages/helm_repository` | Helm package access | NO |
| Integrations | Project | `/projects/:id/integrations` | Formerly called services | NO |
| Invitations | Project | `/projects/:id/invitations` | Also available for groups | NO |
| Issue boards | Project | `/projects/:id/boards` | Board management | NO |
| Issue links | Project | `/projects/:id/issues/.../links` | Issue-to-issue relations | NO |
| Issues Statistics | Project | `/projects/:id/issues_statistics` | Also available for groups and standalone | NO |
| Issues | Project | `/projects/:id/issues` | Also available for groups and standalone | TRUE |
| Iterations | Project | `/projects/:id/iterations` | Also available for groups | NO |
| Project CI/CD job token scope | Project | `/projects/:id/job_token_scope` | Job token access scope | NO |
| Jobs | Project | `/projects/:id/jobs`, `/projects/:id/pipelines/.../jobs` | Project and pipeline jobs | NO |
| Jobs Artifacts | Project | `/projects/:id/jobs/:job_id/artifacts` | Artifact download/access | NO |
| Labels | Project | `/projects/:id/labels` | Project labels | NO |
| Maven repository | Project | `/projects/:id/packages/maven` | Also available for groups and standalone | NO |
| Members | Project | `/projects/:id/members` | Also available for groups | NO |
| Merge request approvals | Project | `/projects/:id/approvals`, `/projects/:id/merge_requests/.../approvals` | Approval policies and MR approvals | NO |
| Merge requests | Project | `/projects/:id/merge_requests` | Also available for groups and standalone | NO |
| Merge trains | Project | `/projects/:id/merge_trains` | Merge train operations | NO |
| Metadata | Project | `/metadata` | Global metadata endpoint listed in project section | NO |
| Model registry | Project | `/projects/:id/packages/ml_models/` | ML model registry | NO |
| Notes | Project | `/projects/:id/issues/.../notes`, `/projects/:id/snippets/.../notes`, `/projects/:id/merge_requests/.../notes` | Comments; also available for groups | NO |
| Notification settings | Project | `/projects/:id/notification_settings` | Also available for groups and standalone | NO |
| NPM repository | Project | `/projects/:id/packages/npm` | NPM package access | NO |
| NuGet packages | Project | `/projects/:id/packages/nuget` | Also available for groups | NO |
| Packages | Project | `/projects/:id/packages` | Generic package registry API | NO |
| Pages domains | Project | `/projects/:id/pages/domains` | Also available standalone | NO |
| Pages settings | Project | `/projects/:id/pages` | GitLab Pages settings | NO |
| Pipeline schedules | Project | `/projects/:id/pipeline_schedules` | Scheduled pipelines | NO |
| Pipeline triggers | Project | `/projects/:id/triggers` | Trigger token operations | NO |
| Pipelines | Project | `/projects/:id/pipelines` | Pipeline listing and control | NO |
| Project badges | Project | `/projects/:id/badges` | Badge definitions | NO |
| Project clusters | Project | `/projects/:id/clusters` | Project cluster integration | NO |
| Project import/export | Project | `/projects/:id/export`, `/projects/import`, `/projects/:id/import` | Import and export workflow | NO |
| Project milestones | Project | `/projects/:id/milestones` | Project milestones | NO |
| Project snippets | Project | `/projects/:id/snippets` | Project-scoped code snippets | NO |
| Project templates | Project | `/projects/:id/templates` | Project template access | NO |
| Project vulnerabilities | Project | `/projects/:id/vulnerabilities` | Vulnerability inventory | NO |
| Project wikis | Project | `/projects/:id/wikis` | Project wiki API | NO |
| Project-level variables | Project | `/projects/:id/variables` | CI/CD variables | NO |
| Projects | Project | `/projects`, `/projects/:id/hooks` | Includes project CRUD and webhooks; also available for users | NO |
| Protected branches | Project | `/projects/:id/protected_branches` | Branch protection rules | NO |
| Protected container registry | Project | `/projects/:id/registry/protection/rules` | Registry protection umbrella rules | NO |
| Protected environments | Project | `/projects/:id/protected_environments` | Protected deployment environments | NO |
| Protected packages | Project | `/projects/:id/packages/protection/rules` | Package protection rules | NO |
| Protected tags | Project | `/projects/:id/protected_tags` | Tag protection rules | NO |
| PyPI packages | Project | `/projects/:id/packages/pypi` | Also available for groups | NO |
| Release links | Project | `/projects/:id/releases/.../assets/links` | Release asset links | NO |
| Releases | Project | `/projects/:id/releases` | Project releases | NO |
| Remote mirrors | Project | `/projects/:id/remote_mirrors` | Repository mirroring | NO |
| Repositories | Project | `/projects/:id/repository` | Generic repository operations | NO |
| Repository files | Project | `/projects/:id/repository/files` | File-level repository access | NO |
| Repository submodules | Project | `/projects/:id/repository/submodules` | Submodule operations | NO |
| Resource label events | Project | `/projects/:id/issues/.../resource_label_events`, `/projects/:id/merge_requests/.../resource_label_events` | Label history; also available for groups | NO |
| Ruby gems | Project | `/projects/:id/packages/rubygems` | RubyGems package registry | NO |
| Runners | Project | `/projects/:id/runners` | Also available standalone | NO |
| Search | Project | `/projects/:id/search` | Also available for groups and standalone | NO |
| Tags | Project | `/projects/:id/repository/tags` | Repository tags | NO |
| Terraform modules | Project | `/projects/:id/packages/terraform/modules` | Also available standalone | NO |
| Validate `.gitlab-ci.yml` file | Project | `/projects/:id/ci/lint` | CI linting endpoint | NO |
| Vulnerabilities | Project | `/vulnerabilities/:id` | Global vulnerability endpoint listed in project section | NO |
| Vulnerability exports | Project | `/projects/:id/vulnerability_exports` | Export operations | NO |
| Vulnerability findings | Project | `/projects/:id/vulnerability_findings` | Findings listing | NO |

## Group Resources

| Resource | Context | Endpoint pattern(s) | Characteristics | Used in FLOW |
|---|---|---|---|---|
| Access requests | Group | `/groups/:id/access_requests/` | Also available for projects | NO |
| Access tokens | Group | `/groups/:id/access_tokens` | Also available for projects | NO |
| Custom attributes | Group | `/groups/:id/custom_attributes` | Also available for projects and users | NO |
| Debian distributions | Group | `/groups/:id/-/packages/debian` | Also available for projects | NO |
| Deploy tokens | Group | `/groups/:id/deploy_tokens` | Also available for projects and standalone | NO |
| Discussions | Group | `/groups/:id/epics/.../discussions` | Comments and threads; also available for projects | NO |
| Epic issues | Group | `/groups/:id/epics/.../issues` | Epic-to-issue relationships | NO |
| Epic links | Group | `/groups/:id/epics/.../epics` | Epic linking | NO |
| Epics | Group | `/groups/:id/epics` | Group-level epics | NO |
| Groups | Group | `/groups`, `/groups/.../subgroups` | Group and subgroup management | NO |
| Group badges | Group | `/groups/:id/badges` | Badge definitions | NO |
| Group issue boards | Group | `/groups/:id/boards` | Board management | NO |
| Group iterations | Group | `/groups/:id/iterations` | Also available for projects | NO |
| Group labels | Group | `/groups/:id/labels` | Group labels | NO |
| Group-level variables | Group | `/groups/:id/variables` | CI/CD variables | NO |
| Group milestones | Group | `/groups/:id/milestones` | Group milestones | NO |
| Group releases | Group | `/groups/:id/releases` | Group releases | NO |
| Group SSH certificates | Group | `/groups/:id/ssh_certificates` | Group certificate management | NO |
| Group wikis | Group | `/groups/:id/wikis` | Group wiki API | NO |
| Invitations | Group | `/groups/:id/invitations` | Also available for projects | NO |
| Issues | Group | `/groups/:id/issues` | Also available for projects and standalone | NO |
| Issues Statistics | Group | `/groups/:id/issues_statistics` | Also available for projects and standalone | NO |
| Linked epics | Group | `/groups/:id/epics/.../related_epics` | Epic relationship management | NO |
| Member Roles | Group | `/groups/:id/member_roles` | Custom/member role management | NO |
| Members | Group | `/groups/:id/members` | Also available for projects | NO |
| Merge requests | Group | `/groups/:id/merge_requests` | Also available for projects and standalone | NO |
| Notes | Group | `/groups/:id/epics/.../notes` | Comments; also available for projects | NO |
| Notification settings | Group | `/groups/:id/notification_settings` | Also available for projects and standalone | NO |
| Resource label events | Group | `/groups/:id/epics/.../resource_label_events` | Label event history; also available for projects | NO |
| Search | Group | `/groups/:id/search` | Also available for projects and standalone | NO |

## Standalone Resources

| Resource | Context | Endpoint pattern(s) | Characteristics | Used in FLOW |
|---|---|---|---|---|
| Appearance | Standalone | `/application/appearance` | Instance appearance settings | NO |
| Applications | Standalone | `/applications` | OAuth applications | NO |
| Audit events | Standalone | `/audit_events` | Audit trail access | NO |
| Avatar | Standalone | `/avatar` | Avatar lookup | NO |
| Broadcast messages | Standalone | `/broadcast_messages` | Instance broadcast messages | NO |
| Code snippets | Standalone | `/snippets` | Global snippet API | NO |
| Code Suggestions | Standalone | `/code_suggestions` | AI/code suggestion operations | NO |
| Custom attributes | Standalone | `/users/:id/custom_attributes` | Also available for groups and projects | NO |
| Dependency list exports | Standalone | `/pipelines/:id/dependency_list_exports`, `/projects/:id/dependency_list_exports`, `/groups/:id/dependency_list_exports`, `/security/dependency_list_exports/:id`, `/security/dependency_list_exports/:id/download` | Security export workflow | NO |
| Deploy keys | Standalone | `/deploy_keys` | Also available for projects | NO |
| Deploy tokens | Standalone | `/deploy_tokens` | Also available for projects and groups | NO |
| GitLab Duo Agent Platform flows | Standalone | `/ai/duo_workflows` | AI workflow endpoints | NO |
| Events | Standalone | `/events`, `/users/:id/events` | Also available for projects | NO |
| Feature flags | Standalone | `/features` | Feature flag administration | NO |
| Geo Nodes | Standalone | `/geo_nodes` | Geo replication nodes | NO |
| GLQL | Standalone | `/glql` | GitLab query language endpoint | NO |
| Group Activity Analytics | Standalone | `/analytics/group_activity/{issues_count}` | Analytics endpoint | NO |
| Group repository storage moves | Standalone | `/group_repository_storage_moves` | Storage migration operations | NO |
| Import repository from GitHub | Standalone | `/import/github` | GitHub import flow | NO |
| Import repository from Bitbucket Server | Standalone | `/import/bitbucket_server` | Bitbucket Server import flow | NO |
| Instance clusters | Standalone | `/admin/clusters` | Admin-oriented | NO |
| Instance-level CI/CD variables | Standalone | `/admin/ci/variables` | Admin-oriented instance variables | NO |
| Issues Statistics | Standalone | `/issues_statistics` | Also available for groups and projects | NO |
| Issues | Standalone | `/issues` | Also available for groups and projects | NO |
| Jobs | Standalone | `/job` | Standalone job endpoint family | NO |
| Keys | Standalone | `/keys` | SSH key lookup | NO |
| License | Standalone | `/license` | Instance license info | NO |
| Markdown | Standalone | `/markdown` | Markdown rendering | NO |
| Merge requests | Standalone | `/merge_requests` | Also available for groups and projects | NO |
| Namespaces | Standalone | `/namespaces` | Namespace listing and lookup | NO |
| Notification settings | Standalone | `/notification_settings` | Also available for groups and projects | NO |
| Compliance and Policy settings | Standalone | `/admin/security/compliance_policy_settings` | Admin-oriented security settings | NO |
| Pages domains | Standalone | `/pages/domains` | Also available for projects | NO |
| Personal access tokens | Standalone | `/personal_access_tokens` | Token inventory/management | NO |
| Plan limits | Standalone | `/application/plan_limits` | Plan quota settings | NO |
| Project repository storage moves | Standalone | `/project_repository_storage_moves` | Storage migration operations | NO |
| Projects | Standalone | `/users/:id/projects` | User-related project listing; also available as project resource | NO |
| Runners | Standalone | `/runners` | Also available for projects | NO |
| Search | Standalone | `/search` | Also available for groups and projects | NO |
| Service Data | Standalone | `/usage_data` | Administrator-only usage data | NO |
| Settings | Standalone | `/application/settings` | Instance settings | NO |
| Sidekiq metrics | Standalone | `/sidekiq` | Background job metrics | NO |
| Sidekiq queues administration | Standalone | `/admin/sidekiq/queues/:queue_name` | Admin-oriented queue control | NO |
| Snippet repository storage moves | Standalone | `/snippet_repository_storage_moves` | Storage migration operations | NO |
| Statistics | Standalone | `/application/statistics` | Instance statistics | NO |
| Suggestions | Standalone | `/suggestions` | Suggestion endpoints | NO |
| System hooks | Standalone | `/hooks` | System-wide webhook management | NO |
| To-dos | Standalone | `/todos` | User to-do items | NO |
| Token information | Standalone | `/admin/token` | Admin-oriented token inspection | NO |
| Topics | Standalone | `/topics` | Topic/tag catalog | NO |
| Users | Standalone | `/users` | User management | NO |
| Web commits | Standalone | `/web_commits/public_key` | Web commit public key helper | NO |

## Template Resources

| Resource | Context | Endpoint pattern(s) | Characteristics | Used in FLOW |
|---|---|---|---|---|
| Dockerfile templates | Template | `/templates/dockerfiles` | Template catalog; guest users cannot access | NO |
| `.gitignore` templates | Template | `/templates/gitignores` | Template catalog; guest users cannot access | NO |
| GitLab CI/CD YAML templates | Template | `/templates/gitlab_ci_ymls` | Template catalog; guest users cannot access | NO |
| Open source license templates | Template | `/templates/licenses` | Template catalog; supports license discovery | NO |

## Suggested FLOW Starting Points

If FLOW continues toward orchestration around project delivery and GitLab Flow concepts, the most likely early candidates are:

- `Projects`
- `Issues`
- `Merge requests`
- `Notes`
- `Discussions`
- `Labels`
- `Members`
- `Milestones`
- `Iterations`
- `Events`
- `Pipeline schedules`
- `Pipelines`
- `Jobs`
- `Project-level variables`
- `Search`

Those are only likely candidates, not approved usage. The `Used in FLOW` column remains `NO` everywhere until the integration design is decided.
