import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

public class Main {
    private static final String INSTALLER_HELP_MESSAGE = "Use install <package name>";
    private static final String ALREADY_INSTALLED = " is already installed";
    private static final String REMOVE_HELP_MESSAGE = "Use remove <package name>";

    private static void printHelp() {
        System.out.println(INSTALLER_HELP_MESSAGE);
        System.out.println("OR");
        System.out.println(REMOVE_HELP_MESSAGE);
    }
    private static void error(String msg) {
        System.out.println(msg);
    }
    // Delete a directory with whatever it contains
    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }
    public static void main(String[] args) throws IOException, GitAPIException {
        String homeDir = PackageManager.getHomeDirectory();
        ArgParser argParser = new ArgParser(args);
        if (argParser.getArgs().size() < 1) {
            printHelp();
        } else {
            if (argParser.getArgs().get(0).equals("install") || argParser.getArgs().get(0).equals("i")) {
                if (argParser.getArgs().size() != 2) {
                    // Check that the package file name is passed
                    error(INSTALLER_HELP_MESSAGE);
                } else {
                    // parse the config file (YAML)
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    mapper.findAndRegisterModules();
                    Package aPackage = null;
                    try {
                        aPackage = mapper.readValue(new File(argParser.getArgs().get(1)), Package.class);
                    } catch (FileNotFoundException e) {
                        try {
                            argParser.getArgs().set(1, argParser.getArgs().get(1) + ".yaml");
                            aPackage = mapper.readValue(new File(argParser.getArgs().get(1)), Package.class);
                        } catch (FileNotFoundException e1) {
                            packageNotFound(argParser.getArgs().get(1));
                        }
                    }
                    File packDir = new File(homeDir + "/" + aPackage.getName());
                    if (packDir.exists()) {
                        if (argParser.getItems().contains("u") || argParser.getItems().contains("update")) {
                            main(new String[] {"remove", aPackage.getName()}); // remove previous package
                            main(new String[] {"install", argParser.getArgs().get(1)}); // install new one
                        } else {
                            error("\"" + aPackage.getName() + "\"" + ALREADY_INSTALLED);
                            versionDifferenceLog(aPackage.getVersion(), PackageManager.getPackage(aPackage.getName()).getVersion());
                        }
                    } else {
                        // check dependencies
                        if (aPackage.getDependencies() != null) {
                            for (String dep : aPackage.getDependencies()) {
                                try {
                                    PackageManager.getPackage(dep);
                                } catch (FileNotFoundException ignored) {
                                    main(new String[]{"install", dep});
                                }
                            }
                        }
                        packDir.mkdirs();
                        for (String i : aPackage.getFiles()) {
                            if (i.startsWith("git ")) {
                                Git.cloneRepository()
                                        .setURI(i.replace("git ", "").replace(" subm", ""))
                                        .setDirectory(new File(homeDir + "/" + aPackage.getName()))
                                        .setCloneSubmodules(i.contains("subm"))
                                        .call();
                            } else if (i.startsWith("download ")) {
                                URL website = new URL(i.replace("download ", ""));
                                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                                FileOutputStream fos = new FileOutputStream(homeDir + "/" + aPackage.getName() + "/" + i.split("/")[i.split("/").length - 1]);
                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                            } else if (i.startsWith("unzip ")) {
                                Zip.unzip(homeDir + "/" + aPackage.getName() + "/" + i.replace("unzip ", ""), homeDir + "/" + aPackage.getName() + "/" + i.replace("unzip ", "").replace(".zip", "").split("/")[i.split("/").length - 1]);
                            }
                        }
                        Files.copy(new File(argParser.getArgs().get(1)).toPath(), new File(homeDir + "/" + aPackage.getName() + "/" + aPackage.getName() + ".pkgconf").toPath());
                    }
                }
            } else if (argParser.getArgs().get(0).equals("remove")) {
                if (argParser.getArgs().size() != 2) {
                    error(REMOVE_HELP_MESSAGE);
                } else {
                    deleteDir(new File(homeDir + "/" + argParser.getArgs().get(1)));
                }
            }
        }
    }

    private static void packageNotFound(String s) {
        System.out.println("no file found for package: " + s);
        System.exit(1);
    }

    private static void versionDifferenceLog(String version, String version1) {
        System.out.println("installed version is: " + (version1 == null? "unspecified":version1) + " and you are going to install " + version);
        System.out.println("run the same command with -u or --update to update the package");
    }
}
