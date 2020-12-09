# User Access Hierarchy

Request method POST for checking if a provided user has access to the provided list of resource ids.
Returns list of resource ids that the provided user has access to, a sub list of the provided ids.

**Warning:**
 * Calling this endpoint will bypass the validation of user permissions
   of the user performing the action.
 * Calling this endpoint will bypass the validation of users
   (existence of user and user belonging in legal entity)
   upon users on which the action is performed

**Recommendation: Use the corresponding endpoint on presentation service or use Auth Security library.**
