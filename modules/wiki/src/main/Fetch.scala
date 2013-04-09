package lila.wiki

import lila.db.Types.Coll
import lila.db.api._
import tube._

import java.io.File
import com.google.common.io.Files
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepository
import eu.henkelmann.actuarius.ActuariusTransformer

import scala.collection.JavaConversions._
import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._

private[wiki] final class Fetch(gitUrl: String)(implicit coll: Coll) {

  def apply: Funit = getFiles flatMap { files ⇒
    val pages = files.map(filePage).flatten
    $remove($select.all) >> Future.sequence(pages.map($insert(_))).void
  }

  private def filePage(file: File): Option[Page] = {
    val name = """^(.+)\.md$""".r.replaceAllIn(file.getName, _ group 1)
    (name != "Home") option Page.make(name, toHtml(fileContent(file)))
  }

  private def getFiles: Fu[List[File]] = Future {
    val dir = Files.createTempDir
    dir.deleteOnExit
    Git.cloneRepository
      .setURI(gitUrl)
      .setDirectory(dir)
      .setBare(false)
      .call
    dir.listFiles.toList filter (_.isFile) sortBy (_.getName)
  }

  private def fileContent(file: File) =
    scala.io.Source.fromFile(file.getCanonicalPath).mkString

  private def toHtml(input: String): String =
    new ActuariusTransformer() apply input

}
