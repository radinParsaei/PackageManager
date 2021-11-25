import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

    public static Package getPackage(String packageName) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        return mapper.readValue(new File(getHomeDirectory() + "/" + packageName + "/" + packageName + ".pkgconf"), Package.class);
    }

    public static File getPackageDirectory(String packageName) {
        return new File(getHomeDirectory() + "/" + packageName);
    }

    public static File getPackageDirectory(Package aPackage) {
        return new File(getHomeDirectory() + "/" + aPackage.getName());
    }

    public static ArrayList<Package> findPackageWithTags(String... tags) {
        ArrayList<Package> res = new ArrayList<>();
        File[] files = new File(getHomeDirectory()).listFiles();
        if (files == null) return res;
        for (File file : files) {
            if (!file.isDirectory()) continue;
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            Package aPackage;
            try {
                aPackage = mapper.readValue(new File(file.getPath() + "/" + file.getName() + ".pkgconf"), Package.class);
            } catch (IOException e) {
                continue;
            }
            boolean add = true;
            for (String tag : tags) {
                if (!aPackage.getTags().contains(tag.toLowerCase())) {
                    add = false;
                    break;
                }
            }
            if (add) {
                res.add(aPackage);
            }
        }
        return res;
    }
}
