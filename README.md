# 2D Sandbox (LibGDX)

A Terraria-style 2D sandbox game built with Java 17 + LibGDX.

## Run

```bash
./gradlew desktop:run
```

On Windows:

```bat
gradlew.bat desktop:run
```

## Controls

- `A / D` or `Left / Right`: move
- `Space / W / Up`: jump
- `Mouse Left (hold)`: mine targeted block
- `Mouse Right`: place selected hotbar block
- `Mouse Wheel`: hotbar slot select
- `Shift + Mouse Wheel`: zoom camera
- `-` / `+`: zoom camera
- `F3`: toggle debug overlay
- `Esc`: pause / resume
- `M` (in pause): return to menu

## Implemented Systems

- Chunked infinite horizontal world (`16x256` chunks)
- Procedural terrain + caves + trees (noise-based)
- Custom AABB collision and gravity physics
- Mining + placement with mouse raycast targeting
- 9-slot hotbar inventory with stack size 64
- Basic underground light falloff
- Chunk JSON save/load + player save/load
- Lazy chunk streaming around player
- HUD + debug overlay
