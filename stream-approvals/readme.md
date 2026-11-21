# Approvals Ingestion SAGA

This is not idempotent implementation!!!
 - due to lack of service-api
 - The request for improvement has being created

```
bootstrap:
  approvals:
    - name: 4 eye approval policy
        approvalTypes:
          - name: Supervisor
            description: Supervisor approval level
            rank: 1
          - name: HelpDesk
            description: Digital helpdesk and Operations User
            rank: 2
        policies:
          - name: 4 eye policy
            description: Policy that requires approval from supervisor
            logicalItems:
              - rank: 1
                items:
                  - approvalTypeName: Supervisor
                    numberOfApprovals: 1
              - rank: 2
                operator: OR
                items:
                  - approvalTypeName: HelpDesk
                    numberOfApprovals: 2
        policyAssignments:
          - externalServiceAgreementId: sa_coutts-bank
            policyAssignmentItems:
              - functions:
                  - Assign Permissions
                bounds:
                  - policyName: 4 eye policy
              - functions:
                  - Manage Data Groups
                bounds:
                  - policyName: 4 eye policy
            approvalTypeAssignments:
              - approvalTypeName: Supervisor
                jobProfileName: SUUS
              - approvalTypeName: HelpDesk
                jobProfileName: Digital helpdesk and Operations User
```

## Expression-based policies

A policy can define an `expression` instead of `items` and `logicalItems`. Those two describe an OR of AND groups:
`items` are combined with AND, and `logicalItems` combines its groups with OR and the `items` in each group with AND.
Use `expression` for sequential approvals with `then` (one group approves first, then the next), which `items` and
`logicalItems` can't describe, or to write nested expressions such as `(A or B) and C` directly. Policies with an
`expression` are created through the Approval Management Service API
(`POST /service-api/v1/approvals/management/policies`); all other policies keep using Service API v2.

An expression is a tree of nodes. Each node is either:

- an operator node: `operator` (`and`, `or` or `then`) and at least two `operands`;
- a unit node: `approvalTypeName` (or `approvalTypeId`) and `count`.

A node with both an `operator` and an approval type, or with neither, is rejected, as is a unit with `operands` or
without `count`. Approval-service validates the rest of the expression, for example that `then` is allowed for the
business function the policy is assigned to.

```
        policies:
          - name: Supervisor or HelpDesk, then Manager
            description: Supervisor or HelpDesk approves first, then Manager
            expression:
              operator: then
              operands:
                - operator: or
                  operands:
                    - approvalTypeName: Supervisor
                      count: 1
                    - approvalTypeName: HelpDesk
                      count: 1
                - approvalTypeName: Manager
                  count: 1
```

If a policy defines both an `expression` and `items` or `logicalItems`, the `expression` is used and the items are
ignored with a warning.