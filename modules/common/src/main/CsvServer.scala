package lila.common

import com.github.tototoshi.csv.CSVWriter
import play.api.Play
import play.api.Play.current
import scala.concurrent.Future

object CsvServer {

  // returns the web path
  def apply(filename: String)(lines: ⇒ List[List[Any]]): Fu[String] = {

    val file = Play.getFile("serve/" + filename)
    val webPath = "/serve/" + filename

    file.exists.fold(funit, {
      println("[csv] Export " + file)
      Future {
        val writer = new CSVWriter(file, "UTF-8")
        writer writeAll lines.map(_ map (_.toString))
        writer.close()
      }
    }) inject webPath
  }
}
