# Pulsar-Side Work Prompts for Quick Wins Features

This directory contains detailed implementation prompts for 4 agents working in parallel on the Pulsar side of the Quick Wins integration.

## Overview

PulseQL has implemented 4 Quick Wins features that require corresponding backend support in Pulsar:

1. **Query History & Favorites** - Frontend complete, no Pulsar changes needed
2. **Deep Linking** - Requires Pulsar API to generate links
3. **Keyboard Shortcuts** - Frontend complete, no Pulsar changes needed  
4. **Activity Tracking** - Requires Pulsar backend to receive and process activity events

Additionally, we need query sharing infrastructure for future collaboration features.

## Agent Assignments

### Agent 1: Deep Linking API (`agent1-deep-linking-api.md`)

**Objective**: Implement backend API endpoints in Pulsar to generate deep links for PulseQL workspace navigation.

**Key Deliverables**:
- REST endpoint: `POST /api/pulseql/generate-link`
- DeepLinkService.java - Link generation logic
- DeepLinkButton.tsx - React component for Pulsar UI
- Integration with table context menus and schema browser
- Short URL service (optional)

**Estimated Effort**: 13-18 hours (1.5-2 days)

**Dependencies**: Spring Boot REST, Apache Pulsar client

**Priority**: HIGH - Required for deep linking to work

---

### Agent 2: Activity Tracking Backend (`agent2-activity-tracking-backend.md`)

**Objective**: Implement backend endpoints in Pulsar to receive and validate activity tracking events from PulseQL.

**Key Deliverables**:
- REST endpoint: `POST /api/sso/activity`
- SessionValidationService.java - Session validation logic
- IdleSessionManager.java - Idle session detection and cleanup
- Database schema for activity_events table
- Analytics dashboard for admins
- Audit logging

**Estimated Effort**: 26-34 hours (3-4 days)

**Dependencies**: Spring Boot, PostgreSQL/MySQL, Redis (optional), Spring Scheduler

**Priority**: HIGH - Required for activity tracking and session security

---

### Agent 3: Query Sharing Infrastructure (`agent3-query-sharing-infrastructure.md`)

**Objective**: Implement backend infrastructure in Pulsar to support query sharing, collaboration, and team knowledge sharing features.

**Key Deliverables**:
- Complete database schema (shared_queries, query_permissions, query_comments, etc.)
- SharedQueryService.java - Query management logic
- QueryPermissionService.java - Permission management
- REST API endpoints for CRUD operations
- WebSocket endpoint for real-time collaboration
- Notification service

**Estimated Effort**: 32-42 hours (4-5 days)

**Dependencies**: Spring Boot, Spring Data JPA, PostgreSQL, WebSocket, Email service, Redis

**Priority**: MEDIUM - For future Phase 2 collaboration features, not required for Quick Wins

---

### Agent 4: Integration Testing & Deployment (`agent4-integration-testing-deployment.md`)

**Objective**: Create comprehensive integration tests, deployment scripts, and configuration management for Quick Wins features.

**Key Deliverables**:
- End-to-end integration test suite (Selenium/Cypress)
- Performance test suite (JMeter/Gatling)
- Security test suite
- Deployment configuration (feature flags, migrations, nginx)
- Monitoring setup (Prometheus, Grafana)
- Deployment documentation and runbook

**Estimated Effort**: 40-51 hours (5-6 days)

**Dependencies**: Cypress/Selenium, TestContainers, JMeter/Gatling, Prometheus, Grafana, Flyway

**Priority**: HIGH - Required for production deployment

---

## Parallel Execution Strategy

All 4 agents can work in parallel with minimal dependencies:

```
Week 1 Timeline (Parallel Execution):

Day 1-2:
├── Agent 1: Deep Linking API Implementation
├── Agent 2: Activity Tracking Backend (start)
├── Agent 3: Query Sharing Infrastructure (start)
└── Agent 4: Test Framework Setup

Day 3-4:
├── Agent 1: ✅ Complete (integration testing)
├── Agent 2: Activity Tracking Backend (continue)
├── Agent 3: Query Sharing Infrastructure (continue)
└── Agent 4: Integration Tests (Quick Wins)

Day 5-6:
├── Agent 1: ✅ Complete
├── Agent 2: ✅ Complete
├── Agent 3: Query Sharing Infrastructure (continue)
└── Agent 4: Performance & Security Tests

Day 7+:
├── Agent 3: ✅ Complete (or defer to Phase 2)
└── Agent 4: ✅ Complete (deployment ready)
```

