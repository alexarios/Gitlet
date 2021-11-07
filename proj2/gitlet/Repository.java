package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  Has methods that implement all the gitlet command.
 *
 *  @author Alex Rios
 */
public class Repository implements Serializable {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** Directory representing the staging area. Contains add/rm dirs as well. */
    public static final File STAGING_AREA = join(GITLET_DIR, ".staging");
    public static final File STAGING_ADD = join(STAGING_AREA, ".add");
    public static final File STAGING_RM = join(STAGING_AREA, ".rm");
    /** Directory that stores all our blobs. */
    public static final File BLOBS_DIR = join(GITLET_DIR, ".blobs");
    /** File that stores all our commit info, including branches. */
    public static final File COMMIT_INFO = join(GITLET_DIR, ".CommitInfo");


    /** Initializes gitlet repository. */
    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            String msg = "A Gitlet version-control system already exists in the current directory.";
            gitletError(msg);
        }
        GITLET_DIR.mkdir();
        STAGING_AREA.mkdir();
        STAGING_ADD.mkdir();
        STAGING_RM.mkdir();
        BLOBS_DIR.mkdir();
        COMMIT_INFO.createNewFile();

        Commit initCommit = new Commit();
        String initUID = initCommit.getUID();
        new CommitInfo("master", initUID);
    }

    /** Stages a file for addition. */
    public static void add(String name) throws IOException {
        Commit currCommit = Commit.readCommit(CommitInfo.readCommitInfo().getHeadCommit());
        String currVersion = currCommit.getBlobUID(name);
        File addedFile = join(CWD, name);
        File stageFile = join(STAGING_ADD, name);
        if (!addedFile.exists()) {
            gitletError("File does not exist.");
        }
        if (sha1(readContents(addedFile)).equals(currVersion)) {
            stageFile.delete();
        } else {
            Files.copy(addedFile.toPath(), stageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        join(STAGING_RM, name).delete();

    }

    /** Creates a new commit. */
    public static void newCommit(String msg, String secParent) throws IOException {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        List<String> addList = plainFilenamesIn(STAGING_ADD);
        List<String> rmList = plainFilenamesIn(STAGING_RM);
        if (addList.isEmpty() && rmList.isEmpty()) {
            gitletError("No changes added to the commit.");
        }
        new Commit(msg, currInfo.getHeadCommit(), secParent);
        clearStagingArea();
    }

    /** Stages a file for removal, deleting it from CWD. */
    public static void remove(String name) throws IOException {
        Commit currCommit = Commit.readCommit(CommitInfo.readCommitInfo().getHeadCommit());
        File stagedAdd = join(STAGING_ADD, name);
        File rmFile = join(CWD, name);
        File stageFile = join(STAGING_RM, name);

        Boolean isTracked = currCommit.isTracked(name);
        if (!stagedAdd.exists() && !isTracked) {
            gitletError("No reason to remove the file.");
        }
        stagedAdd.delete();
        if (isTracked) {
            stageFile.createNewFile();
            rmFile.delete();
        }
    }

    /** Prints the log of all commits starting at HEAD. */
    public static void log() {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        Commit currCommit = Commit.readCommit(currInfo.getHeadCommit());
        while (currCommit.getParent() != null) {
            System.out.println("===");
            System.out.println("commit " + currCommit.getUID());
            if (currCommit.getSecParent() != null) {
                String parent = currCommit.getParent().substring(0, 7);
                String secParent = currCommit.getSecParent().substring(0, 7);
                System.out.println("Merge: " + parent + " " + secParent);
            }
            System.out.println("Date: " + currCommit.getTimestamp());
            System.out.println(currCommit.getMessage() + "\n");
            currCommit = Commit.readCommit(currCommit.getParent());
        }
        System.out.println("===");
        System.out.println("commit " + currCommit.getUID());
        System.out.println("Date: " + currCommit.getTimestamp());
        System.out.println(currCommit.getMessage());
    }

    /** Prints log of all commits ever made. */
    public static void globalLog() {
        List<String> commitList = plainFilenamesIn(GITLET_DIR);
        for (String curr : commitList) {
            if (!curr.equals(".CommitInfo")) {
                Commit currCommit = Commit.readCommit(curr);
                System.out.println("===");
                if (currCommit.getSecParent() != null) {
                    String parent = currCommit.getParent().substring(0, 6);
                    String secParent = currCommit.getSecParent().substring(0, 6);
                    System.out.println("Merge: " + parent + secParent);
                }
                System.out.println("commit " + currCommit.getUID());
                System.out.println("Date: " + currCommit.getTimestamp());
                System.out.println(currCommit.getMessage() + "\n");
            }
        }
    }

    /** Prints the ids of all commits with the given message. */
    public static void find(String msg) {
        List<String> commitList = plainFilenamesIn(GITLET_DIR);
        Boolean commitExists = false;
        for (String curr : commitList) {
            if (!curr.equals(".CommitInfo")) {
                Commit currCommit = Commit.readCommit(curr);
                if (currCommit.getMessage().equals(msg)) {
                    System.out.println(currCommit.getUID());
                    commitExists = true;
                }
            }
        }
        if (!commitExists) {
            gitletError("Found no commit with that message.");
        }
    }

    /** Prints existing branches, files staged for addition/removal, modifications
     *  not staged for commit, and current files that are untracked.
     */
    public static void status() {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        Commit currComit = Commit.readCommit(currInfo.getHeadCommit());
        System.out.println("=== Branches ===");
        String[] branches = currInfo.sortedBranches();
        for (String branch : branches) {
            if (currInfo.getHEAD().equals(branch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println("\n" + "=== Staged Files ===");
        printFiles(plainFilenamesIn(STAGING_ADD));
        System.out.println("=== Removed Files ===");
        printFiles(plainFilenamesIn(STAGING_RM));
        System.out.println("=== Modifications Not Staged For Commit ===");
        modifiedNotStaged();
        System.out.println("=== Untracked Files ===");
        List<String> untrackedList = new ArrayList<>();
        for (String file : plainFilenamesIn(CWD)) {
            if (!currComit.isTracked(file) && !join(STAGING_ADD, file).exists()) {
                untrackedList.add(file);
            }
        }
        printFiles(untrackedList);
    }
    /** Prints out the "Modifications Not Staged For Commit section. */
    private static void modifiedNotStaged() {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        Commit currCommit = Commit.readCommit(currInfo.getHeadCommit());
        Set<String> fileSet = new HashSet<>(plainFilenamesIn(CWD));
        fileSet.addAll(currCommit.getBlobMap().keySet());
        List<String> totalList = new ArrayList<>();
        for (String file : fileSet) {
            File currFile = join(CWD, file);
            File addFile = join(STAGING_ADD, file);
            File rmFile = join(STAGING_RM, file);
            if (currFile.exists()) {
                String fileHash = (sha1(readContents(currFile)));
                if ((currCommit.isTracked(file)
                        && !Objects.equals(fileHash, currCommit.getBlobUID(file))
                        && !addFile.exists())
                        && !readContentsAsString(currFile).contains("<<<<<<< HEAD")
                        || (addFile.exists()
                        && !Objects.equals(fileHash, sha1(readContents(addFile))))) {
                    totalList.add(file + " (modified)");
                }
            } else {
                if (addFile.exists() || (!rmFile.exists()) && currCommit.isTracked(file)) {
                    totalList.add(file + " (deleted)");
                }
            }
        }
        printFiles(totalList);
    }
    /** Prints out a list of file names, sorted lexicographically. */
    private static void printFiles(List<String> fileList) {
        if (fileList != null) {
            Collections.sort(fileList);
            for (String fileName : fileList) {
                System.out.println(fileName);
            }
        }
        System.out.println("");
    }

    /** All three cases for checkout. */
    public static void checkout(String fileName) throws IOException {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        checkout(currInfo.getHeadCommit(), fileName);

    }
    public static void checkout(String commitUID, String fileName) throws IOException {
        commitUID = fullUID(commitUID);
        Commit targetCommit = Commit.readCommit(commitUID);
        String blobUID = targetCommit.getBlobUID(fileName);
        if (blobUID != null) {
            File blobFile = join(BLOBS_DIR, blobUID);
            File targetFile = join(CWD, fileName);
            Files.copy(blobFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            gitletError("File does not exist in that commit.");
        }
    }
    public static void checkoutBranch(String branch) throws IOException {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        String branchUID = currInfo.branchUID(branch);
        if (branchUID == null) {
            gitletError("No such branch exists.");
        }
        checkUntracked(branchUID);
        if (branch.equals(currInfo.getHEAD())) {
            gitletError("No need to checkout the current branch.");
        }
        Commit branchCommit = Commit.readCommit(branchUID);
        delUntracked(branchCommit);
        branchCommit.checkoutCommit();
        currInfo.changeHead(branch);
        clearStagingArea();
    }
    /** Checks if there's an untracked file that would be overwritten. */
    private static void checkUntracked(String otherBranch) {
        Commit currCommit = Commit.readCommit(CommitInfo.readCommitInfo().getHeadCommit());
        Commit otherCommit = Commit.readCommit(otherBranch);
        List<String> cwdFiles = plainFilenamesIn(CWD);
        String er = "There is an untracked file in the way; delete it, or add and commit it first.";
        if (cwdFiles != null) {
            for (String fileName : cwdFiles) {
                if (!currCommit.isTracked(fileName) && otherCommit.isTracked(fileName)) {
                    gitletError(er);
                }
            }
        }
    }
    /** Deletes files in cwd not tracked by the given commit  */
    private static void delUntracked(Commit targetCommit) throws IOException {
        List<String> cwdFiles = plainFilenamesIn(CWD);
        if (cwdFiles != null) {
            for (String fileName : cwdFiles) {
                if (!targetCommit.isTracked(fileName)) {
                    Files.delete(join(CWD, fileName).toPath());
                }
            }
        }
    }

    /** Creates a new branch w/ the given name, pointing at the current head commit. */
    public static void branch(String name) {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        currInfo.addBranch(name);
    }

    /** Deletes the branch with the given name. */
    public static void rmBranch(String name) {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        currInfo.removeBranch(name);
    }

    /** Checks out all files tracked by the given commit. */
    public static void reset(String commitUID) throws IOException {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        checkUntracked(commitUID);
        commitUID = fullUID(commitUID);
        Commit targetCommit = Commit.readCommit(commitUID);
        delUntracked(targetCommit);
        targetCommit.checkoutCommit();
        currInfo.updateHead(commitUID);
        clearStagingArea();
    }
    /** Returns the fullUID of the commit if it exists. Otherwise, returns error message. */
    private static String fullUID(String uid) {
        if (uid.length() == 40 && join(GITLET_DIR, uid).exists()) {
            return uid;
        } else if (uid.length() >= 6) {
            List<String> commitList = plainFilenamesIn(GITLET_DIR);
            for (String currUID : commitList) {
                if (currUID.startsWith(uid)) {
                    return currUID;
                }
            }
        }
        gitletError("No commit with that id exists.");
        return null;
    }

    public static void merge(String otherBranch) throws IOException {
        CommitInfo currInfo = CommitInfo.readCommitInfo();
        if (!plainFilenamesIn(STAGING_ADD).isEmpty() || !plainFilenamesIn(STAGING_RM).isEmpty()) {
            gitletError("You have uncommitted changes.");
        }
        if (currInfo.branchUID(otherBranch) == null) {
            gitletError("A branch with that name does not exist.");
        }
        checkUntracked(currInfo.branchUID(otherBranch));
        boolean hasConflict = false;
        if (currInfo.getHEAD().equals(otherBranch)) {
            gitletError("Cannot merge a branch with itself.");
        }
        Commit headCommit = Commit.readCommit(currInfo.getHeadCommit());
        Commit otherCommit = Commit.readCommit(currInfo.branchUID(otherBranch));
        Commit splitCommit = splitPoint(headCommit, otherCommit);
        if (splitCommit.getUID().equals(otherCommit.getUID())) {
            gitletError("Given branch is an ancestor of the current branch.");
        } else if (splitCommit.getUID().equals(headCommit.getUID())) {
            checkoutBranch(otherBranch);
            gitletError("Current branch fast-forwarded.");
        }
        Set<String> fileNames = fileSet(splitCommit, headCommit, otherCommit);
        for (String name : fileNames) {
            String headFile = headCommit.getBlobUID(name);
            String otherFile = otherCommit.getBlobUID(name);
            String splitFile = splitCommit.getBlobUID(name);
            if (splitFile != null && (headFile != null || otherFile != null)) {
                if (!Objects.equals(otherFile, splitFile) && Objects.equals(headFile, splitFile)) {
                    if (otherFile == null) {
                        remove(name);
                    } else {
                        checkout(otherCommit.getUID(), name);
                        add(name);
                    }

                } else if (Objects.equals(headFile, splitFile) && otherFile == null) {
                    remove(name);
                }
            }
            if (!Objects.equals(headFile, splitFile) && !Objects.equals(otherFile, splitFile)) {
                if (!Objects.equals(headFile, otherFile)) {
                    conflict(name, headFile, otherFile);
                    hasConflict = true;
                }
            }
            if (splitFile == null) {
                if (headFile == null && otherFile != null) {
                    checkout(otherCommit.getUID(), name);
                    add(name);
                }
            }
        }
        newCommit("Merged " + otherBranch + " into " + currInfo.getHEAD() + ".",
                otherCommit.getUID());
        if (hasConflict) {
            gitletError("Encountered a merge conflict.");
        }
    }
    /** Returns a set of all filenames between all three commits. */
    public static Set<String> fileSet(Commit split, Commit head, Commit other) {
        Set<String> totalSet = new HashSet<>(split.getBlobMap().keySet());
        totalSet.addAll(head.getBlobMap().keySet());
        totalSet.addAll(other.getBlobMap().keySet());
        return totalSet;
    }
    /** Returns the commit of the split point given another branch. */
    private static Commit splitPoint(Commit head, Commit other) {
        Queue<Commit> commitQ = new LinkedList<>();
        commitQ.add(head);
        HashSet<String> marked = new HashSet<>();
        marked.add(head.getUID());
        while (!commitQ.isEmpty()) {
            Commit currCommit = commitQ.remove();
            String parent = currCommit.getParent();
            String secParent = currCommit.getSecParent();
            if (!marked.contains(parent) && parent != null) {
                marked.add(parent);
                commitQ.add(Commit.readCommit(parent));
            }
            if (!marked.contains(secParent) && secParent != null) {
                marked.add(secParent);
                commitQ.add(Commit.readCommit(secParent));
            }
        }
        commitQ.add(other);
        while (!commitQ.isEmpty()) {
            Commit currCommit = commitQ.remove();
            String otherParent = currCommit.getParent();
            String otherSecParent = currCommit.getSecParent();
            if (marked.contains(currCommit.getUID())) {
                return currCommit;
            }
            if (otherParent != null) {
                commitQ.add(Commit.readCommit(otherParent));
            }
            if (otherSecParent != null) {
                commitQ.add(Commit.readCommit(otherSecParent));
            }
        }
        return null;
    }
    /** Prints out a merge conflict given two files. */
    private static void conflict(String name, String head, String other) {
        String conflictString = "<<<<<<< HEAD\n";
        if (head != null) {
            conflictString += readContentsAsString(join(BLOBS_DIR, head));
        }
        conflictString += "=======\n";
        if (other != null) {
            conflictString += readContentsAsString(join(BLOBS_DIR, other));
        }
        conflictString += ">>>>>>>\n";
        writeContents(join(CWD, name), conflictString);

    }

    /** Checks to see if the repo has been initialized. */
    public static void repoExists() {
        if (!GITLET_DIR.exists()) {
            gitletError("Not in an initialized Gitlet directory.");
        }
    }

    /** Clears the staging area. */
    private static void clearStagingArea() {
        List<String> addList = plainFilenamesIn(STAGING_ADD);
        List<String> rmList = plainFilenamesIn(STAGING_RM);
        if (addList != null) {
            for (String curr : addList) {
                File currFile = join(STAGING_ADD, curr);
                currFile.delete();
            }
        }
        if (rmList != null) {
            for (String curr : rmList) {
                File currFile = join(STAGING_RM, curr);
                currFile.delete();
            }
        }
    }

    /** Prints the given message and exits. */
    public static void gitletError(String msg) {
        System.out.println(msg);
        System.exit(0);
    }
}
