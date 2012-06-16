package lila
package i18n

import java.io.File
import org.eclipse.jgit.api._
import scalaz.effects._

final class UpstreamFetch(repoPath: String) {

  val apply: IO[Unit] = io()
}
