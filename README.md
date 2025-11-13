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

# --- Cooldowns (in milliseconds) ---
# Time a player must wait between uses of a command.
cooldowns:
  leak: 5000 # 5 seconds
  share: 2000 # 2 seconds
  reload: 10000 # 10 seconds for admin reload command
  setprice: 5000 # 5 seconds for admin setprice command

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
# General messages
prefix: "&7[&bCoordLeak&7] "
no-permission: "&cYou don't have permission to do that."
player-only-command: "&cThis command can only be run by a player."
configError: "&cConfig error, please check your config."
invalidArgument: "&eInvalid argument. Please check your command."

# --- Command Feedback Messages ---
command-cooldown: "&cYou are on cooldown for this command. Please wait %time%."
command-rate-limited: "&cYou are sending commands too fast. Please slow down."
global-rate-limited: "&cThe server is currently under high load. Please try again in a moment."
daily-limit-exceeded: "&cYou have exceeded your daily limit for this command."
player-blacklisted: "&cYou are blacklisted from using this feature."
player-not-whitelisted: "&cYou are not whitelisted to use this feature."

# --- Economy Messages ---
economy-error: "&cAn economy error occurred. Please contact an administrator."
insufficient-funds: "&cYou do not have enough money to do that. You need %amount%."

# --- Price Related Messages ---
invalid-price: "&cInvalid price. Please enter a valid number."
price-out-of-range: "&cThe price must be between %min_price% and %max_price%."
currentPrice: "&aThe current price for leaking coordinates is: &e%price%&a."
setprice-success: "&aPrice for leaking coordinates set to &e%price%&a."

# --- Target Related Messages ---
invalidPlayer: "&cThat player is not online or does not exist."
cannotTargetYourself: "&cYou cannot target yourself!"
target-not-online: "&cThe target player is not online."
target-excluded-world: "&cYou cannot leak coordinates in this world."
target-excluded-permission: "&cThis player is protected from coordinate leaks."
target-no-consent: "&cThe target player has not consented to share their coordinates."
no-leak-target-found: "&cNo suitable player found to leak coordinates."

# --- Admin Command Confirmation Messages ---
reload-confirm-required: "&ePlease confirm your action by typing &6/coord %command% confirm &ewithin %time% seconds."
reload-confirmed: "&aAction confirmed. Plugin reloaded successfully!"
reload-cancelled: "&cConfirmation expired or cancelled."
admin-action-logged: "&eAdmin action logged."

# --- Leak Command Messages ---
leak-success: "&aYou successfully leaked &e%player%'s &acoordinates: &e%coords%&a."
leak:
  exposed: "<red>Your location has been leaked!"

# --- Share Command Messages ---
share-success-sender: "&aYou shared &e%player%'s &acoordinates with &e%target%&a."
share-success-target: "&a%sender% &ashared &e%player%'s &acoordinates with you: &e%coords%&a."

# --- Info Command Layout ---
info:
  layout:
    - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
    - "<white><gradient:#29E7D7:#FFFFFF>CoordLeak Info</gradient>"
    - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
    - "<gray>• <white>Version: <aqua>%plugin_version%</aqua>"
    - "<gray>• <white>Author: <aqua>%plugin_author%</aqua>"
    - "<gray>• <white>Default Price: <gold>%leak_price%</gold>"
    - "<gray>• <white>Leak Cooldown: <yellow>%leak_cooldown%</yellow>"
    - "<gray>• <white>PlaceholderAPI: %papi_status%"
    - "<gray>• <white>Vault: %vault_status%"
    - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
  enabled: "<green>Enabled</green>"
  disabled: "<red>Disabled</red>"

# --- Internal Logging Messages ---
global-limit-exceeded-log: "&c[CoordLeak] Global rate limit exceeded. Blocking requests for %time% seconds."
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