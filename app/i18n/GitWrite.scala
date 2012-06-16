package lila
package i18n

import java.io.File
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepository
import scalaz.effects._
import scala.collection.JavaConversions._

final class GitWrite(
    transRelPath: String,
    repoPath: String) {

  val repo = new FileRepository(repoPath + "/.git")
  val git = new Git(repo, debug = true)

  def apply(translations: List[Translation]): IO[Unit] = for {
    _ ← putStrLn("Working on " + repoPath)
    currentBranch ← git.currentBranch
    _ ← putStrLn("Current branch is " + currentBranch)
    _ ← (translations map write).sequence map (_ ⇒ Unit)
    _ ← putStrLn("Checkout " + currentBranch)
    _ ← git checkout currentBranch
  } yield ()

  private def write(translation: Translation): IO[Unit] = {
    val branch = "t/" + translation.id
    val code = translation.code
    val name = (LangList name code) err "Lang does not exist: " + code
    val commitMsg = commitMessage(translation, name)
    for {
      branchExists ← git branchExists branch
      _ ← branchExists.fold(
        putStrLn("! Branch already exists: " + branch),
        for {
          _ ← git.checkout(branch, true)
          _ ← writeMessages(translation)
          _ ← putStrLn("Add " + relFileOf(translation))
          _ ← git add relFileOf(translation)
          _ ← putStrLn("- " + commitMsg)
          _ ← git commit commitMsg
        } yield ()
      )
    } yield ()
  }

  private def writeMessages(translation: Translation) = for {
    _ ← putStrLn("Write messages to " + absFileOf(translation))
    _ ← printToFile(absFileOf(translation)) { writer ⇒
      translation.lines foreach writer.println
    }
  } yield ()

  private def relFileOf(translation: Translation) =
    "%s/messages.%s".format(transRelPath, translation.code)

  private def absFileOf(translation: Translation) =
    repoPath + "/" + relFileOf(translation)

  private def commitMessage(translation: Translation, name: String) =
    """"%s" translation #%d. Author: %s. %s""".format(
      name,
      translation.id,
      translation.author | "Anonymous",
      translation.comment | "")

  final class Git(repo: Repository, debug: Boolean = false) {

    import org.eclipse.jgit.api._

    val api = new org.eclipse.jgit.api.Git(repo)

    def currentBranch = io {
      cleanupBranch(repo.getFullBranch)
    }

    def branchList = io {
      api.branchList.call map (_.getName) map cleanupBranch
    }

    def branchExists(branch: String) =
      branchList map (_ contains branch)

    def checkout(branch: String, create: Boolean = false) = io {
      api.checkout.setName(branch).setCreateBranch(create).call
    }

    def add(pattern: String) = io {
      api.add.addFilepattern(pattern).call
    }

    def commit(message: String) = io {
      api.commit.setMessage(message).call
    }

    private def cleanupBranch(branch: String) =
      branch.replace("refs/heads/", "")

    private def log(msg: ⇒ Any) =
      debug.fold(putStrLn(msg.toString), io())
  }

}
