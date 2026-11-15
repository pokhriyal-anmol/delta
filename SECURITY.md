# Security policy

## Reporting a vulnerability

Please report security issues privately through
[GitHub's private vulnerability reporting](https://github.com/pokhriyal-anmol/delta/security/advisories/new)
and do not open a public issue. Expect a first reply within a week. Delta is a hobby project
maintained by one person, so fixes are best effort, but a real vulnerability gets priority over
feature work.

Only the latest release is supported.

## Why the app has a small attack surface

Scanning tools such as Shodan and mass port scans find things that listen. Delta never listens.
It has no server, no open port and no bound socket. It makes outbound HTTPS requests to
`api.jolpi.ca` (results), `api.multiviewer.app` (circuit outlines), `media.formula1.com` (driver
photos and car images), the hosts of the news feeds listed in
[docs/v2-spec.md](docs/v2-spec.md) and, in the `direct` build only, `github.com` (the releases
feed, at most once a day, to show an update note), and reads the replies. That is the whole network
surface. Every request carries a `Delta/<version>` User-Agent and no other identifier. The update
note only opens an `https` release page in the browser. The app never downloads or installs an APK.

An outbound-only client cannot be addressed from the internet. This is a property of the design
and cannot be misconfigured away.

## What the app does about it

| Risk | Mitigation | Where |
|---|---|---|
| Plaintext traffic | `usesCleartextTraffic=false` and a network security config that denies cleartext. No HTTP can leave the app | `AndroidManifest.xml`, `network_security_config.xml` |
| Interception with an installed certificate | Only system CAs are trusted. User-installed CAs are not | `network_security_config.xml` |
| Another app reading Delta's data | No exported components except the launcher activity, no content providers, `InitializationProvider` removed | `AndroidManifest.xml` |
| Over-permissioning | Four permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` and `USE_EXACT_ALARM` (the one race reminder). No storage, location, contacts, camera or microphone | `AndroidManifest.xml` |
| Cache leaking into cloud backup | `allowBackup=false` and `feeds.db` excluded from backup and device transfer | `backup_rules.xml`, `data_extraction_rules.xml` |
| Reverse engineering | R8 shrinking and obfuscation on release builds | `app/build.gradle.kts` |
| Secrets in the binary | None. Every service Delta calls needs no key | n/a |
| URLs in logcat | No logging interceptor | `data/Api.kt` |
| Third-party SDKs | None. No analytics, crash reporting or ads. Dependencies come from Google, Square and JetBrains | `app/build.gradle.kts` |

### Certificate pinning is deliberately off

The config has a `<pin-set>` block, commented out. Pinning is the one control here that can
brick an app: a host rotates its certificate (Let's Encrypt does every 90 days), every request
fails and the fix is a new APK. Trusting only system CAs already blocks the realistic attack, a
proxy certificate on a compromised device. Pinning defends against a compromised public CA, which
is a poor trade for public F1 data.

## Untrusted input

Feeds and API replies are the one place the app parses text written by strangers. Every field
is treated as untrusted.

- **Strip, never render.** Titles and snippets become plain text with tags removed and entities
  decoded. There is no WebView, no HTML rendering and no JavaScript from a feed.
- **Only `https` links open.** An item whose URL is not `https` is rejected, so a feed cannot
  hand the app an `intent:` or `file:` link. Articles open in the browser with a plain view intent.
- **Everything is capped.** Maximum feed size, items per feed and snippet length, so one broken
  feed cannot fill the cache or stall the tab.
- **Images load through Coil only,** over https, from the host the feed named. The realistic
  risk is a tracking pixel, and the cost is the publisher seeing your IP, as in any browser visit.
- **Fetch on tab open only.** No background refresh, which is also a security property.
- **Reddit follows the same rules plus two.** Only the three subreddits in `SUBREDDITS` can
  reach the request URL. A post link is kept only if its host is `reddit.com` or `redd.it`. No
  Reddit account, token or API key is used or stored.

## Learn F1 adds no input

The Learn section is text and drawings compiled into the app. It makes no request, parses nothing
from outside, opens no link and stores nothing. Its one date-dependent piece, the fact of the day,
is chosen from the phone's clock. Nothing in it changes the threat model below.

## Threat model, ranked

1. Installing a malicious APK. This is by far the likeliest way a phone is compromised, and
   nothing in this project changes it. Install Delta only from this repository's releases, or
   build it yourself.
2. A hostile feed item crafted to abuse the app. Covered by the rules above.
3. A physically taken, unlocked phone. Screen lock is the control, not code.
4. A compromised dependency. Versions are pinned and upgrades are separate, reviewable changes.
5. Someone finding the app by scanning the internet. Not possible, because nothing listens.

## The support link

The `direct` flavor can show one sentence and a link at the bottom of Home. The link opens in the
browser through the same https-only check the News tab uses (`httpsUrl`), so the app makes no
request of its own and gains no permission. If `supportUrl` is empty or not https, nothing is
drawn. The `play` flavor has no such code at all (`app/src/play`), because Google Play has removed
apps for donation links.
