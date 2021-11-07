# Gitlet Design Document

**Name**: Alex Rios

## Classes and Data Structures

### Main
Takes in arguments from the command line, calling the appropriate methods in our Repository class.

#### Fields
None


### Repository
Does the bulk of the computation for each command. Has a method for each command in Gitlet, with
some having their own private helper methods as well.


#### Fields
1. public static final File CWD = new File(System.getProperty("user.dir")) \
   The current working directory.
2. public static final File GITLET_DIR = join(CWD, ".gitlet") \
   The .gitlet directory.
3. public static final File STAGING_AREA = join(GITLET_DIR, ".staging")\
   public static final File STAGING_ADD = join(STAGING_AREA, ".add")\
   public static final File STAGING_RM = join(STAGING_AREA, ".rm")\
   Directory representing the staging area. Contains add/rm dirs as well.
4. public static final File BLOBS_DIR = join(GITLET_DIR, ".blobs") \
   Directory that stores all our blobs.
5. public static final File REPO_INFO = join(GITLET_DIR, ".repoInfo") \
   File that stores all our repo's info, including branches.

### Commit
Consists of a log message, timestamp, mapping of file names to blob references, parent reference, & second parent reference (for merges).

### Fields
None

### RepoInfo
Contains all information on the structure of the repository, including a map of all branches and their
respective commits, the current head, etc.

## Algorithms
#### Repository
public static void init \
Initializes gitlet repository.

public static void newCommit \
Creates a new commit.

public static void remove \
Stages a file for removal, deleting it from CWD.

public static void log \
Prints the log of all commits starting at HEAD.

public static void globalLog \
Prints log of all commits ever made.

public static void find \
Prints the ids of all commits with the given message.

public static void status \
Prints existing branches, files staged for addition/removal, modifications not staged for commit, and current files that are untracked.

public static void checkout \
Checks out a file from the head commit, from a given id, or all the files from a certain branch.

public static void branch \
Creates a new branch w/ the given name, pointing at the current head commit.

public static void rmBranch \
Deletes the branch with the given name.

public static void reset \
Checks out all files tracked by the given commit.

public static void merge \
Merges files from the given branch to the current branch.

## Persistence

To save certain files in commits, I will create files in the blob directory and copy files from the cwd being tracked into them.
Commits will have mappings to these blobs based on their uids.

To save the commits themselves, I will write the commit object itself into a file, and save it in the .gitlet directory with its
sha1 hash as its name.

To save information about the repo like branches, the current head, etc, I will write an object that contains all this info
into a file, and read it whenever I need to access the info.

