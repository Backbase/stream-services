# User Access GET

Request method GET for fetching all arrangements with privileges
by functionName and resourceName as required,
and userId, serviceAgreementId, legalEntityId, arrangementId and privilege as optional.

**Warning:**
 * Calling this endpoint will bypass the validation of user permissions
   of the user performing the action.
 * Calling this endpoint will bypass the validation of users
   (existence of user and user belonging in legal entity)
   upon users on which the action is performed

**Recommendation: Use the corresponding endpoint on presentation service or use Auth Security library.**
