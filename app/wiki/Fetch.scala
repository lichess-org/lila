package lila
package wiki

import java.io.File
import com.google.common.io.Files
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepository
import scalaz.effects._
import scala.collection.JavaConversions._
import eu.henkelmann.actuarius.ActuariusTransformer

final class Fetch(
    gitUrl: String,
    pageRepo: PageRepo) {

  import Page.DefaultLang

  def apply: IO[Unit] = for {
    files ← getFiles
    pages = files.map(filePage).flatten
    (defaultPages, langPages) = pages partition (_.isDefaultLang)
    newLangPages = (langPages map { page ⇒
      defaultPages find (_.number == page.number) map { default ⇒
        page.copy(slug = default.slug)
      }
    }).flatten
    _ ← pageRepo.clear
    _ ← (newLangPages ::: defaultPages).map(pageRepo.saveIO).sequence
  } yield ()

  private def filePage(file: File): Option[Page] = {
    val name = """^(.+)\.md$""".r.replaceAllIn(file.getName, _ group 1)
    if (name == "Home") None
    else Page(name, toHtml(fileContent(file)))
  }

  private def getFiles: IO[List[File]] = io {
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
