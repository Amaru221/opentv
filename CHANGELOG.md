# Changelog

## 0.12.5

- **The player bar keeps four buttons and pushes the rest into a "⋯" row.** Subtitles, audio,
  aspect, record, picture-in-picture and the new sleep timer live in a row anchored to the bar.
  Eight icon+label chips in one row was wider than a phone, and the labels were being squeezed
  until "Record" ran one letter per line.
- **A timeline above the buttons**, with elapsed / total and a scrubber. Shown only when the
  content can be seeked — a live channel has no duration.
- **Sleep timer in the player**, with its list (off, 15, 30, 45, 60, 90, 120 min). It stops
  playback and returns to the guide; it does not lock the device, and because the player releases
  its keep-screen-on flag on the way out, the system can then sleep the screen on its own.
- **The on-screen keyboard is a keyboard.** It drew fixed 48 dp keys — 552 dp for the widest row —
  so everything past the second row was cut off. Keys are now sized from the available width, the
  rows are staggered 10/10/9/7, and shift, space and backspace are where you expect them. Shift
  also toggles case: before, every character came out upper case, which breaks case-sensitive hosts.
- **The nav rail no longer loses Search, Me and Settings** on a phone in portrait: the tabs scroll
  and those three stay pinned.
- **A title is no longer squeezed by its buttons.** The TV guide header put its title and two
  buttons on one row; on a phone the title had no width and broke one letter per line.
- **The movie player gets the same bar** — four round buttons and an overflow row.

## 0.12.4

- **Two providers are no longer mixed into one list.** With more than one provider configured, the
  guide used to start in "all sources", which folded every provider's categories into a single rail
  — Spain, UK, Movies and Series side by side, though those shelves belong to different services.
  It now starts on one provider, and the categories you see all come from it.
- **The provider switch is on phones again.** It lived in the side rail, which the compact layout
  replaced with chips, so on a phone it had stopped being drawn at all. There is now a chip per
  provider above the categories, shown only when there is more than one.
- **The choice is remembered**, per device, and falls back to "all" if that provider is deleted.
  Choosing "all sources" is still possible — it is now a decision rather than the default.
- **"All channels" says which provider it means**: "All channels · Movistar", or plain when the
  guide really is showing everything.

## 0.12.3

Documentation only — no behaviour changes.

- **The README now documents the free Spanish DTT lists.** OpenTV still ships with no channels and
  no playlists, but "free-to-air television" is a real, legal, account-free case the app already
  supports, and it was undocumented. The addresses for the channel list and its guide, the steps in
  the app, and the three caveats (Antena 3 and laSexta are not in it, the links rotate, and
  TDTChannels is a third party we fetch from rather than something we bundle) are now in one place.
- **Donation references removed.** The About screen lost its donate section in 0.12.0; the README
  kept advertising it, screenshot included. Both are gone, and the screenshot is replaced with a
  current one.

## 0.12.2

- **The channel name has room in landscape and on a TV.** The column was 180 dp wide with 92 dp of
  it taken by the number, logo, gap and favourite star, which left about eight characters — enough
  for "Movistar LaL…" and no more. It is now 240 dp on a phone held sideways, 260 on a tablet and
  300 on a TV, so the name gets 16, 19 and 24 characters. The timeline coarsened from 3.5 to 3 dp
  per minute in landscape, so it still shows about an hour and three quarters.
- The channel number and the star are slightly smaller to pay for it. In portrait nothing changes:
  the list already gives the name the whole width.

## 0.12.1

- **The guide now fits the screen it is on.** It was built for a television and shipped unchanged
  to phones, where the channel column, the nav rail and the margins left 37 dp for the timeline —
  about nine minutes of guide — and the preview card was wider than the screen it sat in. On a
  phone you now get a channel list with the categories as a tappable strip, and no preview pane,
  because the list already shows what is on now, what is next and how far through it is.
- **A phone held sideways is a phone, not a tablet.** Everything is sized from the screen: 260 dp
  channel column and 5 dp per minute on a TV, 180 dp and 3.5 on a phone in landscape. The category
  rail is replaced by chips on touch devices — there is no d-pad there to slide a hidden panel open.
- **The channel name has room again.** The favourite star went from 40 dp to 28 dp (40 on a phone,
  where a finger needs it) and rows grew with the system font size instead of being fixed at 64 dp.
- **Added a "now" line and half-hour grid lines** to the guide, so you can see where you are in the
  evening and read a block's start time against the header.
- **Programme blocks line up with the time header again.** A short programme used to be widened to
  keep its title readable, and everything after it drifted out of step with the clock.
- **System bars no longer cover the app on a phone**, and the preview pane on a TV is 160 dp instead
  of 212, so it takes a quarter of the screen instead of two fifths.

## 0.12.0

First release from this fork. Everything in 0.11.10 plus:

