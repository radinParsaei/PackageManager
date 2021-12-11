import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class InstallerFunctions {
    private static String packageName = "";

    public static String getPackageName() {
        return packageName;
    }

    public static void setPackageName(String packageName) {
        InstallerFunctions.packageName = packageName;
    }

    public static class Shell extends CustomValue {
        private final ValueBase value;
        public Shell(ValueBase value) {
            this.value = value;
        }

        @Override
        public Object getData() {
            Runtime rt = Runtime.getRuntime();
            Process proc = null;
            try {
                proc = rt.exec(value + "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            while (true) {
                try {
                    String s = stdInput.readLine();
                    if (s == null) break;
                    output.append(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            while (true) {
                try {
                    String s = stdError.readLine();
                    if (s == null) break;
                    error.append(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new SyntaxTree.List(new SyntaxTree.Text(output.toString()), new SyntaxTree.Text(error.toString()),
                    new SyntaxTree.Number(proc.exitValue()), new SyntaxTree.Number(new BigDecimal(proc.pid())));
        }

        @Override
        ArrayList<Object> addNamespaceOn() {
            return new ArrayList<>(Collections.singleton(value));
        }
    }

    public static class AddExecutable extends CustomProgram {
        private final ValueBase value;
        public AddExecutable(ValueBase value) {
            this.value = value;
        }

        @Override
        void eval() {
            try {
                File executablesDir = new File(PackageManager.getHomeDirectory(), "bin");
                if (!executablesDir.isDirectory()) {
                    executablesDir.mkdir();
                }
                String fileName = value.toString();
                Files.createSymbolicLink(new File(executablesDir.getPath(), fileName.split("/")[fileName.split("/").length - 1]).toPath(), new File(PackageManager.getPackageDirectory(packageName), value.toString()).toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        ArrayList<Object> addNamespaceOn() {
            return new ArrayList<>(Collections.singleton(value));
        }
    }
}
