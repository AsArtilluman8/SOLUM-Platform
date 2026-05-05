# PATCH_RESEARCH_TEMPLATE

Use this template before any Research-Gated patch.

```markdown
# Research Summary — PXX: Topic

## 1. Problem
What exact SOLUM problem are we solving?

## 2. Current SOLUM state
What exists now?
What constraints must not be broken?

## 3. References checked
- Repo/doc 1 — path/link/topic
- Repo/doc 2 — path/link/topic
- Repo/doc 3 — path/link/topic

## 4. What each reference teaches

### Reference 1
Useful principles:
- ...

Rejected parts:
- ...

### Reference 2
Useful principles:
- ...

Rejected parts:
- ...

## 5. Options

### Option A — REFERENCE_ONLY
What we do.
Pros.
Cons.

### Option B — SMALL_SLICE
What we adapt.
Pros.
Cons.
License/build risk.

### Option C — ADAPTER
What adapter means here.
Pros.
Cons.

### Option D — DEPENDENCY
Dependency name.
License.
Build feasibility.
Size/perf risk.

### Option E — REJECT
Why rejected.

## 6. Recommended choice
Recommended path and why.

## 7. SOLUM adaptation
How it fits:
- architecture;
- asset schema;
- diagnostics;
- UI/UX;
- performance budget;
- Android/Termux.

## 8. Risks
- build risk;
- runtime risk;
- performance risk;
- UX risk;
- maintenance risk.

## 9. Diagnostics/test plan
How we prove it works:
- build evidence;
- runtime evidence;
- report fields;
- performance snapshot;
- visual QA if render/UI.

## 10. User decision required
What user must choose before patch.
```

## Minimal chat answer format

```text
Research Gate
References checked:
1. ...
2. ...
3. ...

Options:
A) ...
B) ...
C) ...

Recommended: ...
Need your choice: A/B/C
```
