# 1. Record architecture decisions

## Status

Accepted

## Context

This project will accumulate technical decisions over time — choices between competing designs, results of spikes, and trade-offs that aren't obvious from reading the code or git history alone. Without a record, that reasoning is lost as soon as the PR discussion scrolls out of view, and future contributors (human or AI) end up re-litigating settled questions or unknowingly undoing them.

## Decision

We will record significant technical decisions and spike learnings as Architecture Decision Records (ADRs) in `docs/adr/`, numbered sequentially, using the format in [0000-template.md](0000-template.md).

An ADR is warranted for decisions such as: a choice between competing architectural approaches, adopting or rejecting a library/tool with lasting impact, a deviation from a convention in [AGENTS.md](../../AGENTS.md), or the outcome of a spike. It is not warranted for routine implementation work, bug fixes, or anything the code itself makes self-evident.

## Consequences

Decisions and the reasoning behind them stay discoverable without digging through PR history. ADRs are immutable once accepted — a changed decision gets a new ADR that supersedes the old one, rather than an edit, so the history of *why* is preserved alongside the history of *what*.