- **Automated backups are off.** Your provider credentials, your SMB password and your watch
  history no longer get copied to Google Drive by Android's own backup service. That was the one
  way those credentials could leave the device without you asking for it.
- **Device sync no longer sends your stream URLs.** A provider URL contains your username and
  password, and the sync bundle was built from them. It now carries a hash instead, so two boxes
  still agree on what you watched without either one seeing the other's account. Sync with an
  older build keeps working.
- **Free guides are offered, not switched on.** The app used to notice your channels looked like
  they came from one region and download a guide from a third-party host by itself. It now asks
  first, in Settings → TV guide.
- **Update checks look at this repository.** Builds from here were checking the upstream project's
  releases and would have replaced themselves with that APK.
- Privacy page rewritten to list every destination the app can reach.

## 0.11.10

- **Fire TV: app row fix, take two.** v0.11.9 replaced the placeholder artwork but the stick still
  wouldn't list OpenTV. Fire OS reads a sideloaded app's launcher icon straight out of the APK by
  path, and it only looks in `res/drawable…` folders (a known Fire OS 7/8 quirk that leaves apps
  with icons under `res/mipmap…` broken or missing). The icon now lives in `drawable-*dpi` and is
  referenced as `@drawable/ic_launcher`, and the release build keeps real resource paths instead
  of aapt2-shortened ones, so the stick can find it. After updating, uninstall OpenTV, restart the
  stick and install it again from Downloader so the launcher re-reads the APK.

## 0.11.9

- **OpenTV now shows up on the TV home screen.** The app was built with a placeholder vector
  banner and an adaptive-only (XML) launcher icon, which the Fire TV and Google TV launchers
  either rendered as a blank dark tile with no name on it or failed to render at all, so a
  sideloaded install only appeared under Settings → Applications. The APK now ships a proper
  320×180 **banner with the OpenTV wordmark** (Google TV / Android TV home screen) and plain
  **PNG launcher icons at every density** (Fire TV app row, phones), which is what both
  launchers expect. Install the update and the OpenTV tile appears in the apps row — on the
  new Fire TV layout it lands at the end of the row, so hold Select → Move to bring it forward.

## 0.11.8

- **Edit a saved provider.** Settings → Providers now has an **Edit** button on each provider, so you
  can fix a server address, password, MAC or name in place instead of removing and re-adding it —
  your favourites, hidden channels and per-provider settings are kept.
- **Stalker portals: fewer dropped sessions.** A portal that occasionally rejected a call
  mid-session (channels loading only "sometimes") now gets one automatic re-handshake and retry, so
  an intermittent refusal becomes a silent re-auth rather than an empty channel list or a channel
  that won't tune.
- **Player controls stay clear of the system bar off-TV.** On phones and tablets the bottom control
  bar (subtitles, audio, quality, aspect ratio) could be drawn under the navigation bar and cut off;
  it's now inset above it. No change on Android TV.

## 0.11.7

- **Stalker portals: closer to a real box.** Building on v0.11.6, OpenTV now sends the rest of the
  identity a real MAG set-top box presents at login — the auth token in the session cookie (not just
  the header), a hardware-version hash, a timestamp and the box's API signature — and it probes a
  couple more portal paths. Some Ministra/Stalker panels only return the channel list when they see
  all of this, so lines that connected but showed no channels have a better chance of loading. If a
  portal still comes up empty, please open an issue and say which panel software it runs.

## 0.11.6

- **Stalker portals: full set-top-box identity.** OpenTV now authenticates to Stalker/Ministra
  portals the way a real MAG box does — sending the device serial, `device_id`, a signature and a
  metrics blob derived from your MAC, instead of the MAC on its own. Many portals won't hand over
  the channel list or stream links to a box that only presents a MAC, which is why some lines that
  worked in other apps came up empty in OpenTV. This lets those portals authorise OpenTV. If a
  portal still fails, please open an issue and say which one — it helps a lot.

## 0.11.5

- **Add a provider from your phone or laptop.** The "Manage on phone or laptop" web page now has an
  **Add a provider** section — pick Xtream, M3U or Stalker portal, fill it in with a real keyboard,
  then Test and Add. Channels load straight onto the TV, so you never have to type a server address
  or a MAC on the remote again.
- **Fixed the "camera won't scan it?" web address.** The manage screen was printing that link without
  its access token, so typing it into a browser hit a dead "Not found". It now shows the full working
  URL, and the token is short enough to type.


## 0.11.4

The big DVR release. Recordings behave like Sky Q, you can record straight to a NAS, watch a
recording while it's still taping, and the whole interface now speaks 30 languages.

### Recording & DVR

- **Watch a recording while it's still recording.** Start playing something the moment it begins
  taping — OpenTV reads the growing file straight off disk, so it costs **zero extra connections**
  to your provider. On a single-stream account that's the difference between "wait until it's
  finished" and "watch now". Fast-forward and rewind work within whatever's been taped so far.
