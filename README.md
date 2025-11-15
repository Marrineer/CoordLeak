# CoordLeak

A lightweight and modular Minecraft plugin for Paper/Spigot servers that allows players to "leak" (reveal) random online player coordinates for a configurable price, or "share" their own coordinates with others. Built with production-grade standards, featuring robust economy integration, cooldowns, and extensive message customization.

## Features

*   **Coordinate Leaking:** Players can pay a configurable price to get the coordinates (X, Z, and dimension) of a random online player.
*   **Coordinate Sharing:** Players can share their current location with another player, optionally including a custom message.
*   **Vault Economy Integration:** Seamlessly integrates with Vault-compatible economy plugins for in-game currency transactions.
*   **PlaceholderAPI Support:** Utilize custom placeholders (`%coordleak_posx%`, `%coordleak_posy%`, `%coordleak_posz%`, `%coordleak_dimension%`) in messages for dynamic content.
*   **Per-Player Cooldowns:** Prevent command spamming with configurable cooldowns for the "leak" feature.
*   **Admin Price Management:** Administrators can easily set and view the price for coordinate leaking via a dedicated command.
*   **Config Reload:** Safely reload plugin configurations and messages without server restarts.
*   **Fully Customizable Messages:** All plugin messages are configurable via `messages.yml`, supporting MiniMessage for rich text formatting.
*   **Tab Completion:** Intuitive tab completion for all commands and subcommands.
*   **Modular & Efficient:** Designed with a clean, modular architecture using dependency injection for optimal performance and maintainability.

## Commands

| Command                               | Description                                                              | Permission             | Default       |
| :------------------------------------ | :----------------------------------------------------------------------- | :--------------------- | :------------ |
| `/coord leak`                         | Reveals the coordinates of a random online player for a set price.       | `coordleak.leak`       | ✅ All players |
| `/coord share <player> [custom_text]` | Shares your current location with `<player>`, with an optional message.  | `coordleak.share`      | ✅ All players |
| `/coord reload [confirm]`             | Reloads the plugin's `config.yml` and `messages.yml`.                    | `coordleak.admin`      | ❌ OP only     |
| `/coord setprice <amount> [confirm]`  | Sets the price for the `/coord leak` command. If no amount, shows current. | `coordleak.setprice`   | ❌ OP only     |

## Permissions

| Permission             | Description                                                              | Default       |
| :--------------------- | :----------------------------------------------------------------------- | :------------ |
| `coordleak.admin`      | Grants access to admin commands (`/coord reload`) and bypasses most protections. | ❌ OP only     |
| `coordleak.leak`       | Allows players to use the `/coord leak` command.                         | ✅ All players |
| `coordleak.share`      | Allows players to use the `/coord share` command.                        | ✅ All players |
| `coordleak.setprice`   | Allows administrators to use the `/coord setprice` command.              | ❌ OP only     |
| `coordleak.bypass.cooldown` | Bypasses command cooldowns.                                              | ❌ OP only     |
| `coordleak.bypass.ratelimit` | Bypasses command rate limits.                                            | ❌ OP only     |
| `coordleak.bypass.dailylimit` | Bypasses daily command limits.                                           | ❌ OP only     |
| `coordleak.protected`  | Protects a player from being leaked/shared.                              | ❌ OP only     |

## Configuration

### `config.yml`

