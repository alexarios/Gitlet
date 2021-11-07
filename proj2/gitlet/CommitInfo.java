package gitlet;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static gitlet.Repository.gitletError;
import static gitlet.Utils.*;

public class CommitInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -2496762734957454755L;

    /** The sha1 hash of the head commit. */
    private String HEAD;
    private HashMap<String, String> branchMap = new HashMap<>();

    /** Creates a new CommitInfo object that stores itself in a file for later use. */
    public CommitInfo(String head, String uid) throws IOException {
        Repository.COMMIT_INFO.createNewFile();
        branchMap.put(head, uid);
        this.HEAD = head;
        saveCommitInfo();
    }

    /** Creates a new branch w/ the given name. */
    public void addBranch(String name) {
        if (branchMap.containsKey(name)) {
            gitletError("A branch with that name already exists.");
        }
        branchMap.put(name, getHeadCommit());
        saveCommitInfo();
    }

    /** Removes the branch with the given name. */
    public void removeBranch(String name) {
        if (!branchMap.containsKey(name)) {
            gitletError("A branch with that name does not exist.");
        }
        if (name.equals(this.HEAD)) {
            gitletError("Cannot remove the current branch.");
        }
        branchMap.remove(name);
        saveCommitInfo();
    }

    /** Returns the head branch. */
    public String getHEAD() {
        return this.HEAD;
    }
    /** Returns the UID of the head commit. */
    public String getHeadCommit() {
        return branchUID(this.HEAD);
    }
    /** Changes the head branch to the given branch. */
    public void changeHead(String branch) {
        this.HEAD = branch;
        saveCommitInfo();
    }
    /** Updates the commit HEAD is pointing at. */
    public void updateHead(String uid) {
        branchMap.put(HEAD, uid);
        saveCommitInfo();
    }
    /** Returns the uid of the commit for the given branch */
    public String branchUID(String branch) {
        return branchMap.get(branch);
    }

    /** Returns an array of all the branches in lexicographic order. */
    public String[] sortedBranches() {
        Set<String> branchSet = branchMap.keySet();
        String[] branchArray = branchSet.toArray(new String[0]);
        Arrays.sort(branchArray);
        return branchArray;
    }

    /** Writes the CommitInfo object into our file. */
    public void saveCommitInfo()  {
        writeObject(Repository.COMMIT_INFO, this);
    }
    public static CommitInfo readCommitInfo() {
        return readObject(Repository.COMMIT_INFO, CommitInfo.class);
    }

}
