package lila.common

import com.github.tototoshi.csv.CSVWriter
import play.api.Play
import play.api.Play.current

object CsvServer {

  // returns the web path
  def apply(name: String)(lines: => List[List[Any]]): Fu[String] =
    if (exists(name)) fuccess(webPath(name))
    else {
      val file = getFile(name)
      loginfo("[csv] Export " + file)
      scala.concurrent.Future {
        val writer = CSVWriter.open(file, "UTF-8")
        val printable = lines.map(_ map (_.toString))
        writer writeAll printable
        writer.close()
        webPath(name)
      }
    }

  def exists(name: String) = getFile(name).exists

  private def getFile(name: String) = Play.getFile("serve/" + name)
  private def webPath(name: String) = "/serve/" + name
}
