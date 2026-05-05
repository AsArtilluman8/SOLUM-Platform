# UX-0004: Too many micro-patches

## Problem

Previous projects had many tiny patches for one real feature:

- one button;
- one slider;
- one debug text;
- one small variable;
- one partial UI change.

After many patches, the feature still did not feel complete.

## Why bad

- progress feels fake;
- project becomes patch soup;
- user loses motivation;
- UX does not become a complete tool;
- bugs accumulate across many tiny changes.

## Rule

Prefer large, coherent, testable vertical-system patches.

One patch should close one system layer.

Good:

```text
Diagnostics v1
Asset Schema v1
Vulkan Foundation v1
Material Studio v1
Transform Tool v1
```

Bad:

```text
add one bias slider
add one button
add one text label
```

## Exception

Micro-patch is allowed only for:

- compile/build fix;
- one-line hotfix;
- emergency rollback;
- targeted fix after diagnostics.
