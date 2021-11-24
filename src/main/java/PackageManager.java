public class PackageManager {
    public static String getHomeDirectory() {
        // TODO: choose a name for this package manager and the language!
        String homeDir;
        if (System.getenv().get("PackageManagerHOME") != null) {
            homeDir = System.getenv().get("PackageManagerHOME");
        } else {
            homeDir = System.getProperty("user.home") + "/packageManager";
        }
        return homeDir;
    }
}
