import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static final String INSTALLER_HELP_MESSAGE = "Use install <package name>";
    private static final String ALREADY_INSTALLED = " is already installed";
    private static final String REMOVE_HELP_MESSAGE = "Use remove <package name>";
    private static final String ADD_KNOWN_PKG_HELP_MESSAGE = "Use add-known-package <describer file>";

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

    private static void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!sourceDir.isDirectory()) {
            copyFile(sourceDir, destDir);
            return;
        }
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        for (String f : sourceDir.list()) {
            File source = new File(sourceDir, f);
            File destination = new File(destDir, f);
            if (source.isDirectory()) {
                copyDirectory(source, destination);
            } else {
                copyFile(source, destination);
            }
        }
    }

    private static void copyFile(File sourceFile, File destinationFile) throws IOException {
        if (sourceFile.isDirectory()) {
            copyDirectory(sourceFile, destinationFile);
            return;
        }
        FileInputStream input = new FileInputStream(sourceFile);
        FileOutputStream output = new FileOutputStream(destinationFile);
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buf)) > 0) {
            output.write(buf, 0, bytesRead);
        }
        input.close();
        output.close();
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
                    String body = null;
                    if (argParser.getArgs().get(1).startsWith("http://") || argParser.getArgs().get(1).startsWith("https://")) {
                        URL url = new URL(argParser.getArgs().get(1));
                        URLConnection con = url.openConnection();
                        InputStream in = con.getInputStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int len = 0;
                        while ((len = in.read(buf)) != -1) {
                            baos.write(buf, 0, len);
                        }
                        body = baos.toString("UTF-8");
                        aPackage = mapper.readValue(body, Package.class);
                    } else {
                        try {
                            aPackage = mapper.readValue(new File(argParser.getArgs().get(1)), Package.class);
                        } catch (FileNotFoundException e) {
                            try {
                                argParser.getArgs().set(1, argParser.getArgs().get(1) + ".yaml");
                                aPackage = mapper.readValue(new File(argParser.getArgs().get(1)), Package.class);
                            } catch (FileNotFoundException e1) {
                                String pathInRepo = PackageManager.getAliases().get(argParser.getArgs().get(1).substring(0, argParser.getArgs().get(1).length() - 5));
                                if (pathInRepo != null) {
                                    if (argParser.getItems().contains("u"))
                                        main(new String[] {"install", pathInRepo, "-u"});
                                    else
                                        main(new String[] {"install", pathInRepo});
                                    System.exit(0);
                                } else {
                                    packageNotFound(argParser.getArgs().get(1));
                                }
                            }
                        }
                    }
                    File packDir = new File(homeDir + "/" + aPackage.getName());
                    if (packDir.exists()) {
                        if (argParser.getItems().contains("u") || argParser.getItems().contains("update")) {
                            main(new String[] {"remove", aPackage.getName()}); // remove previous package
                            main(new String[] {"install", argParser.getArgs().get(1)}); // install new one
                            return;
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
                                    main(new String[]{"install", dep}); // Install the dependency if it isn't already installed
                                }
                            }
                        }
                        packDir.mkdirs();
                        InstallerFunctions.setPackageName(aPackage.getName());
                        for (String i : aPackage.getFiles()) {
                            if (i.startsWith("git ")) {
                                Git.cloneRepository()
                                        .setURI(i.replaceFirst("git ", "").replace(" subm", ""))
                                        .setDirectory(new File(homeDir + "/" + aPackage.getName() + "/" + (i.split("/")[i.split("/").length - 1]).replace(" subm", "")))
                                        .setCloneSubmodules(i.contains("subm"))
                                        .call();
                            } else if (i.startsWith("download ")) {
                                URL website = new URL(i.replaceFirst("download ", ""));
                                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                                FileOutputStream fos = new FileOutputStream(homeDir + "/" + aPackage.getName() + "/" + i.split("/")[i.split("/").length - 1]);
                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                            } else if (i.startsWith("unzip ")) {
                                Zip.unzip(homeDir + "/" + aPackage.getName() + "/" + i.replaceFirst("unzip ", ""), homeDir + "/" + aPackage.getName() + "/" + i.replace("unzip ", "").replace(".zip", "").split("/")[i.split("/").length - 1]);
                            } else if (i.startsWith("copy ")) {
                                File dest = new File(packDir, i.split(",")[1]);
                                if (dest.isDirectory()) {
                                    dest = new File(dest, i.split(",")[0].replaceFirst("copy ", "").split("/")[i.split(",")[0].split("/").length - 1]);
                                }
                                copyFile(new File(packDir, i.split(",")[0].replaceFirst("copy ", "")), dest);
                            } else if (i.startsWith("addexec ")) {
                                new InstallerFunctions.AddExecutable(new SyntaxTree.Text(i.replace("addexec", "").trim())).eval();
                            }
                        }
                        if (body == null) {
                            Files.copy(new File(argParser.getArgs().get(1)).toPath(), new File(homeDir + "/" + aPackage.getName() + "/" + aPackage.getName() + ".pkgconf").toPath());
                        } else {
                            byte[] data = body.getBytes();
                            FileOutputStream out = new FileOutputStream(new File(homeDir + "/" + aPackage.getName() + "/" + aPackage.getName() + ".pkgconf").getPath());
                            out.write(data);
                            out.close();
                        }
                    }
                    File installCode = new File(packDir.getPath() + "/install");
                    if (installCode.exists()) {
                        SyntaxTree.getFunctions().clear();
                        SyntaxTree.getVariables().clear();
                        SyntaxTree.getClassesParents().clear();
                        SyntaxTree.getClassesWithInit().clear();
                        SyntaxTree.getClassesParameters().clear();
                        new SyntaxTree.Function("sh", new SyntaxTree.Return(new InstallerFunctions.Shell(new SyntaxTree.Variable("a"))), "a").eval();
                        new SyntaxTree.Function("addexec", new InstallerFunctions.AddExecutable(new SyntaxTree.Variable("a")), "a").eval();
                        new SyntaxTree.SetVariable("OSName", new SyntaxTree.Text(System.getProperty("os.name"))).eval();
                        CompilerMain.compile(new Compiler(installCode.getPath(), false, null, null, null, null));
                    }
                }
            } else if (argParser.getArgs().get(0).equals("remove")) {
                if (argParser.getArgs().size() != 2) {
                    error(REMOVE_HELP_MESSAGE);
                } else {
                    deleteDir(new File(homeDir + "/" + argParser.getArgs().get(1)));
                }
            } else if (argParser.getArgs().get(0).equals("alias")) {
                if (argParser.getArgs().size() != 2) {
                    error(ADD_KNOWN_PKG_HELP_MESSAGE);
                } else {
                    File file = new File(argParser.getArgs().get(1));
                    File repos = new File(PackageManager.getHomeDirectory(), "aliases.list");
                    if (!repos.exists()) repos.createNewFile();
                    if (file.isFile()) {
                        Scanner scanner = null;
                        try {
                            scanner = new Scanner(file);
                        } catch (FileNotFoundException e) {
                            System.err.println("file not found");
                            System.exit(1);
                        }
                        FileOutputStream out = new FileOutputStream(repos, true);
                        while (true) {
                            try {
                                String line = scanner.nextLine();
                                String key = line.split("\t")[0];
                                String value = line.split("\t")[1];
                                if (key.equals("") || value.equals("")) continue;
                                byte[] data = (line + "\n").getBytes();
                                out.write(data);
                            } catch (NoSuchElementException e) {
                                break;
                            } catch (ArrayIndexOutOfBoundsException ignored) {}
                        }
                        out.close();
                    }
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
