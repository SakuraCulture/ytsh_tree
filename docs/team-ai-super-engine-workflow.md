# Team AI Super Engine Workflow for ant_dev

> This document is the `ant_dev` project mirror and local entry point.
> The authoritative spec lives at `C:/Users/ytsh01/.claude/docs/team-ai-super-engine-workflow.md`.

## 1. What this file is

This file is not the workflow source of truth. It exists so readers inside `ant_dev` can quickly see where the authoritative workflow spec lives and how that workflow is already wired into this project context.

## 2. Where the authoritative spec lives

The authoritative Team AI Super Engine Workflow spec is:

`C:/Users/ytsh01/.claude/docs/team-ai-super-engine-workflow.md`

Update that file first whenever the workflow rules change. Treat this repo file as a mirror plus local usage notes.

## 3. What is already wired in this project context

The current project context is already connected to the workflow through three layers:

- `~/.claude/skills/running-super-engine-workflow/SKILL.md`
  - provides the top-level workflow skill used for implementation-class requests
- `~/.claude/CLAUDE.md`
  - routes implementation work toward `running-super-engine-workflow`
- `~/.claude/settings.json`
  - injects a reminder hook so implementation prompts prefer the workflow before direct execution or config-first wiring

## 4. How to use it in ant_dev

For this repository:
- feature and behavior-change requests should hit `running-super-engine-workflow` first, then route into design and planning
- bug-fix requests should hit `running-super-engine-workflow` first, then route into systematic debugging
- workflow-solidification requests should produce or update the workflow source of truth before changing routing, hooks, or other automation

This repository should not treat `CLAUDE.md`, the skill file, or hook config as the rule source. Those are integration layers that carry the workflow defined by the global spec.

## 5. Project-local notes

- This repo keeps design specs under `docs/superpowers/specs/`.
- This repo keeps implementation plans under `docs/superpowers/plans/`.
- The workflow formalization design for this mirror is recorded in `docs/superpowers/specs/2026-05-12-team-ai-super-engine-workflow-formalization-design.md`.
