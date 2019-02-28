package tk.zwander.oneuituner;

interface RootBridge {
    void reboot(String reason);

    void installPkg(String path, String name);
    void uninstallPkg(String pkg);
}
