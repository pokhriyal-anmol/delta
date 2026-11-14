# Contributing

Bug reports and small fixes are welcome. Open an issue before starting anything large, because the
app has hard rules (see `CLAUDE.md`): no background work, no server of our own, F1 only.

The code is AGPL-3.0. The same author also distributes the app through Google Play, whose terms
cannot be applied to code the author does not own. So a pull request is merged only after you agree
in the pull request that the author may also distribute your change under other terms, in addition to
the AGPL. If you would rather not, keep your change in your own fork, which the AGPL fully allows.

Before you open a pull request:

```bash
./gradlew testDebugUnitTest      # needs JDK 17
```

Feed and API text is untrusted input. Read `SECURITY.md` before touching anything network related.
