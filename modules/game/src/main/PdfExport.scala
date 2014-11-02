package lila.game

import java.io.{ File, OutputStream }
import scala.sys.process._

object PdfExport {

  private def log(n: String)(msg: String) {
    println(s"$n: $msg")
  }

  private val logger = ProcessLogger(log("png out"), log("png err"))

  def apply(execPath: String)(id: String)(out: OutputStream) {
    val exec = Process(Seq("php", "main.php", id), new File(execPath))
    exec #> out ! logger
  }
}
