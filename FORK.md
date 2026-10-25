# parth-nikam/cleanyourtv — Sony BRAVIA VU2 fork

A personal fork of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), set up and tuned on a
**Sony BRAVIA VU2** (device `BRAVIA_VU1_4K`, Realtek, Google TV 14 / Android 14, build `UKR1.240726.001`, 2 GB RAM).

Everything the upstream project does still applies; this file lists only what is different here.

## What this fork changes

### 1. A Sony BRAVIA VU2 debloat list
`examples/sony-bravia-vu2/debloat.txt` — disables Sony demo/help, Google feedback/partner-setup/
calendar-sync/Play Games/print/gallery, the Android ad & personalization services, and the Sony partner
auto-installer. Keeps Assistant/voice, Chromecast, firmware updates, Live TV (`com.android.tv`), the inputs
and `com.sony.dtv.smartui`. The stock launcher `com.google.android.tvlauncher` is disabled **last** by
`launcher/build-and-install.sh`, after the custom launcher is confirmed as Home.

Apply / undo:
```
TV_HOST=<tv ip> tools/disable.sh examples/sony-bravia-vu2/debloat.txt
TV_HOST=<tv ip> tools/restore.sh examples/sony-bravia-vu2/debloat.txt com.google.android.tvlauncher
```
Already disabled from the factory and intentionally left alone (do **not** re-enable):
`com.sony.dtv.b2b.vendorprotocol`, `com.sony.dtv.ecodashboard`.

### 2. A header weather line (the one network feature)
The upstream launcher declares **no `INTERNET` permission by design**. This fork adds it for a single opt-in
feature: a current-weather line in the top-left header, under the date.

- `launcher/app/src/main/java/com/gathin/tvhome/Weather.java` — fetches from
  [Open-Meteo](https://open-meteo.com) (no API key) for fixed coordinates. Adapted from the repo's
  `addons/photo-screensaver` weather code, minus location services, so nothing about the TV's own position
  leaves it beyond the city you configure.
- Fetched on a background `HandlerThread` every 30 minutes, started in `onResume` and stopped in `onPause`
  (mirrors the existing stats-line pattern). Offline or a timeout just keeps the last reading.
- `INTERNET` permission added in `AndroidManifest.xml`.

Config keys (in `launcher/app/src/main/res/values/config.xml`):
```
weather_enabled    true            # the line on/off
weather_latitude   18.52           # Pune
weather_longitude  73.86
```
Turn it off by setting `weather_enabled` to `false` and rebuilding.

### 3. Apple TV look + owner settings (`config.xml`)
- `greeting_name` = `Parth` → "GOOD EVENING, PARTH".
- `columns` = 5, `dock_apps` = Netflix, Prime Video, YouTube, JioHotstar, SonyLIV.
- `live_tv_package` = `com.android.tv`, hidden from the grid (the Inputs menu covers it).
- `dock_glass` = true (frosted rounded dock band) and `cinematic_home` = true with `cinematic_art` = 40
  (fills the area above the dock with the focused app's own artwork — no network, uses the local banner).

### 4. Liquid-glass redesign
One coherent "liquid glass" material across the whole home screen (no real blur — too costly on this
Realtek GPU — so layered translucency + specular highlights + hairline rims, which read as glass at TV
distance):
- Top-bar pills (`pill_bg.xml`): one cohesive glass surface per state — a bright top lip easing down into a
  translucent body, with a hairline rim. The ambient glow shows through the resting pills.
- Focus highlight (`GlassView.java`): more body in the fill, brighter top specular, a wider softer glow so
  the focused tile looks lit from within.
- Dock band (`DockDecoration.java`): a three-stop glass gradient with a bright top lip and a crisper rim.
- Tile cards (`tile_bg.xml`): icon-only tiles use the same top-lit gradient + rim, so the grid matches.
- Header typography (`activity_main.xml`): greeting → clock → date/weather tuned into one clean hierarchy.

### 5. Device-wide ad blocking (Private DNS)
Set on the TV (not in the launcher), the network-level equivalent of what Brave does — it blocks ad and
tracker **domains** for the whole TV (apps and the system), which is the only ad blocking possible without
root (no sideloaded app can strip ads from inside Netflix/YouTube):
```
settings put global private_dns_specifier dns.adguard-dns.com
settings put global private_dns_mode hostname
```
Verified: `doubleclick.net` sinkholes to 127.0.0.1 while `netflix.com`/`google.com` resolve normally. Note
it does NOT block YouTube's in-stream ads (they serve from the video domain). Undo:
```
settings put global private_dns_mode off
```
If your router already filters DNS (Pi-hole / AdGuard Home / a filtering VPN), this bypasses it, so blocking
on the router is the better choice there.

### 6. Speed tuning
- `focus_overshoot` = false. On this armeabi-v7a Realtek GPU the spring overshoot drove a ~1.2 s
  movement-settle and kept the launcher busy while idle. Turning it off cut the settle time ~25% and dropped
  idle launcher CPU from ~4.5% to ~0.1%. Full before/after numbers in `work/SPEED-AUDIT.md` (local, not
  committed).

## Rebuild & install after changing config
```
TV_HOST=<tv ip> launcher/build-and-install.sh
```
If the stock launcher is already disabled and this launcher is already Home, pass `STOCK_LAUNCHER=none` to
skip the disable step.

## Change the dock order or apps
Edit `dock_apps` (and `columns` for the slot count) in
`launcher/app/src/main/res/values/config.xml`, then rebuild. App package names:
`tools/tv.py 'pm list packages'`.

## Re-enable any disabled app
```
TV_HOST=<tv ip> tools/tv.py 'pm enable --user 0 <package>'
```
or restore the whole list with `tools/restore.sh` as shown above.

## Closing ADB when done
Turn USB debugging and Network/ADB (Wireless) debugging off on the TV, revoke authorisations, reboot, then
confirm the port refuses the connection:
```
tools/adb-closed.py <tv ip>
```
