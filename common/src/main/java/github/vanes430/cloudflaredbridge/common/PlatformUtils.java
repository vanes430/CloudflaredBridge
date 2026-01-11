package github.vanes430.cloudflaredbridge.common;

import java.util.Locale;

public class PlatformUtils {

    public enum OS {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public enum Arch {
        AMD64,
        ARM64,
        X86,
        UNKNOWN
    }

    public static OS getOS() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
            return OS.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return OS.MACOS;
        }
        return OS.UNKNOWN;
    }

    public static Arch getArch() {
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            return Arch.AMD64;
        } else if (osArch.contains("arm64") || osArch.contains("aarch64")) {
            return Arch.ARM64;
        } else if (osArch.contains("x86") || osArch.contains("i386")) {
            return Arch.X86;
        }
        return Arch.UNKNOWN;
    }
}
