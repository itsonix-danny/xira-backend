# Issue Relations Implementation Summary

## Overview
Successfully implemented functionality to connect tickets (issues) with each other using three types of relations:
- **BLOCKS** / **BLOCKED_BY**: Directional blocking relationships
- **RELATES_TO**: Bidirectional relationship

## Implementation Details

### 1. Database Entity Changes

#### Issue.java
- Added `createdAt` field with `@CreatedDate` annotation for tracking issue creation time
- Added `issueRelations` collection with cascade delete
- Added `relatedIssueRelations` collection for bidirectional cascade delete
- Updated `@ToString` to exclude both relation collections

#### IssueRelation.java (Already existed)
- Composite primary key using `IssueRelationId`
- Contains: `issueId`, `relatedIssueId`, and `relationType` (enum)
- Bidirectional ManyToOne relationships with Issue entity

#### IssueRelationType.java (Already existed)
- Enum with values: `BLOCKS`, `BLOCKED_BY`, `RELATES_TO`

### 2. Repository Layer

#### IssueRelationRepository.java (Already existed)
- Methods for CRUD operations on issue relations
- Custom queries for finding and deleting relations

### 3. Service Layer

#### IssueService.java
- Added `XiraUserRepository` dependency (was missing)
- **`addIssueRelation()`**: Creates bidirectional relations
  - BLOCKS → automatically creates inverse BLOCKED_BY relation
  - BLOCKED_BY → automatically creates inverse BLOCKS relation
  - RELATES_TO → creates symmetric bidirectional relation
  - Validates that issues don't relate to themselves
  - Checks for duplicate relations
  
- **`removeIssueRelation()`**: Removes both directions of a relation
  - Deletes primary relation
  - Deletes inverse relation automatically

### 4. Controller Layer

#### IssueController.java
- **POST /projects/{key}/issues/{issueKey}/relations**
  - Adds a new issue relation
  - Returns 201 Created with location header
  - Requires project membership
  
- **DELETE /projects/{key}/issues/{issueKey}/relations/{relatedIssueKey}**
  - Removes an issue relation
  - Returns 204 No Content
  - Requires project membership

### 5. Mapper Layer

#### IssueMapper.java
- Fixed duplicate `.toList()` bug
- Added proper mapping for `relations` field in `IssueDetailsResponse`
- Added proper mapping for `comments` field in `IssueDetailsResponse`

#### IssueRelationMapper.java (Already existed)
- Maps `IssueRelation` entity to `IssueRelationResponse` DTO

### 6. API Specification (openapi.yml)
Already updated by user with:
- `AddIssueRelationRequest` schema
- `IssueRelationResponse` schema  
- Endpoint definitions with proper responses

## Testing

### Unit Tests (IssueServiceTest.java)
Added 13 comprehensive test cases:
- `addIssueRelation_Creates_Blocks_Relation_When_Valid_Request`
- `addIssueRelation_Creates_Blocked_By_Relation_When_Valid_Request`
- `addIssueRelation_Creates_Relates_To_Relation_When_Valid_Request`
- `addIssueRelation_Throws_Exception_When_Issue_Not_Found`
- `addIssueRelation_Throws_Exception_When_Related_Issue_Not_Found`
- `addIssueRelation_Throws_Exception_When_Issue_Relates_To_Itself`
- `addIssueRelation_Throws_Exception_When_Relation_Already_Exists`
- `removeIssueRelation_Deletes_Relation_When_Valid_Request`
- `removeIssueRelation_Throws_Exception_When_Issue_Not_Found`
- `removeIssueRelation_Throws_Exception_When_Related_Issue_Not_Found`
- `removeIssueRelation_Throws_Exception_When_Relation_Not_Found`

### Integration Tests (IssueControllerIntegrationTest.java)
Added 10 comprehensive integration test cases:
- `givenValidRequest_whenAddIssueRelation_thenReturns201`
- `givenBlocksRelation_whenAddIssueRelation_thenCreatesInverseRelation`
- `givenRelatesToRelation_whenAddIssueRelation_thenCreatesBidirectionalRelation`
- `givenNonMember_whenAddIssueRelation_thenReturns403`
- `givenInvalidRelatedIssue_whenAddIssueRelation_thenReturns404`
- `givenExistingRelation_whenAddIssueRelation_thenReturns409`
- `givenValidRelation_whenRemoveIssueRelation_thenReturns204`
- `givenValidRelation_whenRemoveIssueRelation_thenRemovesInverseRelation`
- `givenNonMember_whenRemoveIssueRelation_thenReturns403`
- `givenNonExistingRelation_whenRemoveIssueRelation_thenReturns404`

## Test Results
✅ **All 307 tests pass successfully**
- 48 unit tests in IssueServiceTest
- 52 integration tests in IssueControllerIntegrationTest
- All other existing tests continue to pass

## Key Features

### Bidirectional Relationship Management
- **BLOCKS/BLOCKED_BY**: When you create a "A blocks B" relation, the system automatically creates "B is blocked by A"
- **RELATES_TO**: Symmetric relationship - "A relates to B" automatically creates "B relates to A"
- Deletion removes both directions automatically

### Validation
- Issues cannot relate to themselves
- Duplicate relations are prevented
- Only project members can manage relations
- Validates that both issues exist in the same project

### Security
- All endpoints require authentication via `@PreAuthorize`
- Project membership verification using `@authService.isProjectMember(#key)`

## Database Schema
The implementation uses Hibernate's `ddl-auto: update` strategy, so schema changes are applied automatically on startup.

### Foreign Key Cascade
- Issue deletion cascades to related `IssueRelation` records via JPA cascade configuration
- Both `issueRelations` and `relatedIssueRelations` collections use `CascadeType.ALL` and `orphanRemoval = true`

## Usage Example

### Add a "blocks" relation:
```
POST /projects/XIRA/issues/XIRA-1/relations
{
  "relatedIssueKey": "XIRA-2",
  "relationType": "BLOCKS"
}
```
This automatically creates:
- XIRA-1 blocks XIRA-2
- XIRA-2 is blocked by XIRA-1

### Remove a relation:
```
DELETE /projects/XIRA/issues/XIRA-1/relations/XIRA-2
```
This removes both directions of the relation.

### View relations in issue details:
```
GET /projects/XIRA/issues/XIRA-1
```
Response includes:
```json
{
  "key": "XIRA-1",
  "relations": [
    {
      "relatedIssueKey": "XIRA-2",
      "relationType": "BLOCKS"
    }
  ]
}
```

## Notes
- The implementation follows the existing code patterns in the project
- Test naming follows the convention: `MethodName_Expected_Behavior_State_Under_Test`
- Uses FluentAssertions and NSubstitute as per project standards
- All code is properly commented and follows Java best practices

