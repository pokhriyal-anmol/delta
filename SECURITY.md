# Security — the honest version

You said: *"high security, I don't want hackers scraping the internet getting access to my phone."*

That specific fear doesn't apply to this app, and understanding **why** is more useful than
any checklist — because it tells you which risks are real and which are theatre.

---

## Why "hackers scraping the internet" cannot reach this app

Scanning finds things that **listen**. Shodan, mass port scans, botnets — they work by
connecting to open ports on reachable IPs.

This app **never listens**. It has no server, no open port, no bound socket, no DNS record,
no public IP. It makes outbound HTTPS requests to `api.jolpi.ca` (results), `api.multiviewer.app` (track
outlines), `media.formula1.com` (driver photos and 2026 car images) and the news publishers' feed and image hosts
(listed in `.scratch/f1-v2/spec.md`), and reads the replies. That's the entire network surface.

An outbound-only client is not addressable from the internet. There is nothing to scan. Your
phone is also behind carrier CGNAT or your home router's NAT, so even if the app *did* listen,
there'd be no route to it from outside.

**This is a structural property, not a setting.** It can't be misconfigured away. It's why the
local-first, no-server architecture is a security decision as much as a data-ownership one:
the server you never built is the server nobody can attack.

## Nothing listens, anywhere

There is no second app and no peer sync. The earlier plan had Syncthing for a Tasks tab, which
was the only inbound surface in the system. Tasks is gone, so the surface is zero.

---

## What's actually in this project

| # | Risk | What's done | Where |
|---|---|---|---|
| 1 | Plaintext traffic / café-wifi MITM | `usesCleartextTraffic=false` **and** a network security config denying cleartext. No HTTP can leave this app. | `AndroidManifest.xml`, `network_security_config.xml` |
| 2 | MITM via a proxy cert | `<certificates src="system"/>` only — **user-installed CAs are not trusted**. Blocks Burp/mitmproxy-style interception on a compromised device. | `network_security_config.xml` |
| 3 | Another app reading your data | Zero exported components except the launcher activity. No content providers, no exported services/receivers. `InitializationProvider` removed. | `AndroidManifest.xml` |
| 4 | Over-permissioning | Four permissions total: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` and `USE_EXACT_ALARM` (the one race reminder, an hour before lights out). No storage, location, contacts, camera, mic. | `AndroidManifest.xml` |
| 5 | Cache leaking to Google's cloud | `allowBackup=false` + explicit exclusion of `feeds.db` (results cache and news) from cloud backup and device transfer. | `backup_rules.xml`, `data_extraction_rules.xml` |
| 6 | Reverse-engineering the APK | R8 shrink + obfuscate on release. | `app/build.gradle.kts` |
| 7 | Secrets in the binary | There are none. Jolpica, MultiViewer, F1's CDN and the RSS feeds need no auth. Nothing to steal. | — |
| 8 | URLs leaking to logcat | No logging interceptor. Any app with `READ_LOGS` could otherwise read your traffic. | `data/Api.kt` |
| 9 | Third-party SDKs phoning home | None. No analytics, no crash reporting, no ads. Dependencies are Google/Square/JetBrains only. | `app/build.gradle.kts` |

## Certificate pinning — deliberately left off

The config has a `<pin-set>` block, **commented out**. That's on purpose.

Pinning is the one item here that can **brick your app**. Pin a cert, the host rotates it
(Let's Encrypt does every 90 days), and every request fails until you ship a new APK. For a
sideloaded personal app with no update channel, that's a self-inflicted outage.

You already have `system`-CA-only trust, which blocks the realistic attack (a proxy cert
installed on your device). Pinning defends against a *compromised public CA* — a nation-state
problem, not a yours problem, and the data is public F1 results anyway.

If you want it anyway, generate real pins:

```bash
openssl s_client -connect api.jolpi.ca:443 -servername api.jolpi.ca < /dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Pin the **intermediate**, not the leaf, and always include a backup pin. Set `expiration` so
it fails open rather than bricking.

---

## What to do when News lands

Feeds are the first place this app parses text written by strangers. Treat every field as
untrusted.

- **Strip, never render.** Titles and snippets become plain text (tags removed, entities
  decoded). No WebView, no HTML rendering, no JavaScript from a feed.
- **Only open `https` links.** Reject any item whose URL is not `https`, so a feed cannot hand
  the app an `intent:` or `file:` link. Open articles in the browser with a plain view intent.
- **Cap everything.** Maximum feed size, items per feed and snippet length, so one broken feed
  cannot fill the cache or stall the tab.
- **Images are loaded by Coil only**, over https, from the host the feed named. A tracking
  pixel is the realistic risk; the cost is the publisher seeing your IP on open, which any
  browser visit does too.
- **Fetch on tab open only.** No background refresh, which is also a security property.
- **Reddit is the same rules plus two.** Only the three subreddits in `SUBREDDITS` can reach the request URL, so no name is ever typed in. A post link is kept only if its host is reddit.com or redd.it. Reddit's feeds allow roughly one request a minute without an account, so only the selected subreddit is fetched, and a 429 shows a quiet note and keeps the old copy. No Reddit account, token or API key is used or stored.

## The realistic threat model, ranked

1. **You install a malicious APK** — by far the likeliest way your phone gets compromised, and
   nothing in this project affects it. Sideload only what you built.
2. **A hostile feed item** (a link or text crafted to abuse the app). Covered by the News rules above.
3. **Unlocked phone, physically taken.** Screen lock is your control, not code.
4. **Compromised dependency.** Pin versions, use `./gradlew dependencies` occasionally.
5. ~~Someone scanning the internet and finding your app~~ — **not possible.** No listener.

Number 5 is what you asked about. It's the only one on the list that can't happen.
