<div align="center">

# ☁️ CloudflaredBridge 🌉

![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Spigot%20|%20Folia%20|%20Velocity-blue?style=for-the-badge&logo=minecraft)
![License](https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge)

**Seamlessly connect your local Minecraft server to the global Cloudflare network.**
<br>
*No port forwarding. No IP leaks. Pure performance.*

</div>

---

## 🚀 Why CloudflaredBridge?

**CloudflaredBridge** is the ultimate utility for server administrators who want to use [Cloudflare Tunnel](https://www.cloudflare.com/products/tunnel/) without the headache of managing external binaries or complex shell scripts.

It acts as a wrapper that automatically downloads, verifies, and manages the lifecycle of the `cloudflared` process directly from your Minecraft server console.

### ✨ Key Features

*   **🤖 Smart Automation**: Automatically detects your OS (Windows, Linux, macOS) and Architecture (amd64, arm64, x86) to fetch the correct binary.
*   **🛡️ Iron-Clad Security**:
    *   **Strict SHA256 Verification**: Every download is cryptographically verified against official Cloudflare hashes before execution.
    *   **Auto-Healing**: If the binary is corrupted or tampered with, it is immediately deleted and re-downloaded.
*   **⚡ Folia & Velocity Native**: Built from the ground up with **Async** architecture. No main-thread blocking, making it 100% safe for **Folia**'s region threading and **Velocity** proxies.
*   **🔄 Multi-Tunnel Support**: Run multiple tunnels simultaneously with a single plugin.
*   **⏱️ Rate-Limit Protection**: Configurable delay between tunnel starts to prevent API rate-limiting when running multiple tokens.

---

## 💡 Use Cases

**CloudflaredBridge** is versatile and can be used to protect various services associated with your Minecraft server:

*   **🗺️ Map Security (Dynmap / BlueMap)**: Expose your web-based maps to the internet without opening any ports on your router. Benefit from Cloudflare's caching and DDoS protection.
*   **📊 Web Panels (Plan / Pterodactyl)**: Securely access your player analytics or management panels through a Cloudflare-protected domain.
*   **🔗 Bypassing CGNAT / No Port Forwarding**: Perfect for servers running on home connections where port forwarding is not possible or restricted.
*   **🛡️ DDoS Mitigation**: Hide your server's true IP address behind Cloudflare's global edge network, making it significantly harder for attackers to target your infrastructure directly.

---

## 📦 Installation

1.  **Download** the plugin jar from the [Releases](#) tab.
    *   `cloudflaredbridge-spigot-1.0.0.jar` (Works on Spigot, Paper, Folia)
    *   `cloudflaredbridge-velocity-1.0.0.jar` (Works on Velocity Proxy)
2.  **Drop** it into your server's `plugins` folder.
3.  **Restart** your server.
4.  The plugin will automatically create a `cloudflared` folder in your server root.

---

## ⚙️ Configuration

Located at `cloudflared/config.yml`.

```yaml
# List your Cloudflare Tunnel tokens here.
# You can get these from the Cloudflare Zero Trust Dashboard.
tokenlist:
  - "eyJhIjoi..." # Main Server Tunnel
  - "eyJhIjoi..." # Map/Dynmap Tunnel (Optional)
```

---

## 🖥️ Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/cloudflared start [--delay=X]` | `cloudflaredbridge.admin` | **Starts** all configured tunnels. <br>• Checks for updates & SHA256 validity first.<br>• `X` is delay in seconds (default: 10s, min: 5s). |
| `/cloudflared stop` | `cloudflaredbridge.admin` | **Stops** all running cloudflared processes gracefully. |

### Example Usage

Start tunnels with a safe 15-second gap between them:
```bash
/cloudflared start --delay=15
```

---

## 🛠️ How it Works

1.  **Initialization**: When you run `/cloudflared start`, the plugin queries the GitHub API for the latest `cloudflared` release.
2.  **Verification**: It compares the local file's SHA256 hash with the official hash from the release notes.
3.  **Update/Repair**: If the file is missing or the hash doesn't match, the secure version is downloaded automatically.
4.  **Execution**: The process is launched in the background, independent of the Minecraft server thread, ensuring zero lag.

---

## 🏗️ Building from Source

Requirements: **JDK 17+** and **Maven**.

```bash
git clone https://github.com/vanes430/CloudflaredBridge.git
cd CloudflaredBridge
mvn clean install
```

The final artifacts will be located in the `target/` directory.

---

<div align="center">
Made by [vanes430](https://github.com/vanes430)
</div>
