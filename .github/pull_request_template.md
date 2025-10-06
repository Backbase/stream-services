## Description

A few sentences describing the overall goals of the pull request's commits.

## Checklist

<!--
  Please review the requirements for each checkbox, and check them
  off (change "[ ]" to "[x]") as you verify that they are complete.
  
  Add N/A to the task if they are not relevant to the current PR(validation will be skipped). 
  e.g. [ ] My changes are adequately tested ~ N/A
-->

 - [ ] I made sure, I read [CONTRIBUTING.md](CONTRIBUTING.md) to put right branch prefix as per my need.
 - [ ] I made sure to update [CHANGELOG.md](CHANGELOG.md).
 - [ ] I made sure to update [Stream Wiki](https://github.com/Backbase/stream-services/wiki)(only valid in case of new stream module or architecture changes).
 - [ ] My changes are adequately tested.
 - [ ] I made sure all the SonarCloud Quality Gate are passed.
 - [ ] I have read and adhered to [CODING_RULES_COPILOT.md](../CODING_RULES_COPILOT.md); any intentional deviations are documented under a 'Rule Deviations' section below.
 - [ ] Changes respect module boundaries (no leaking core internals into model modules, no edits to generated sources).
 - [ ] Logging follows placeholder style and avoids sensitive data.
 - [ ] Public APIs remain backward compatible or CHANGELOG notes a breaking change.
 - [ ] Added/updated tests cover new logic branches and edge cases.
 - [ ] No unmanaged dependency versions introduced (all leverage BOM / parent) or justification provided.
 - [ ] Idempotency preserved for ingestion / saga operations (where applicable).

### Rule Deviations (if any)

