# Contributing to Delta

Bug reports and focused fixes are welcome. For anything bigger than a fix, open an issue first so
we can agree on it before you spend the time.

## What gets merged

Delta has a few hard rules. A change that breaks one will not be merged, however good it is.

1. **No background execution.** No polling, no periodic jobs, no foreground services, no
   persistent sockets. The app does nothing when it is closed. A single alarm for a known future
   time (the race reminder) is the only exception.
2. **No server of our own.** The app fetches public data on open and keeps a disposable copy.
3. **F1 only.** Other series and other kinds of tab are out of scope.
4. **Untrusted input stays untrusted.** Feed and API text is stripped to plain text, only `https`
   links open and sizes are capped. Read [SECURITY.md](SECURITY.md) before touching anything that
   touches the network.
5. **Design decisions are recorded.** Read [docs/design-system.md](docs/design-system.md) first. If
   you want to change a visual rule, change the document in the same pull request and say why.

## Setting up

You need JDK 17 and the Android SDK.

```bash
git clone https://github.com/pokhriyal-anmol/delta.git
cd delta
./gradlew assembleDirectDebug
adb install -r app/build/outputs/apk/direct/debug/app-direct-debug.apk
```

## Before you open a pull request

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest
```

- Add a test for new logic. Parsing, calculations and anything touching money or security should
  have one.
- Keep the change to one concern.
- If the change alters a screen, update both sets in [`screenshots/`](screenshots/README.md)
  (`dark/` and `light/`).
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
  `feat(news): ...`, `fix(championship): ...`, `docs: ...`.

## Licence of contributions

The code is AGPL-3.0. The author also distributes the app through other channels whose terms cannot
be applied to code the author does not own. So a pull request is merged only after you agree in the
pull request that the author may also distribute your change under other terms, in addition to the
AGPL. If you would rather not, keep your change in your own fork, which the AGPL fully allows.
