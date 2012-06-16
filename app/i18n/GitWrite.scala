package lila
package i18n

import java.io.File
import org.eclipse.jgit.api._
import org.eclipse.jgit.storage.file.FileRepository
import scalaz.effects._

final class GitWrite(repoPath: String) {

  def apply(translations: List[Translation]): IO[Unit] = for {
    currentBranch ← io(repo.getFullBranch.pp)
    _ ← (translations map write).sequence map (_ ⇒ Unit)
    _ ← io(git.checkout.setName(currentBranch).call)
  } yield ()

  private def write(translation: Translation): IO[Unit] = io()

  private lazy val git = new Git(repo)

  private lazy val repo = new FileRepository(repoPath + "/.git")
}
