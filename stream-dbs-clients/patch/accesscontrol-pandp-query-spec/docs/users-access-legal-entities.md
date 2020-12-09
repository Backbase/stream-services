# User Access Legal Entities

Request method POST for checking to which legal entities the user has access to.

**Warning:**
 * Calling this endpoint will bypass the validation of user permissions
   of the user performing the action.
 * Calling this endpoint will bypass the validation of users
   (existence of user and user belonging in legal entity)
   upon users on which the action is performed

**Recommendation: Use the corresponding endpoint on presentation service or use Auth Security library.**