```yaml
# Prefix for all plugin messages. Supports MiniMessage formatting.
prefix: "<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>"

# --- Price Settings ---
price:
  # Default price for leaking coordinates.
  default: 50.0
  # Minimum allowed price.
  min: 1.0
  # Maximum allowed price.
  max: 10000.0

# --- Cooldowns (in seconds) ---
# Time a player must wait between uses of a command.
cooldowns:
  leak: 5 # 5 seconds
  share: 2 # 2 seconds
  setprice: 5 # 5 seconds

# --- Rate Limits (per-player, sliding window) ---
# Limits how many times a player can use a command within a specific window.
ratelimit:
  leak:
    limit: 5 # Max 5 uses
    window: 60000 # within 60 seconds (60000 ms)
  share:
    limit: 10 # Max 10 uses
    window: 30000 # within 30 seconds (30000 ms)
  setprice:
    limit: 1 # Max 1 use
    window: 10000 # within 10 seconds (10000 ms)

# --- Global Rate Limit (server-wide anti-flood) ---
# Limits total requests across the server to prevent abuse.
global:
  ratelimit:
    enabled: true
    limit: 200 # Max 200 requests
    window: 60000 # within 60 seconds (60000 ms)
    # How long to block all new requests if the global limit is exceeded (in milliseconds).
    block-duration: 10000 # 10 seconds

# --- Daily Limits (per-player) ---
# Limits how many times a player can use a command per day.
limits:
  daily:
    leak: 50 # Max 50 leaks per player per day
    share: 100 # Max 100 shares per player per day

# --- Audit Logging ---
audit:
  enabled: true
  # Path to the audit log file, relative to the plugin's data folder.
  # Will automatically roll daily.
  log-file: "logs/coordleak.log"
  # Set to true to log sensitive information like player IP addresses.
  log-sensitive: false

# --- Admin Command Protection ---
reload:
  # Whether admin commands like /coord reload and /coord setprice require a 'confirm' argument.
  require-confirm: true
  # Time (in milliseconds) a player has to confirm an admin command.
  confirmation-timeout: 10000 # 10 seconds

# --- Blacklist / Whitelist ---
blacklist:
  enabled: false
  # List of player UUIDs who are blacklisted from using certain features.
  uuids:
    - "UUID_OF_PLAYER_1"
    - "UUID_OF_PLAYER_2"
whitelist:
  enabled: false
  # List of player UUIDs who are whitelisted to use certain features (if enabled).
  uuids:
    - "UUID_OF_PLAYER_3"
    - "UUID_OF_PLAYER_4"

# --- Target Validation ---
target:
  # List of world names where coordinates cannot be leaked/shared.
  exclude-worlds:
    - "admin_world"
    - "event_world"
  # List of permissions that protect a player from having their coordinates leaked/shared.
  exclude-permissions:
    - "coordleak.protected"
    - "coordleak.staff"
  # If true, players must explicitly consent to have their coordinates shared (TODO: consent mechanism).
  require-consent-for-share: false
```

### `messages.yml`

