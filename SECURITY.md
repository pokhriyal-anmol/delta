# Security — the honest version

You said: *"high security, I don't want hackers scraping the internet getting access to my phone."*

That specific fear doesn't apply to this app, and understanding **why** is more useful than
any checklist — because it tells you which risks are real and which are theatre.

---

## Why "hackers scraping the internet" cannot reach this app

Scanning finds things that **listen**. Shodan, mass port scans, botnets — they work by
connecting to open ports on reachable IPs.

This app **never listens**. It has no server, no open port, no bound socket, no DNS record,
no public IP. It makes outbound HTTPS requests to `api.jolpi.ca` and `media.formula1.com`
and reads the replies. That's the entire network surface.

An outbound-only client is not addressable from the internet. There is nothing to scan. Your
phone is also behind carrier CGNAT or your home router's NAT, so even if the app *did* listen,
there'd be no route to it from outside.

**This is a structural property, not a setting.** It can't be misconfigured away. It's why the
local-first, no-server architecture is a security decision as much as a data-ownership one:
the server you never built is the server nobody can attack.

## The one thing that *does* listen: Syncthing

When Tasks lands, Syncthing (a **separate app** — not this code) opens a listening port for
peer sync. That's your only inbound surface in the whole system.

It's well-designed: TLS 1.3, and every peer must be explicitly allowlisted by device ID —
an unknown device can connect but cannot sync. Still, three things to do when you get there:

- **Turn off Global Discovery and Relaying.** Both announce you to Syncthing's public
  infrastructure. You sync at home over Wi-Fi; you only need Local Discovery.
- Never enable the Web GUI on `0.0.0.0`. Localhost only.
- Keep the folder ignore-permissions off and the device list short.

That's the honest place to spend your paranoia — not on this tab.

---

## What's actually in this project

| # | Risk | What's done | Where |
|---|---|---|---|
| 1 | Plaintext traffic / café-wifi MITM | `usesCleartextTraffic=false` **and** a network security config denying cleartext. No HTTP can leave this app. | `AndroidManifest.xml`, `network_security_config.xml` |
| 2 | MITM via a proxy cert | `<certificates src="system"/>` only — **user-installed CAs are not trusted**. Blocks Burp/mitmproxy-style interception on a compromised device. | `network_security_config.xml` |
| 3 | Another app reading your data | Zero exported components except the launcher activity. No content providers, no exported services/receivers. `InitializationProvider` removed. | `AndroidManifest.xml` |
| 4 | Over-permissioning | Two permissions total: `INTERNET`, `ACCESS_NETWORK_STATE`. No storage, location, contacts, camera, mic. | `AndroidManifest.xml` |
| 5 | Cache leaking to Google's cloud | `allowBackup=false` + explicit exclusion of `feeds.db` from cloud backup and device transfer. | `backup_rules.xml`, `data_extraction_rules.xml` |
| 6 | Reverse-engineering the APK | R8 shrink + obfuscate on release. | `app/build.gradle.kts` |
| 7 | Secrets in the binary | There are none. Jolpica needs no auth; F1's CDN needs no auth. Nothing to steal. | — |
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

## What to do when Tasks and Twitch land

- **Twitch token → Android Keystore**, not SharedPreferences. Use `EncryptedSharedPreferences`
  (androidx.security-crypto) with a Keystore-backed master key. Device Code Flow means no
  client secret in the APK — that's already the plan.
- **tasks.db → consider SQLCipher.** Your phone is encrypted at rest already, so this only
  matters against an unlocked-and-stolen device. Judgement call; probably not worth it.
- **Op logs are plaintext JSONL on disk by design.** That's the point — any client can read
  them. If that bothers you, encrypt the Syncthing folder, not the file format.
- **The dictation model download must be checksummed.** You're fetching several hundred MB and
  executing it via ONNX runtime. Verify SHA-256 against a pinned value. This is a *real* supply
  chain risk and the only one in the whole project that worries me.

## The realistic threat model, ranked

1. **You install a malicious APK** — by far the likeliest way your phone gets compromised, and
   nothing in this project affects it. Sideload only what you built.
2. **Syncthing misconfigured** with global discovery on and a loose device list.
3. **Unlocked phone, physically taken.** Screen lock is your control, not code.
4. **Compromised dependency.** Pin versions, use `./gradlew dependencies` occasionally.
5. ~~Someone scanning the internet and finding your app~~ — **not possible.** No listener.

Number 5 is what you asked about. It's the only one on the list that can't happen.
