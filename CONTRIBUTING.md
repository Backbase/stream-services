## Contributing

You are welcome to provide bug fixes and new features in the form of pull requests. If you'd like to contribute, please be mindful of the following guidelines:

- All changes should be properly tested for common scenarios (i.e. if changing Legal Entity SAGA, test that your change doesn't affect in non-intended ways to LE ingestion and update).
- Try to avoid reformat of files that change the indentation, tabs to spaces etc., as this makes reviewing diffs much more difficult.
- Please make one change/feature per pull request.
- Use descriptive PR description and commit messages.
- Together with your changes, submit updated [CHANGELOG.md](CHANGELOG.md) in your PR using the next desired version as reference.
- After your pull request gets approved and integrated, then **GitHub actions will bump** the `MINOR` version and deploy it to Backbase maven repository. *(e.g. 2.45.0 -> 2.46.0)*
    * For small fixes and patches utilize the `hotfix/` branch prefix, so once it is integrated the pipelines will automatically bump the `PATCH` version instead of the `MINOR`. *(e.g. 2.46.0 -> 2.46.1)*

### Branching Strategy Flow
![Branching Strategy](docs/branching_strategy.jpg)
