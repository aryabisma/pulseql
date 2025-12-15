# GitHub Copilot Agent Instructions

This folder contains custom instruction files for GitHub Copilot agents working on the PulseQL-Pulsar Integration project.

## Agent Roster

| Agent File | Role | Primary Responsibilities |
|------------|------|-------------------------|
| `project_manager.md` | Project Manager | Sprint management, coordination, blockers |
| `senior_principal_architect.md` | Senior Principal Architect | System design, technical decisions |
| `senior_developer_1.md` | Senior Developer (Frontend Lead) | Frontend architecture, React/TypeScript |
| `senior_developer_2.md` | Senior Developer (Backend Lead) | Backend services, Java/GraphQL |
| `senior_developer_3.md` | Senior Developer (Integration) | SSO/RBAC, API integration |
| `senior_developer_4.md` | Senior Developer (UI/UX) | UI components, theming |
| `senior_developer_5.md` | Senior Developer (Performance) | Optimization, caching |
| `senior_test_engineer_1.md` | Senior Test Engineer (QA Lead) | Test strategy, automation |
| `senior_test_engineer_2.md` | Senior Test Engineer (Security) | Security testing, penetration |
| `senior_devops_engineer_1.md` | Senior DevOps Engineer (CI/CD) | Pipelines, deployment |
| `senior_devops_engineer_2.md` | Senior DevOps Engineer (Infra) | Infrastructure, monitoring |

## Usage

Each agent file provides:
1. Role context and responsibilities
2. Technical focus areas
3. Reference documentation
4. Decision-making guidelines
5. Communication protocols

## Global Instructions

All agents MUST follow the rules in `/copilot-instructions.md`:
- No shortcut solutions
- No sleep timers for waiting
- Clean code practices
- 360° review before completion
- Keep documentation updated

## Sprint Work

Current sprint work items are distributed in `/docs/pulsar_work_prompts/`:
- `senior_developer_1-deep-linking-api.md` - Deep Linking API
- `senior_developer_2-activity-tracking-backend.md` - Activity Tracking Backend
- `senior_developer_2-query-sharing-infrastructure.md` - Query Sharing Infrastructure
- `senior_devops_engineer_1-integration-testing-deployment.md` - Integration Testing & Deployment
