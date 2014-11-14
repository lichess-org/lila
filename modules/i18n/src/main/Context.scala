package lila.i18n

import java.io.File
import scala.concurrent.duration._
import scala.concurrent.Future

import com.google.common.io.Files
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository

import lila.memo.AsyncCache

private[i18n] final class Context(gitUrl: String, gitFile: String, keys: I18nKeys) {

  type Contexts = Map[String, String]

  def get: Fu[Contexts] = cache(true)

  private val cache = AsyncCache.single[Contexts](fetch, timeToLive = 1 hour)

  private def parse(text: String): Contexts =
    text.lines.toList.map(_.trim).filter(_.nonEmpty).map(_.split('=')).foldLeft(Map[String, String]()) {
      case (cs, Array(key, text)) if (keySet contains key) => cs + (key -> text)
      case (cs, Array(key, _)) =>
        // logwarn("i18n context skipped key " + key)
        cs
      case (cs, line) if line startsWith "//" => cs
      case (cs, line) =>
        // logwarn("i18n context skipped line " + line.mkString("="))
        cs
    }

  private lazy val keySet: Set[String] = keys.keys.map(_.en()).toSet

  private def fetch: Fu[Contexts] = gitClone map { dir =>
    val filePath = s"${dir.getAbsolutePath}/$gitFile"
    val content = fileContent(new File(filePath))
    dir.delete
    parse(content)
  }

  private def gitClone: Fu[File] = Future {
    val dir = Files.createTempDir
    dir.deleteOnExit
    Git.cloneRepository
      .setURI(gitUrl)
      .setDirectory(dir)
      .setBare(false)
      .call
    dir
  }

  private def fileContent(file: File) =
    scala.io.Source.fromFile(file.getCanonicalPath, "UTF-8").mkString
}
