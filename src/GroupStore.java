import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Saves the group (people and expenses) to a JSON file and loads it back
 *
 * The file has its own small records instead of saving UserGraph directly, so the file format
 * stays the same even if the model changes, and loading goes through the normal checks.
 */
public class GroupStore {

    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    // what the file looks like, people.get(0) is always you
    private record SaveFile(int version, List<String> people, List<SavedExpense> expenses) {}

    // date is written as text, ie "2026-11-03"
    private record SavedExpense(int payer, List<Integer> splitWith, long[] shares, String description,
            String date) {}

    /**
     * Makes a store for one save file, it isn't read or written until load or save
     * @param file where the group is saved
     */
    public GroupStore(Path file) {
        this.file = file;
    }

    /**
     * Getter for the usual save file, ~/.tabsplit/group.json
     * @return the path
     */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".tabsplit", "group.json");
    }

    /**
     * Getter for the save file
     * @return the path
     */
    public Path file() {
        return file;
    }

    /**
     * Checks if there's a save file yet, ie false on first launch
     * @return true if the file exists
     */
    public boolean exists() {
        return Files.exists(file);
    }

    /**
     * Writes the whole group to the file, replacing what was there
     * @param group the group to save
     * @throws IOException if the file can't be written
     */
    public void save(UserGraph group) throws IOException {
        List<SavedExpense> expenses = new ArrayList<>();
        for (Expense expense : group.getExpenses()) {
            expenses.add(new SavedExpense(expense.payer(), expense.splitWith(), expense.shares(),
                expense.description(), expense.date().toString()));
        }
        String json = GSON.toJson(new SaveFile(VERSION, group.getUserList(), expenses));

        // write a temp file then swap it in, so a crash mid-save can't leave half a file
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, json, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Reads the group back from the file
     * @return the group
     * @throws IOException if the file can't be read or anything in it is wrong
     */
    public UserGraph load() throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        try {
            SaveFile saved = GSON.fromJson(json, SaveFile.class);
            if (saved == null || saved.version() != VERSION) {
                throw new IOException("Unknown save file version.");
            }
            // rebuild through the normal methods so a hand-edited file gets the same checks
            UserGraph group = new UserGraph();
            List<String> people = saved.people();
            if (!people.get(0).equals(group.getUserList().get(0))) {
                group.renameUser(0, people.get(0));
            }
            for (int i = 1; i < people.size(); i++) {
                group.addUser(people.get(i));
            }
            for (SavedExpense e : saved.expenses()) {
                long total = 0;
                for (long share : e.shares()) {
                    total += share;
                }
                group.addSplit(e.payer(), e.splitWith(), total, e.shares(), e.description(),
                    LocalDate.parse(e.date()));
            }
            return group;
        } catch (RuntimeException e) {
            // bad json, a missing field, a bad date, a rejected expense... all mean the file is unusable
            throw new IOException("The save file is damaged: " + e.getMessage(), e);
        }
    }

    /**
     * Renames a damaged save file to group.json.bad so it's kept but not loaded again
     * @return where it was moved
     * @throws IOException if it can't be moved
     */
    public Path moveAside() throws IOException {
        Path bad = file.resolveSibling(file.getFileName() + ".bad");
        Files.move(file, bad, StandardCopyOption.REPLACE_EXISTING);
        return bad;
    }
}