All messages are fully customizable and support [MiniMessage](https://docs.adventure.kyori.net/minimessage/format.html) formatting.

```yaml
# CoordLeak Plugin Messages - Enhanced by Gemini
# All messages support MiniMessage format. For a full guide, visit: https://docs.advntr.dev/minimessage/format.html
# Use the {prefix} placeholder to insert the prefix defined in config.yml.

# --- General Messages ---
no-permission: "{prefix} <red>You lack the required permission to perform this action."
player-only-command: "{prefix} <red>This command is for players only."
config-error: "{prefix} <dark_red>A configuration error was detected. Please notify an administrator."
invalid-argument: "{prefix} <yellow>Invalid argument. Use <white>/coord help</white> for command usage."

# --- Command Feedback Messages ---
command-cooldown: "{prefix} <gray>Please wait <yellow>%time%</yellow> before using this command again."
command-rate-limited: "{prefix} <gold>You're doing that a bit too fast. Please wait a moment."
global-rate-limited: "{prefix} <red>The server is currently limiting this action due to high usage. Please try again shortly."
daily-limit-exceeded: "{prefix} <red>You have reached your daily usage limit for this command."
player-blacklisted: "{prefix} <dark_red>You are restricted from using this feature."
player-not-whitelisted: "{prefix} <red>This feature is currently restricted to whitelisted players."

# --- Economy Messages ---
economy-error: "{prefix} <dark_red>Economy service not available. Please report this to an admin."
insufficient-funds: "{prefix} <red>Not enough funds. You need <gold>%amount%</gold> to do this."

# --- Price Related Messages ---
invalid-price: "{prefix} <red>Invalid price. Please specify a valid number."
price-out-of-range: "{prefix} <red>The price must be between <yellow>%min_price%</yellow> and <yellow>%max_price%</yellow>."
current-price: "{prefix} <green>The current price for leaking coordinates is <gold>%price%</gold>."
setprice-success: "{prefix} <green>Successfully set the coordinate leak price to <gold>%price%</gold>."

# --- Target Related Messages ---
invalid-player: "{prefix} <red>The specified player does not exist or is not online."
cannot-target-yourself: "{prefix} <yellow>You can't select yourself for this action!"
target-not-online: "{prefix} <red>The target player is no longer online."
target-excluded-world: "{prefix} <red>This action is not permitted in the target's current world."
target-excluded-permission: "{prefix} <red>You cannot target this player; they are protected."
target-no-consent: "{prefix} <red>The target player has not provided consent for this action."
no-leak-target-found: "{prefix} <yellow>Could not find a suitable random player to target."

# --- Admin Command Confirmation Messages ---
reload-confirm-required: "{prefix} <hover:show_text:'<gray>Click to confirm or type <white>/coord %command% confirm</white>'><yellow>Please confirm this action by typing <gold>/coord %command% confirm</gold> within <white>%time%s</white>.</hover>"
reload-confirmed: "{prefix} <green>Action confirmed. The plugin has been reloaded successfully."
reload-cancelled: "{prefix} <red>Confirmation timed out. The action was cancelled."
admin-action-logged: "{prefix} <dark_gray><i>This administrative action has been logged.</i>"

# --- Leak Command Messages ---
leak-success:
  - "{prefix} <white>A random player has been selected!"
  - "{prefix} <white>Target: <cyan>%player%"
  - "{prefix} <white>Coord: <cyan>X=%coordleak_posx%, Z=%coordleak_posz%"
  - "{prefix} <white>Dimension: <cyan>%coordleak_dimension%"
leak-exposed: "<dark_red><sound:entity.wither.spawn:0.8:1>A chill runs down your spine... your location has been exposed!</dark_red>"

# --- Share Command Messages ---
share-success-sender: "{prefix} <green>You shared your coordinates with <yellow>%target%</yellow>."
share-success-target: "{prefix} <yellow>%sender%</yellow> <green>shared their location with you: <aqua><hover:show_text:'<gray>Click to copy coordinates to chat!'><click:suggest_command:'%raw_coords%'>%coords%</click></hover></aqua>.<light_purple><sound:entity.experience_orb.pickup:1:1.5>"

# --- Consent Messages ---
consent:
  request: |-
    {prefix} <gray>%sender% wants to share coordinates with you.
    {prefix} <gray>This request expires in %time% seconds.
    {prefix} <click:run_command:'/coord consent accept %id%'><hover:show_text:'<green>Click to accept'><green>   [Accept]   </green></hover></click><click:run_command:'/coord consent deny %id%'><hover:show_text:'<red>Click to deny'><red>   [Deny]   </red></hover></click>
  denied: "{prefix} <red>Your coordinate share request to <yellow>%target%</yellow> was denied."
  accepted: "{prefix} <green>Your coordinate share request to <yellow>%target%</yellow> was accepted."
  invalid: "{prefix} <red>This consent request is no longer valid."
  
# --- Info Command Layout ---
info:
  layout:
    - "<st><gradient:#4ECDC4:#29E7D7>                                        </gradient></st>"
    - "<dark_aqua> ● <white>Plugin<dark_gray>:</dark_gray> <gradient:#FFFFFF:#29E7D7>CoordLeak</gradient>"
    - "<dark_aqua> ● <white>Version<dark_gray>:</dark_gray> <aqua>%plugin_version%</aqua>"
    - "<dark_aqua> ● <white>Author<dark_gray>:</dark_gray> <aqua>%plugin_author%</aqua>"
    - ""
    - "<dark_aqua> ● <white>Leak Price<dark_gray>:</dark_gray> <gold>%leak_price%</gold>"
    - "<dark_aqua> ● <white>Leak Cooldown<dark_gray>:</dark_gray> <yellow>%leak_cooldown%</yellow>"
    - ""
    - "<dark_aqua> ● <white>Placeholders<dark_gray>:</dark_gray> %papi_status%"
    - "<dark_aqua> ● <white>Economy<dark_gray>:</dark_gray> %vault_status%"
    - "<st><gradient:#29E7D7:#4ECDC4>                                        </gradient></st>"
  enabled: "<green>● Enabled</green>"
  disabled: "<red>● Disabled</red>"

# --- Internal Logging Messages ---
# This message is intended for the console log, not for players.
global-limit-exceeded-log: "[CoordLeak] Global rate limit of %limit%/%window%ms exceeded. Blocking new requests for %block_duration%ms."
```

## Placeholders (via PlaceholderAPI)

| Placeholder           | Description                      |
| :-------------------- | :------------------------------- |
| `%coordleak_posx%`    | Player's X coordinate            |
| `%coordleak_posy%`    | Player's Y coordinate            |
| `%coordleak_posz%`    | Player's Z coordinate            |
| `%coordleak_dimension%` | Player's current world/dimension |

## Dependencies

*   **Vault** (Required) - For economy integration.
*   **PlaceholderAPI** (Optional) - For custom placeholders in messages.

## Contributing

Pull requests are welcome! Feel free to fork the repository, improve the code, or suggest new ideas.

## License

This project is licensed under the GNU General Public License v3.0.

---
**Author:** qhuy

**Version:** v0.1-beta3