import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

public class Package {
    private String name;
    private String version;
    private ArrayList<String> files;
    private ArrayList<String> tags;

    @JsonIgnoreProperties
    private String author;

    @JsonIgnoreProperties
    private ArrayList<String> dependencies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public ArrayList<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(ArrayList<String> dependencies) {
        this.dependencies = dependencies;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Package{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", files=" + files +
                ", tags=" + tags +
                ", author='" + author + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }
}