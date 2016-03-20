package lila.i18n

import java.io.File
import scala.collection.JavaConversions._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.ask
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import makeTimeout.veryLarge

private[i18n] final class GitWrite(
    transRelPath: String,
    repoPath: String,
    system: ActorSystem) {

  private val repo = (new FileRepositoryBuilder())
    .setGitDir(new File(repoPath + "/.git"))
    .readEnvironment() // scan environment GIT_* variables
    .findGitDir() // scan up the file system tree
    .build()

  private val git = new Git(repo, debug = true)

  def apply(translations: List[Translation]): Funit = {
    logger.info("Working on " + repoPath)
    git.currentBranch flatMap { currentBranch =>
      logger.info("Current branch is " + currentBranch)
      (translations map gitActor.?).sequenceFu >>
        (gitActor ? currentBranch mapTo manifest[Unit])
    }
  }

  private lazy val gitActor = system.actorOf(Props(new Actor {

    def receive = {

      case branch: String => {
        logger.info("Checkout " + branch)
        git checkout branch
        sender ! (())
      }

      case translation: Translation => {
        val branch = "t/" + translation.id
        val code = translation.code
        val name = (LangList name code) err "Lang does not exist: " + code
        val commitMsg = commitMessage(translation, name)
        sender ! (git branchExists branch flatMap {
          _.fold(
            fuccess(logger.warn("! Branch already exists: " + branch)),
            git.checkout(branch, true) >>
              writeMessages(translation) >>-
              logger.info("Add " + relFileOf(translation)) >>
              (git add relFileOf(translation)) >>-
              logger.info("- " + commitMsg) >>
              (git commit commitMsg).void
          )
        }).await
      }

    }
  }))

  private def writeMessages(translation: Translation) = {
    logger.info("Write messages to " + absFileOf(translation))
    printToFile(absFileOf(translation)) { writer =>
      translation.lines foreach writer.println
    }
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

    def currentBranch: Fu[String] = Future {
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
  }
}
