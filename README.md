CoordLeak
=========

CoordLeak is a lightweight Minecraft plugin that lets players purchase "usages" with in-game currency to reveal the coordinates of a random online player.

This is a small hobby project — built for fun.

Features
--------

*   Leak the coordinates (X, Z, and dimension) of a random player.
*   Economy integration with Vault to buy usages.
*   Configurable messages and prefix.
*   Simple, lightweight, and easy to use.

Commands & Permissions
----------------------

| Command                        | Description                            | Permission        | Default     |
|-------------------------------|----------------------------------------|-------------------|-------------|
| /coord                        | Leak coordinates of a random player    | coordleak.use     | ✅ All players |
| /buyusage                     | Buy one usage of /coord                | coordleak.use     | ✅ All players |
| /setusage <player> <amount>   | Set usage count manually               | coordleak.admin   | ❌ OP only    |
| /creload                      | Reload config and messages             | coordleak.admin   | ❌ OP only    |

Note: `/coord` requires the player to have at least one usage.

Configuration
-------------

### config.yml
```yaml
database:
  # DB Type ( SQLITE / MYSQL )
  type: SQLITE
  # SQLITE
  sqlite-file: data.db
  # MYSQL
  host: localhost
  port: 3306
  name: coordleak_db
  user: root
  password: password

prefix: "<i><gradient:#FFFFFF:#29E7D7>[ Coord ]</gradient></i>"

price: 1000

settings:
  enable-auto-save: true
  auto-save-interval: 600
```

### messages.yml
Uses MiniMessage format :D
```yaml
# Legacy format color codes are not supported, use MiniMessage instead
permission: "<red>You don't have permission to do this!</red>"
configError: "<red>Config error, please check your config"
invalidArgument: "<yellow>Invalid argument. Please check your command.</yellow>"
invalidPlayer: "<yellow>That player is not online or does not exist.</yellow>"
notEnoughBalance: "<red>You don't have enough balance.</red>"
buySuccessfully: "<green>You have successfully purchased</green>"
noUsageLeft: "<red>You have no more uses left for this command. Visit the shop to get more!</red>"
configReloaded: "Config reloaded"
noOneIsOnline: "<red>No players are currently online.</red>"
setSuccess: "<green>Set usage count successfully</green>"
onlyPlayer: "Only player can use this command"

helpFallback:
  setusage: "<yellow>Usage: /setusage <player> <integer>"
  buyusage: "<yellow>Usage: /buyusage <integer>"
  coordusage: "<yellow>Usage: /coord"

randomSelect:
  message: "<white>A random player has been selected!"
  target: "<white>Target: <cyan>%player_name%"
  coord: "<white>Coord: <cyan>X=%coordleak_posx%, Z=%coordleak_posz%"
  dimension: "<white>Dimension: <cyan>%coordleak_dimension%"

leak:
  exposed: "<red>Your location has been leaked!"
```

Dependencies
------------

- Vault (Required) — for economy integration.
- PlaceholderAPI (Required) - for custom placeholder.
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

