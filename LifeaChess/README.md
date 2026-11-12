# LifeaChess

Tabletop chess for Paper 1.21.x, built on [ExyliaLib](https://github.com/DiGround-s/ExyliaLib).

Players sit on **chairs** at a **chess table**, a **big public board** floats between them
for everyone to watch, and each player gets a **full-view packet-screen board** to play on.
`/chess` opens a "MODES » TABLETOP" menu with one entry — Chess — and every command still
works as a shortcut.

```
LifeaChess/
├── chess-engine/   pure-Java rules engine + match core + JDBC storage (no Minecraft deps, 74 JUnit tests)
└── chess-plugin/   the Paper plugin (tables, seats, boards, menus, commands, ratings)
```

## Features

- **Complete chess rules** — legal move generation, castling (all FIDE conditions), en passant,
  promotion/underpromotion, check/checkmate/stalemate, dead positions, 50/75-move rules,
  three/fivefold repetition (en passant folded into the key only when a capture is legal),
  resignation, agreement, flag-fall with the insufficient-material exception. FEN, SAN, UCI.
  Perft-verified: start position d1–d4 (20 / 400 / 8,902 / 197,281), Kiwipete, positions 3–5.
- **Tables & chairs** — admins place tables; players right-click a chair (or use the menu) to
  take White/Black; the match starts when both chairs are filled. Standing up / disconnecting
  pauses the clock for a grace period, then forfeits (or adjourns — configurable).
- **Big in-world board** — 8×8 text-display panel above the table, mirrors the live game,
  header with players/clocks/turn, HOW TO PLAY / HOW TO WIN side panels. Only changed squares
  are re-sent.
- **Screen board** (ExyliaLib Screens + PacketEvents) — click a piece, see legal moves and
  captures, click to move, promotion dialog, resign/draw controls, match-end overlay with
  rating changes and Rematch. Spectators get a read-only view.
- **Clocks** — Bullet/Blitz/Rapid/Classical presets plus custom `m+s`; timestamp-based, only
  the side to move ticks, Fischer increment, low-time warning.
- **Challenges, matchmaking, rematch, spectating, draw offers.**
- **Elo & stats** — configurable K-factor (higher while provisional), peak rating, streaks,
  leaderboard, per-player history; ranked vs casual; same-opponent cooldown against farming;
  each result is applied exactly once.
- **Persistence** — SQLite by default (zero setup) or any JDBC URL (MySQL/MariaDB) via
  `database.type: jdbc`. All database work is asynchronous.

## Requirements

| | |
| --- | --- |
| Server | Paper 1.21.1+ (built against the 1.21.4 API) |
| Java | 21 |
| Required plugins | **ExyliaLib** (`depend`), **PacketEvents** (needed for the screen board) |

Without PacketEvents the plugin still runs: players use the in-world board and
`/chess move <move>`; the menu, tables, clocks and ratings all work.

## Building

```sh
cd LifeaChess
mvn clean package
```

`chess-engine` builds anywhere (Maven Central only). `chess-plugin` needs the PaperMC
repository and ExyliaLib:

- ExyliaLib is resolved from JitPack as `com.github.DiGround-s:ExyliaLib:v1.576.0`
  (`provided`). If JitPack cannot build it, clone the repo at tag `v1.576.0`, run
  `./gradlew publishToMavenLocal`, and switch the dependency to `net.exylia:ExyliaLib:1.576.0`.
- The shaded jar is `chess-plugin/target/LifeaChess.jar` (contains the engine; ExyliaLib is
  never shaded).

Run only the verified tests with `mvn -pl chess-engine test`.

## Setting up a table

1. Stand where the board should float and run `/chess admin table create arena1`
   (the board hovers `table.board-height` blocks above this point).
2. Stand on White's chair block and run `/chess admin table setchair arena1 1`.
3. Stand on Black's chair block (opposite side) and run `/chess admin table setchair arena1 2`.
4. The big board appears immediately. Players right-click either chair to sit.

`/chess admin` opens the admin menu (tables, stop all, reload, status). Shift-click a table
in the tables list to remove it.

## Commands

`/chess` opens the menu. Shortcuts:

| Command | Purpose |
| --- | --- |
| `/chess play <player> [3+2] [ranked\|casual]` | Challenge (opens the picker when no control is given) |
| `/chess accept` · `/chess deny` · `/chess cancel` | Handle invitations |
| `/chess queue [3+2] [ranked\|casual]` | Find a match |
| `/chess sit` | Sit at the nearest table |
| `/chess move <e2e4\|Nf3>` | Move by UCI or SAN (fallback / accessibility) |
| `/chess resign` · `/chess draw [decline]` · `/chess rematch` · `/chess leave` | In-match actions |
| `/chess spectate [player]` | Watch (opens the table list without a name) |
| `/chess stats [player]` · `/chess leaderboard` | Ratings |
| `/chess admin [table create\|setchair\|setboard\|remove\|list\|tp\|stop ...]` · `/chess reload` | Admin |

Permissions: `lifeachess.play` (default true), `lifeachess.admin` (default op).

## Configuration

`config.yml` — database, ratings (starting rating, K, provisional K/games, same-opponent
cooldown), invitation expiry, disconnect policy, time-control presets/custom limits, ranked and
casual toggles, spectators, table board height/square size/auto-open/how-to panels, piece
style, theme colours, sounds, animations, disabled worlds, debug.
`messages.yml` — every player-facing string (MiniMessage).
`tables.yml` — generated by the admin commands.

## Status & honesty notes

- `chess-engine` (rules, match core, storage) is complete and verified by 74 JUnit tests.
- `chess-plugin` was written against the documented Paper 1.21 API and the ExyliaLib Screens
  API as described in its docs; it was authored in an environment that could not reach the
  PaperMC/JitPack repositories, so it has not yet been compiled against those jars. Build it
  with `mvn clean package` in a networked environment; any ExyliaLib signature mismatch is
  confined to `ui/ScreenBoardView.java`.
- The in-world board and chairs use Paper display/armour-stand entities (non-persistent) rather
  than ExyliaLib holograms, so they do not depend on unverified API.
- The reference design (ExyliaTabletop) could not be read from the authoring environment; the
  owner's screenshots and written spec were used instead.
