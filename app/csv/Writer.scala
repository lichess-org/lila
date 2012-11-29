package lila
package csv

import com.github.tototoshi.csv.CSVWriter
import play.api.Play
import play.api.Play.current
import scalaz.effects._

object Writer {

  // returns the web path
  def apply(filename: String)(lines: List[List[Any]]): IO[String] = {

    val file = Play.getFile("serve/" + filename)
    val webPath = "/serve/" + filename

    file.exists.fold(
      io(webPath), {
        println("Export " + file)
        val writer = new CSVWriter(file, "UTF-8")
        io {
          writer writeAll lines.map(_ map (_.toString))
          writer.close()
        } map (_ ⇒ webPath)
      })
  }
}
