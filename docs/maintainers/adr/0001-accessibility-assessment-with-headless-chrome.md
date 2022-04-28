# Use accessibility-assessment with headless chrome

* Status: accepted
* Deciders: platui
* Date: 2022-04-28

Technical Story: [PLATUI-1684](https://jira.tools.tax.service.gov.uk/browse/PLATUI-1684)

## Context and Problem Statement

Teams using headless Chrome are not currently able to use the extension required for the accessibility-assessment. It's not currently obvious that headless Chrome is not supported so teams may reach out for support as a result.

### Limitations with the current approach

Headless Chrome doesn't provide support to add extensions (see [Chromium bug](https://bugs.chromium.org/p/chromium/issues/detail?id=706008#c5)).

## Decision Drivers

- It's not clear that headless Chrome cannot be used to run the accessibility-assessment.
- Fast feedback should be provided to teams that headless Chrome cannot be used to run the accessibility-assessment.
- We already provide guidance on using browsers in Docker containers (see [Confluence docs](https://confluence.tools.tax.service.gov.uk/x/wwHuE)) which is preferred to headless Chrome.

## Considered Options

### Option 1: Investigate headless flag alternatives and provide guidance on usage

Pros:
- Wouldn't require changes to webdriver-factory.
- Issue was tested and fixed using additional flag `--headless=chrome`.
- Simple change to implement.

Cons:
- Teams would be required to make change(s) to their test implementation.
- PlatUI would be required to create additional guidance that may get lost over time.
- The `--headless=chrome` flag is not documented anywhere officially (see [Chromium bug](https://bugs.chromium.org/p/chromium/issues/detail?id=706008#c36) comment).
  - No understanding of what the flag is actually doing.
  - If the flag changes in future we may not be aware and may not be able to help teams that are using it.

### Option 2: Modify the webdriver-factory to add a chrome option to disable headless

Pros:
- Teams wouldn't be required to make change(s) to their test implementation.
- PlatUI wouldn't be required to create additional guidance around the change.
- Simple change to implement.

Cons:
- Not obvious to teams that headless Chrome is not supported.
- Teams may reach out for support for us to investigate why the headless flag is not being applied.
- Requires an update to webdriver-factory, therefore, teams would need to upgrade for the change to take effect.

### Option 3: Modify webdriver-factory to remove any custom flags/options set by teams

See pros and cons of Option 2 and the following:

Cons:
- Not obvious to teams that all custom capabilities/flags have been removed.
- Teams may reach out for support for us to investigate why capabilities/flags are not being applied.

### Option 4: Modify webdriver-factory to notify teams using headless chrome that it's not supported

Pros:
- Teams wouldn't be required to make change(s) to their test implementation.
- PlatUI wouldn't be required to create additional guidance around the change.
- Simple change to implement.
- Fast feedback provided to teams that headless Chrome cannot be used to run the accessibility-assessment and is not supported.

Cons:
- Requires an update to webdriver-factory, therefore, teams would need to upgrade for the change to take effect.

## Decision Outcome

Chosen option: Option 4. Modify webdriver-factory to notify teams using headless chrome that it's not supported.
