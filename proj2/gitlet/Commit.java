package gitlet;

import static gitlet.Repository.gitletError;
import static gitlet.Utils.*;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;


/** Represents a gitlet commit object.
 *  Consists of a log message, timestamp, mapping of file names to blob references,
 *  parent reference, & second parent reference (for merges).
 *
 *  @author Alex Rios
 *
 * @Source: https://stackoverflow.com/questions/10378855
 * Defined a private static final long SerialVersionUID to fix InvalidClassException.
 * @Source: https://stackoverflow.com/questions/46898
 * Used ideas from this post in the checkoutCommit method to iterate over each entry in the blobMap.
 */
public class Commit implements Serializable {

    /** Format of the date. */
    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
    /** The message of this Commit. */
    private String message;
    /** Date & time the Commit was created. */
    private String timestamp;
    /** Hash of the parent commit. */
    private String parent;
    /** Hash of a second parent (if applicable, else null) */
    private String secParent;
    /** Sha1 hash of the current commit. */
    private String UID;
    /** Hashmap of all the blobs tracked by the commit. */
    private HashMap<String, String> blobMap;

    @Serial
    private static final long serialVersionUID = -1729364433321816610L;

    public Commit() throws IOException {
        this.message = "initial commit";
        Date epoch = new Date(0);
        this.timestamp = FORMAT.format(epoch);
        this.blobMap = new HashMap<>();
        this.UID = sha1(serialize(this));
        saveCommit();
    }

    public Commit(String message, String parent, String secParent) throws IOException {
        this.message = message;
        Date currDate = new Date();
        this.timestamp = FORMAT.format(currDate);
        this.parent = parent;
        this.secParent = secParent;
        this.blobMap = createBlobMap();
        this.UID = sha1(serialize(this));
        CommitInfo.readCommitInfo().updateHead(UID);
        saveCommit();
    }

    /** Creates a HashMap w/ the file names as the key, and their hash as the value
     *  Inherits from parent commit's map. If an untracked file or new version of a file
     *  exists in the staging area, it's added to the blobMap, overwriting any old versions.
     *  This file is also added to the .blobs directory, w/ it's sha1 hash as its name.
     */
    private HashMap<String, String> createBlobMap() throws IOException {
        HashMap<String, String> parentBlobMap = readCommit(parent).blobMap;
        HashMap<String, String> newBlobMap = new HashMap<>(parentBlobMap);
        List<String> addList = plainFilenamesIn(Repository.STAGING_ADD);
        List<String> rmList = plainFilenamesIn(Repository.STAGING_RM);
        if (addList == null || rmList == null) {
            gitletError("No changes added to the commit.");
        }
        for (String curr:addList) {
            File currAdd = join(Repository.STAGING_ADD, curr);
            String currHash = sha1(readContents(currAdd));
            newBlobMap.put(curr, currHash);
            File blob = join(Repository.BLOBS_DIR, currHash);
            Files.copy(currAdd.toPath(), blob.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        for (String curr:rmList) {
            newBlobMap.remove(curr);
        }
        return newBlobMap;
    }

    /** Reads in a commit from a file with the given uid. */
    public static Commit readCommit(String uid) {
        File targetCommit = join(Repository.GITLET_DIR, uid);
        if (!targetCommit.exists()) {
            gitletError("No commit with that id exists.");
        }
        return readObject(targetCommit, Commit.class);
    }

    /** Saves a commit to a file w/ it's sha1 hash as its name. */
    private void saveCommit() throws IOException {
        File savedCommit = new File(Repository.GITLET_DIR, this.UID);
        savedCommit.createNewFile();
        writeObject(savedCommit, this);
    }

    /** Copies all files being tracked in the current commit into the CWD,
     *  overwriting any older versions if they exist.
     */
    public void checkoutCommit() throws IOException {
        for (Map.Entry<String, String> blob : blobMap.entrySet()) {
            File trackFile = join(Repository.BLOBS_DIR, blob.getValue());
            File tgtFile = join(Repository.CWD, blob.getKey());
            Files.copy(trackFile.toPath(), tgtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Returns the blobMap of the current commit. */
    public HashMap<String, String> getBlobMap() {
        return blobMap;
    }

    /** Returns the UID of the blob from the blobMap that corresponds w/ the given fileName. */
    public String getBlobUID(String fileName) {
        return blobMap.get(fileName);
    }

    public boolean isTracked(String fileName) {
        return blobMap.containsKey(fileName);
    }

    public String getMessage() {
        return this.message;
    }
    public String getTimestamp() {
        return this.timestamp;
    }
    public String getParent() {
        return this.parent;
    }
    public String getSecParent() {
        return this.secParent;
    }
    public String getUID() {
        return this.UID;
    }
}
