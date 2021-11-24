import java.util.ArrayList;
import java.util.HashMap;

public class ArgParser {
    private ArrayList<String> items;
    private ArrayList<String> args;
    private HashMap<String, String> values;

    public ArgParser(String[] args) {
        items = new ArrayList<>();
        this.args = new ArrayList<>();
        values = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String result = arg.substring(2);
                if (result.split("=").length > 1) {
                    values.put(result.split("=")[0], result.split("=")[1]);
                } else {
                    items.add(result);
                }
            } else if (arg.startsWith("-")) {
                for (int i = 1; i < arg.length(); i++) {
                    items.add(((Character) arg.charAt(i)).toString());
                }
            } else {
                this.args.add(arg);
            }
        }
    }

    public ArrayList<String> getItems() {
        return items;
    }

    public ArrayList<String> getArgs() {
        return args;
    }

    public HashMap<String, String> getValues() {
        return values;
    }

}
