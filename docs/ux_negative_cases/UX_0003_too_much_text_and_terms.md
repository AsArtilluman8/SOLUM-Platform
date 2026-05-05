# UX-0003: Too much text, unclear terms and weak explanations

## Problem

AI responses often contained too much text, too many English terms, and unclear explanations.

User could lose the point and say “давай дальше” without understanding what changed.

## Why bad

- user loses project understanding;
- mistakes are harder to catch;
- development becomes passive;
- learning does not happen;
- large answers make the project feel scarier than it is.

## Rule

Responses must be:

- Russian;
- short;
- structured;
- without water;
- with simple term explanations;
- with process algorithms using arrows.

## Correct explanation example

Bad:

```text
Target system — система таргета.
```

Good:

```text
Target System — система выбора цели.
Она решает, на кого персонаж сейчас смотрит, атакует или фокусируется.

Игрок нажал lock-on
↓
система ищет ближайшего врага перед камерой
↓
если враг найден — камера и атаки фокусируются на нём
↓
если враг ушёл далеко или умер — цель сбрасывается
```

## Patch response requirement

After each patch explain:

- what changed;
- what user should see;
- what to verify;
- where APK/report/log is;
- known issues;
- next step.