- **Record straight to a NAS.** Point recordings at a network share over SMB (Settings →
  Recordings → NAS, or set it up from the phone/laptop web manager) so a cheap box isn't boxed in
  by its own storage — and every OpenTV in the house can reach the same recordings.
- **Single-connection auto-switch.** On a one-stream provider, if a recording is due to start on
  another channel, OpenTV moves the screen onto that recording as it begins — with a **30-second
  warning** first and a *Keep watching* button if you'd rather not. No more "why did my live stream
  just cut out?" (Recordings → Recording behaviour → *Auto-switch when recording starts*.)
- **Clash handling with multi-provider fallback.** Book two overlapping recordings and, if more
  than one of your providers carries that channel, OpenTV records each from a **different provider**
  so neither is cut. On a single provider it flags the clash instead.
- **Recording padding.** Start each recording a minute early and run a few minutes past the listed
  end, so a late kick-off or an overrun isn't clipped. Adjustable per side.
- **Series links show the next episode.** A series link now tells you exactly when it next records.
- **Storage readout.** See how much space recordings use and how much is left.

### Live TV

- **Pause & rewind live TV** *(experimental, opt-in).* Holds the last couple of minutes so you can
  pause and jump back. Off by default — it uses more memory.

### Languages

- **30 languages.** The whole interface is translated into Spanish, French, German, Italian,
  Portuguese, Dutch, Polish, Russian, Turkish, Arabic, Simplified Chinese, Japanese, Korean, Hindi,
  Swedish, Danish, Finnish, Norwegian, Czech, Greek, Romanian, Hungarian, Ukrainian, Indonesian,
  Thai, Vietnamese, Bulgarian, Slovak, Croatian and Persian. Settings → Language (each listed in
  its own name), or leave it on *System* to follow your device.

### Fixes & polish

- Smoother watch-while-recording, especially from a NAS: playback now keeps a buffer behind the
  live edge so a network share doesn't stutter at the write head.
- Back never lands you on a stray live channel or drops you out by accident — coming back from a
  recording returns to your recordings, and Back on the home screen asks before it exits.
- Password fields are masked, with a show/hide toggle.
- Recording stability: NAS write timeouts and reconnect handling, so a quiet network doesn't
  silently freeze a capture.

### Notes

- Everything stays on your device — provider logins never leave the box.
- Free and open source (GPL-3.0): https://github.com/opentvproject/opentv


## 0.10.0

- **Movies & Shows, redesigned.** A Plex-style layout with big artwork, cast & director, "more with
  this cast" rows, and cleaned-up titles (no more "NF -" / "(KR)" junk). Optional: add your own free
  TMDB key in Settings → Metadata and OpenTV fills in any posters, backdrops, cast or synopses your
  provider left blank — the key stays on your device.
- **No more stuck "Loading movies & shows".** The movies/series catalogue is cached, so it loads
  instantly on later launches instead of re-downloading every time — and the live preview no longer
  stutters while it loads.
- **Record to a USB / external drive.** Settings → Recordings → USB, pick a folder on a plugged-in
  drive, and recordings write straight there (no storage permission needed) and play back in-app.
- **Recordings screen overhaul.** Scheduled recordings show *when* they'll record and sit in their
  own "Scheduled" section; failed ones have a **Retry**; each is tagged to a profile; and bookings
  re-arm on every launch so a force-stop or app update can't quietly drop them.

## 0.9.0

- **Manage your channels from a phone or laptop.** A new "Manage on phone or laptop" screen in
  Settings shows a QR code and a link — open it on any device on your wifi and you get a proper web
  page to browse, **rename**, hide, favourite and **reorder** channels with a real keyboard and
  mouse. Changes apply to the TV instantly. Local-only: the TV is the server, nothing touches a
  cloud.
- **Recording keeps going when you leave the app or the box sleeps.** OpenTV can now ask Android to
  exempt it from battery optimisation (Settings → Recording → "Recording in the background"), which
  is what keeps recordings running in standby and when you switch away — swiping OpenTV out of
  recents no longer stops a recording either.
- **Rename channels.** Give a channel your own name; it sticks and survives a guide refresh (from
  the web manager for now).
- **Tidier update notes.** The "update available" prompt shows a clean summary of what's new instead
  of build metadata.

## 0.8.0

- **No more endless "Loading channels".** If a provider fails to load or comes back empty (a wrong
  login, a dead server), the guide now shows a clear error with **Retry** and **Provider setup**
  buttons instead of spinning forever.
- **Set up on a phone without the QR dance.** Installing on a phone now shows the login form
  directly so you can type your details in; the QR code is only offered on a TV, where typing is the
  painful part.
- **Paste into the setup fields.** The URL, username and password fields — and the on-screen
  keyboard — now have a **Paste** button, so a copied Xtream line drops straight in.
