# CoordLeak Project Overview

This document provides an overview of the CoordLeak project, intended to serve as instructional context for future interactions.

## Project Overview

**Name:** CoordLeak

**Purpose:** CoordLeak is a lightweight and modular Minecraft plugin designed for Paper/Spigot servers. Its primary function is to enable players to "leak" (reveal) the coordinates of a random online player for a configurable in-game price, or to "share" their own coordinates with other players. It emphasizes production-grade standards, including robust economy integration, cooldowns, and extensive message customization.

**Technologies:**
*   **Language:** Java 21
*   **Build System:** Apache Maven
*   **Platform:** Paper/Spigot Minecraft Server API
*   **Economy Integration:** Vault (requires a compatible economy plugin)
*   **Placeholder Support:** PlaceholderAPI (optional)
*   **Rich Text Formatting:** Adventure library with MiniMessage
*   **Logging:** Custom AuditLogger for in-game actions

**Architecture:** The plugin features a clean, modular architecture utilizing dedicated manager classes for various functionalities, including `ConfigManager`, `MessageManager`, `ProtectionManager`, and `AuditLogger`. This design promotes separation of concerns and maintainability. Dependency injection is used to provide these managers to other components, such as commands.

## Building and Running

**Building:**
The project uses Maven as its build automation tool. To compile the plugin into a `.jar` file, navigate to the project's root directory and execute the following command:

```bash
mvn clean package
```
This command will clean the target directory, compile the source code, and package it into a `.jar` file located in the `target/` directory.

**Running:**
1.  Ensure you have a Paper or Spigot Minecraft server set up.
2.  Install the [Vault plugin](https://www.spigotmc.org/resources/vault.34315/) and a compatible economy plugin (e.g., EssentialsX) into your server's `plugins/` folder.
3.  (Optional) Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) into your server's `plugins/` folder for dynamic message content.
4.  Place the compiled `coordleak-v0.1-beta3.jar` (or whatever the current version is) from the `target/` directory into your server's `plugins/` folder.
5.  Start or restart your Minecraft server. The plugin will generate `config.yml` and `messages.yml` in its data folder (`plugins/CoordLeak/`).

**Testing:**
Unit and integration tests should be considered for future development.

## Development Conventions

**Language:** The project is developed using Java 21.

**Build System:** Apache Maven is used for project management and build automation.

**Code Style:** The codebase appears to follow standard Java coding conventions. A modular approach is adopted, with functionalities encapsulated within manager classes to enhance organization and maintainability.

**Text Formatting:** All in-game messages leverage the Adventure library with MiniMessage for rich text formatting, allowing for advanced styling and gradients.

**Configuration:**
*   `config.yml`: Contains core plugin settings, including prices, cooldowns, rate limits, audit logging preferences, and admin command protection.
*   `messages.yml`: Provides full customization for all user-facing messages, supporting MiniMessage formatting.
