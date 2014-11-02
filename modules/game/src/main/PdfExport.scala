package lila.game

import java.io.{ File, OutputStream }
import scala.sys.process._

object PdfExport {

  private val logger = ProcessLogger(_ => (), _ => ())

  def apply(execPath: String)(id: String)(out: OutputStream) {
    val exec = Process(Seq("php", "main.php", id), new File(execPath))
    exec #> out ! logger
  }
}
