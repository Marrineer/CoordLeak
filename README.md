CoordLeak
=========

CoordLeak is a lightweight Minecraft plugin that lets players purchase "usages" with in-game currency to reveal the coordinates of a random online player.

This is a small hobby project — built for fun.

Features
--------

*   Leak the coordinates (X, Z, and dimension) of a random player.
*   Economy integration with Vault to buy usages.
*   Share your coord to a player
*   Configurable messages and prefix.
*   Simple, lightweight, and easy to use.

Commands & Permissions
----------------------

| Command       | Description                            | Permission        | Default     |
|---------------|----------------------------------------|-------------------|-------------|
| /coord leak   | Leak coordinates of a random player    | coordleak.use     | ✅ All players |
| /coord reload | Reload config and messages             | coordleak.admin   | ❌ OP only    |

Configuration
-------------

### config.yml
```yaml
prefix: "<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>"

price: 1000

settings:
  cooldown-per-usage: 300 # SECOND

  clean-interval: 60 # SECOND
```

### messages.yml
Uses MiniMessage format :D
```yaml
# Thêm prefix vào các message quan trọng
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
cooldownMessage: "<red>You are on cooldown. Please wait before using this command again.</red>"

help:
  - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
  - " <white><gradient:#29E7D7:#FFFFFF>CoordLeak Commands</gradient>"
  - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"
  - " <gray>• <click:suggest_command:'/coord use'><white>/coord use</white></click> <gray>- Leak random player's location</gray>"
  - " <gray>• <click:suggest_command:'/coord reload'><white>/coord reload</white></click> <gray>- Reload plugin config</gray>"
  - " <gray>• <click:suggest_command:'/coord'><white>/coord</white></click> <gray>- Show this help menu</gray>"
  - "<gradient:#FF6B6B:#4ECDC4>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</gradient>"

randomSelect:
  - "<white>A random player has been selected!"
  - "<white>Target: <cyan>%player_name%"
  - "<white>Coord: <cyan>X=%coordleak_posx%, Z=%coordleak_posz%"
  - "<white>Dimension: <cyan>%coordleak_dimension%"

leak:
  exposed: "<red>Your location has been leaked!"
```

Dependencies
------------

- Vault (Required) — for economy integration.
- PlaceholderAPI (Optional) - for custom placeholder.
- Essentials (Optional) — soft dependency.

Placeholders
------------

| Placeholder             | Description                          |
|------------------------|--------------------------------------|
| %coordleak_posx%       | Player's X coordinate                |
| %coordleak_posz%       | Player's Z coordinate                |
| %coordleak_dimension%  | Player's current world/dimension     |

Notes
-----

- Permission & message system is still a work in progress.
- I'm just a Java beginner — code might be messy.

Contributing
------------

Pull requests are welcome! Feel free to fork, improve, or suggest new ideas.

License
-------

This project is licensed under the GNU General Public License v3.0.

