package tk.zwander.oneuituner;

import tk.zwander.oneuituner.SuRunner;

interface RootBridge {
    void reboot(String reason);

    void installPkg(String path, String name);
    void uninstallPkg(String pkg);

    void setSuRunner(SuRunner runner);
}
