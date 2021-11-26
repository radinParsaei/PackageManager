import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

public class InstallerFunctions {
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
}
