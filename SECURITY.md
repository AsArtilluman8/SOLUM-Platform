# Security Policy

Status: public security policy.

SOLUM is early-stage software. Please do not treat it as a hardened production SDK yet.

## Reporting vulnerabilities

If you find a security issue, do not post exploit details publicly first.

Preferred report content:

- affected branch or commit;
- affected files;
- reproduction steps;
- expected impact;
- suggested fix if known.

## Sensitive information

Do not commit:

- API keys;
- tokens;
- private credentials;
- private user data;
- private device or account details;
- private build logs containing secrets.

## AI/code-agent safety

AI agents used with this repository should:

- read the current repo state before editing;
- avoid inventing implemented features;
- avoid exposing private context;
- keep user approval for risky changes;
- document files changed and commands run.

## Current scope

This policy covers the public repository, documentation, build scripts, Android app code, and future package/agent systems.
