package gitlet;

import java.io.IOException;

import static gitlet.Repository.gitletError;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Alex Rios
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            gitletError("Please enter a command.");
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                Repository.repoExists();
                String addFileName = args[1];
                Repository.add(addFileName);
                break;
            case "commit":
                Repository.repoExists();
                if (args.length < 2 || args[1].equals("")) {
                    gitletError("Please enter a commit message.");
                }
                String commitMsg = args[1];
                Repository.newCommit(commitMsg, null);
                break;
            case "rm":
                Repository.repoExists();
                String rmFileName = args[1];
                Repository.remove(rmFileName);
                break;
            case "log":
                Repository.repoExists();
                Repository.log();
                break;
            case "global-log":
                Repository.repoExists();
                Repository.globalLog();
                break;
            case "find":
                Repository.repoExists();
                String findMsg = args[1];
                Repository.find(findMsg);
                break;
            case "status":
                Repository.repoExists();
                Repository.status();
                break;
            case "checkout":
                Repository.repoExists();
                if (args.length == 3) {
                    String checkoutFileName = args[2];
                    Repository.checkout(checkoutFileName);
                } else if (args.length == 4) {
                    String checkOutCommit = args[1];
                    String checkoutFileName = args[3];
                    if (args[2].equals("--")) {
                        Repository.checkout(checkOutCommit, checkoutFileName);
                    } else {
                        gitletError("Incorrect operands.");
                    }

                } else if (args.length == 2) {
                    String branch = args[1];
                    Repository.checkoutBranch(branch);
                }
                break;
            case "branch":
                Repository.repoExists();
                String branchName = args[1];
                Repository.branch(branchName);
                break;
            case "rm-branch":
                Repository.repoExists();
                String rmBranchName = args[1];
                Repository.rmBranch(rmBranchName);
                break;
            case "reset":
                Repository.repoExists();
                String commitUID = args[1];
                Repository.reset(commitUID);
                break;
            case "merge":
                String branch = args[1];
                Repository.merge(branch);
                break;
            default:
                gitletError("No command with that name exists.");
        }
    }
}