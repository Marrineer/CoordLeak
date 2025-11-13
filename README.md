# CoordLeak

A lightweight and modular Minecraft plugin for Paper/Spigot servers that allows players to "leak" (reveal) random online player coordinates for a configurable price, or "share" their own coordinates with others. Built with production-grade standards, featuring robust economy integration, cooldowns, and extensive message customization.

## ✨ Features

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

## 🚀 Commands

| Command                               | Description                                                              | Permission             | Default       |
| :------------------------------------ | :----------------------------------------------------------------------- | :--------------------- | :------------ |
| `/coord leak`                         | Reveals the coordinates of a random online player for a set price.       | `coordleak.leak`       | ✅ All players |
| `/coord share <player> [custom_text]` | Shares your current location with `<player>`, with an optional message.  | `coordleak.share`      | ✅ All players |
| `/coord reload`                       | Reloads the plugin's `config.yml` and `messages.yml`.                    | `coordleak.admin`      | ❌ OP only     |
| `/setprice [amount]`                  | Sets the price for the `/coord leak` command. If no amount, shows current. | `coordleak.setprice`   | ❌ OP only     |

## 🔒 Permissions

| Permission             | Description                                                              | Default       |
| :--------------------- | :----------------------------------------------------------------------- | :------------ |
| `coordleak.admin`      | Grants access to admin commands (`/coord reload`) and bypasses leak costs/cooldowns. | ❌ OP only     |
| `coordleak.leak`       | Allows players to use the `/coord leak` command.                         | ✅ All players |
| `coordleak.share`      | Allows players to use the `/coord share` command.                        | ✅ All players |
| `coordleak.setprice`   | Allows administrators to use the `/setprice` command.                    | ❌ OP only     |

## ⚙️ Configuration

### `config.yml`

```yaml
# Prefix for all plugin messages. Supports MiniMessage formatting.
prefix: "<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>"

# The price players must pay to use the /coord leak command.
price: 1000.0

settings:
  # Cooldown duration in seconds for the /coord leak command per player.
  cooldown-per-usage: 300 

  # Interval in seconds for the plugin to clean up expired cooldown entries.
  clean-interval: 300 
```

### `messages.yml`

All messages are fully customizable and support [MiniMessage](https://docs.adventure.kyori.net/minimessage/format.html) formatting.

```yaml
# General messages
permission: "<red>You don't have permission to do this!</red>"
configError: "<red>Config error, please check your config</red>"
invalidArgument: "<yellow>Invalid argument. Please check your command.</yellow>"
invalidPlayer: "<yellow>That player is not online or does not exist.</yellow>"
notEnoughBalance: "<red>You don't have enough balance.</red>"
buySuccessfully: "<green>You have successfully purchased</green>"
noUsageLeft: "<red>You have no more uses left for this command. Visit the shop to get more!</red>"
configReloaded: "<green>Config reloaded successfully</green>"
noOneIsOnline: "<red>No players are currently online.</red>"
setSuccess: "<green>Set usage count successfully</green>"
onlyPlayer: "<red>Only player can use this command</red>"
cannotTargetYourself: "You cannot target yourself!"
cooldownMessage: "<red>You are on cooldown. Please wait before using this command again.</red>"
currentPrice: "<green>The current price is: %price%</green>"
priceNegative: "<red>Price cannot be negative.</red>"
priceSet: "<green>Price has been set to: %price%</green>"
invalidNumber: "<red>Invalid number format.</red>"

# Help message for the /coord command
help:
  - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
  - "<white><gradient:#29E7D7:#FFFFFF>CoordLeak Commands</gradient>"
  - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
  - "<gray>• <click:suggest_command:'/coord leak'><white>/coord leak</white></click> <gray>- Leak random player's location</gray>"
  - "<gray>• <click:suggest_command:'/coord share <player> <text>'><white>/coord share</white></click> <gray>- Share your location</gray>"
  - "<gray>• <click:suggest_command:'/coord reload'><white>/coord reload</white></click> <gray>- Reload plugin config</gray>"
  - "<gray>• <click:suggest_command:'/setprice <amount>'><white>/setprice <amount></white></click> <gray>- Set the price for leaking coordinates</gray>"
  - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"

# Messages for when a random player is selected for leaking
randomSelect:
  - "<white>A random player has been selected!"
  - "<white>Target: <cyan>%player_name%"
  - "<white>Coord: <cyan>X=%coordleak_posx%, Z=%coordleak_posz%"
  - "<white>Dimension: <cyan>%coordleak_dimension%"

# Message sent to the player whose location was leaked
leak:
  exposed: "<red>Your location has been leaked!"

# Messages for when a player shares their coordinates
shareCoord:
  - "<cyan>%player_name% Shared their position"
  - "➤ <cyan>X=%coordleak_posx% | Y=%coordleak_posy% | Z=%coordleak_posz%"
  - "<white>Dimension: <cyan>%coordleak_dimension%"
```

## 🧩 Placeholders (via PlaceholderAPI)

| Placeholder           | Description                      |
| :-------------------- | :------------------------------- |
| `%coordleak_posx%`    | Player's X coordinate            |
| `%coordleak_posy%`    | Player's Y coordinate            |
| `%coordleak_posz%`    | Player's Z coordinate            |
| `%coordleak_dimension%` | Player's current world/dimension |

## 📦 Dependencies

*   **Vault** (Required) - For economy integration.
*   **PlaceholderAPI** (Optional) - For custom placeholders in messages.

## 🛠️ Installation

1.  Download the latest version of CoordLeak from the [releases page](link_to_releases_page_here).
2.  Place the `CoordLeak-vX.X.X.jar` file into your server's `plugins/` folder.
3.  Ensure you have **Vault** and a compatible economy plugin (e.g., EssentialsX) installed.
4.  (Optional) Install **PlaceholderAPI** for full message customization.
5.  Start or restart your server.
6.  The `config.yml` and `messages.yml` files will be generated in the `plugins/CoordLeak/` folder. Customize them to your liking.

## 🤝 Contributing

Pull requests are welcome! Feel free to fork the repository, improve the code, or suggest new ideas.

## 📄 License

This project is licensed under the GNU General Public License v3.0.

---
**Author:** qhuy
**Version:** v0.1-beta3