- **MPEG-TS or HLS, your choice.** Each Xtream provider now has a **Stream format** toggle. If your
  panel only serves `.ts` and channels wouldn't play, switch to MPEG-TS and they will. Defaults to
  HLS, so nothing changes unless you need it.
- **Parental controls scrolls.** The hidden-categories list is reachable to the bottom now, however
  many categories your provider has.
- **A real Channel Manager.** Settings → Channels is now a browsable, two-pane manager: pick a
  category on the left, see its channels on the right, hide or favourite each one — including
  hidden channels so you can bring them back. With more than one provider, a **source filter** keeps
  them separate instead of merged into one list.

## 0.3.0

- **Profiles.** Add local profiles (just a name — no accounts) from the person icon in the top
  menu, and switch between them. Each profile keeps its own resume points and watched state, so
  your half-watched film doesn't show as watched on someone else's profile. Existing resume points
  become the default "Me" profile's — nothing is lost.
- **Continue Watching.** Movies and Shows now open with a "Continue watching" shelf for the active
  profile — pick up your last film or episode where you left off, with a progress bar on each.
- **Local sync (no servers).** Settings → Sync copies your continue-watching between two OpenTV
  devices over your own wifi: one device shares behind a six-digit code, the other receives. It's
  local-only — nothing leaves the house — the household answer to the cloud sync that died with
  Viewella.

- **Proper player controls.** A single control bar slides up from the bottom with play/pause,
  rewind/forward (where the stream allows it), and pickers for **subtitles, audio track, quality
  and aspect ratio**. It hides after a few seconds and any remote button brings it back —
  nothing is ever left painted permanently over the picture.
- **Captions that actually show.** Subtitles are now chosen from the real tracks in the stream
  (and can be turned off), instead of a blind on/off that often left the renderer enabled but
  nothing on screen. Audio-track switching works the same way for multi-language streams.
- **Page through the guide.** Earlier / Now / Later buttons above the guide move the timeline
  forward and back, so you can see what's on later this evening with a remote.
- **Live preview in the guide.** The highlighted channel now plays, muted, inside the preview
  pane. It uses a single player that stops the instant you go full-screen or leave the guide —
  no second decoder — and it can be turned off in Display & playback for older boxes.
- **Channel manager.** Settings → Channels: search for a channel and hide it from the guide (or
  favourite it). Hidden channels drop out of Live TV and search but stay here, greyed, so you can
  bring them back. Hiding covers every quality variant of a channel at once.
- **Parental controls.** Set a 4-digit PIN and mark categories as hidden — they drop out of the
  guide, All channels and search until you unlock them (and with a PIN set, the parental screen
  itself is locked). In Settings → Parental controls.
- **Unified search with an on-screen keyboard.** Search now has its own screen with a d-pad
  keyboard (no more "type on your phone") and covers **channels, movies and shows** together, with
  live results as you type. Reachable from the top menu.
- **Loading indicators for Movies and Shows.** Both now show a spinner while the catalogue is
  syncing instead of a premature "nothing here".
- **Settings and Search in the top menu.** Both now live top-right, next to Live TV / Movies /
  Shows, instead of being buried behind the guide's icons.
- **A single Settings screen.** One Settings hub — Providers (add / remove / re-test sources), TV
  guide, Display & playback, Parental controls, and About (version, a manual update check,
  licence and links).
- **Dark / light / follow-system.** A new appearance setting. TV still defaults to dark; pick
  Dark or Light to force it on any device.
- **Quality picker only when there's a choice.** No more "Quality: Standard" on single-quality
  channels; the picker appears only when a channel actually has multiple qualities.
- New **Display & playback** settings screen (the sliders icon in the guide).

## 0.2.0

- **Guide detail pane** — highlighting a channel now shows its logo, what's on now with
  start/end times and a live progress bar, a synopsis when the guide carries one, and what's
  on next. Press to watch full-screen.
- **Live progress fill** on the current programme in the guide grid, and a focus highlight on
  the highlighted channel.
- **Fix: never asks for your provider again.** A cold launch could briefly decide "first run"
  before your saved sources had loaded and drop you on the setup screen — it now waits for the
  sources to load before deciding what to show.
- **Fix: no second video decoder on the guide**, which was locking up low-end boxes (a
  Chromecast could freeze). The detail pane is logo + guide info; the one decoder lives
  full-screen.

## 0.1.0

First public release.

- Xtream Codes and M3U/M3U8 sources
- Streaming XMLTV guide parsing with incremental, non-destructive sync
- Live TV playback on Media3/ExoPlayer with IPTV-tuned retry behaviour
- Movies and series catalogue
- Favourites, categories, search
- In-app self-update for sideloaded installs
- Single APK for Android TV, Fire TV, phones and tablets
