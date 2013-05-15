package lila.i18n

import java.io.File
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepository
import scala.collection.JavaConversions._
import scala.concurrent.Future
import akka.actor._

private[i18n] final class GitWrite(
    transRelPath: String,
    repoPath: String,
    system: ActorSystem) {

  private val repo = new FileRepository(repoPath + "/.git")
  private val git = new Git(repo, debug = true)

  def apply(translations: List[Translation]): Funit =
    fuloginfo("Working on " + repoPath) >>
      git.currentBranch flatMap { currentBranch ⇒
        fuloginfo("Current branch is " + currentBranch) >>-
          (translations foreach writer.!) >>
          fuloginfo("Checkout " + currentBranch) >>
          (git checkout currentBranch)
      } void

  private def writer = system.actorOf(Props(new Actor {

    def receive = {

      case translation: Translation ⇒ {
        val branch = "t/" + translation.id
        val code = translation.code
        val name = (LangList name code) err "Lang does not exist: " + code
        val commitMsg = commitMessage(translation, name)
        git branchExists branch flatMap {
          _.fold(
            fuloginfo("! Branch already exists: " + branch),
            git.checkout(branch, true) >>
              writeMessages(translation) >>
              fuloginfo("Add " + relFileOf(translation)) >>
              (git add relFileOf(translation)) >>
              fuloginfo("- " + commitMsg) >>
              (git commit commitMsg).void)
        }
      } await

    }
  }), name = "i18n-gitwrite")

  private def write(translation: Translation): Funit = {
    val branch = "t/" + translation.id
    val code = translation.code
    val name = (LangList name code) err "Lang does not exist: " + code
    val commitMsg = commitMessage(translation, name)
    git branchExists branch flatMap {
      _.fold(
        fuloginfo("! Branch already exists: " + branch),
        git.checkout(branch, true) >>
          writeMessages(translation) >>
          fuloginfo("Add " + relFileOf(translation)) >>
          (git add relFileOf(translation)) >>
          fuloginfo("- " + commitMsg) >>
          (git commit commitMsg).void)
    }
  }

  private def writeMessages(translation: Translation) =
    fuloginfo("Write messages to " + absFileOf(translation)) >>
      printToFile(absFileOf(translation)) { writer ⇒
        translation.lines foreach writer.println
      }

  private def relFileOf(translation: Translation) =
    "%s/messages.%s".format(transRelPath, translation.code)

  private def absFileOf(translation: Translation) =
    repoPath + "/" + relFileOf(translation)

  private def commitMessage(translation: Translation, name: String) =
    """%s "%s" translation #%d. Author: %s. %s""".format(
      translation.code,
      name,
      translation.id,
      translation.author | "Anonymous",
      translation.comment | "")

  final class Git(repo: Repository, debug: Boolean = false) {

    import org.eclipse.jgit.api._

    val api = new org.eclipse.jgit.api.Git(repo)

    def currentBranch = Future {
      cleanupBranch(repo.getFullBranch)
    }

    def branchList = Future {
      api.branchList.call map (_.getName) map cleanupBranch
    }

    def branchExists(branch: String) =
      branchList map (_ contains branch)

    def checkout(branch: String, create: Boolean = false) = Future {
      api.checkout.setName(branch).setCreateBranch(create).call
    }

    def add(pattern: String) = Future {
      api.add.addFilepattern(pattern).call
    }

    def commit(message: String) = Future {
      api.commit.setMessage(message).call
    }

    private def cleanupBranch(branch: String) =
      branch.replace("refs/heads/", "")

    private def log(msg: ⇒ Any) = debug.fold(fuloginfo(msg.toString), funit)
  }

}
