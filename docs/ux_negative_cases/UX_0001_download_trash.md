# UX-0001: Download folder became project trash

## Problem

В прошлых проектах Download забивался файлами:

```text
patch.py
patch (1).py
patch (2).py
app-debug.apk
report.zip
dump.zip
screenshots
logs
```

Пользователь вручную удалял мусор и мог случайно запустить старый патч.

## Why bad

- трудно найти актуальный APK;
- легко применить не тот patch;
- появляются копии `(1)`, `(2)`;
- повышается риск случайного удаления;
- визуально проект ощущается как хаос.

## Rule

Все SOLUM output-файлы должны идти только в controlled folder:

Preferred:

```text
/storage/emulated/0/SOLUMCreative/
```

Fallback:

```text
/storage/emulated/0/Download/SOLUMCreative/
```

## Required solution

```text
SOLUMCreative/
  releases/latest/
  releases/archive/
  diagnostics/latest/
  diagnostics/archive/
  reports/latest/
  reports/archive/
  projects/
  assets/
  exports/
  temp/
```

Каждый patch-файл должен иметь уникальную версию в имени.

Wildcard deletion запрещён без явного подтверждения пользователя.