**Critical Path**: Agents 1, 2, 4 must complete for Quick Wins deployment  
**Optional**: Agent 3 can be deferred to Phase 2 if needed

## Prerequisites

Before starting, ensure:

1. **PulseQL Plugin Deployed**: Quick Wins features implemented in PulseQL
2. **Pulsar Dev Environment**: Spring Boot project setup and running
3. **Database Access**: PostgreSQL or MySQL for data storage
4. **Testing Environment**: Staging environment for integration tests
5. **Monitoring Tools**: Prometheus and Grafana configured

## Coordination

### Shared Resources

- Database: Coordinate schema changes via Flyway migrations
- API Endpoints: Document all new endpoints in Swagger
- Configuration: Use centralized application.properties

### Communication

- Daily sync: 15-min standup to discuss blockers
- Shared Slack channel: #pulseql-quick-wins
- Code reviews: All PRs reviewed by at least one other agent
- Integration points: Document interfaces between components

## Testing Strategy

Each agent is responsible for:
1. **Unit tests** for their components
2. **API tests** for their endpoints
3. **Integration tests** with PulseQL (coordinate with Agent 4)

Agent 4 creates:
1. **End-to-end tests** across all features
2. **Performance tests** for all new APIs
3. **Security tests** for authentication and permissions

## Deployment Plan

### Phase 1: Staging Deployment (Week 1)
- Deploy all Quick Wins backend components
- Run full integration test suite
- Performance and security validation

### Phase 2: Beta Testing (Week 2)
- Enable for internal users only
- Gather feedback and metrics
- Fix bugs and optimize

### Phase 3: Production Rollout (Week 3)
- Gradual rollout: 10% → 25% → 50% → 100%
- Monitor metrics at each stage
- Rollback plan ready if issues detected

## Success Criteria

### Agent 1 (Deep Linking)
- [x] Deep links generate successfully from Pulsar UI
- [x] Links open correct target in PulseQL
- [x] User permissions preserved
- [x] All tests pass

### Agent 2 (Activity Tracking)
- [x] Activity events received and stored
- [x] Session validation working
- [x] Idle sessions detected and terminated
- [x] Analytics dashboard functional

### Agent 3 (Query Sharing)
- [x] Queries can be created and shared
- [x] Permissions enforced correctly
- [x] Real-time collaboration working
- [x] Notifications sent successfully

### Agent 4 (Testing & Deployment)
- [x] All integration tests pass
- [x] Performance meets requirements
- [x] Security tests pass
- [x] Deployment automation working
- [x] Monitoring dashboards live

## Documentation

Each agent must provide:
1. **API Documentation**: Swagger/OpenAPI specs
2. **Implementation Notes**: Design decisions and gotchas
3. **Deployment Guide**: Configuration and deployment steps
4. **Troubleshooting Guide**: Common issues and solutions

## Support

For questions or issues:
1. Review the agent-specific prompt document
2. Check PulseQL Quick Wins implementation: `docs/19-quick-wins-implementation.md`
3. Post in #pulseql-quick-wins Slack channel
4. Escalate to tech lead if blocked

## Files in This Directory

- `agent1-deep-linking-api.md` - Deep linking API implementation
- `agent2-activity-tracking-backend.md` - Activity tracking backend
- `agent3-query-sharing-infrastructure.md` - Query sharing infrastructure  
- `agent4-integration-testing-deployment.md` - Testing and deployment
- `README.md` - This file

## Next Steps

1. **Review All Prompts**: Each agent reads their assigned prompt
2. **Environment Setup**: Ensure all prerequisites met
3. **Start Implementation**: Begin parallel development
4. **Daily Standups**: 15-min sync each morning
5. **Integration Testing**: Coordinate with Agent 4 for E2E tests
6. **Deployment**: Follow phased rollout plan

---

**Status**: 🚀 Ready to start parallel implementation  
**Timeline**: 1 week parallel execution (Agents 1,2,4 critical path)  
**Next Milestone**: Staging deployment with full Quick Wins features
