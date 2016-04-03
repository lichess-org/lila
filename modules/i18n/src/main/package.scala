package lila

package object i18n extends PackageObject with WithPlay {

  type Messages = Map[String, Map[String, String]]

  import scala.concurrent.Future

  private[i18n] def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit): Funit = Future {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  private[i18n] def printToFile(f: String)(op: java.io.PrintWriter => Unit): Funit =
    printToFile(new java.io.File(f))(op)

  private[i18n] def logger = lila.log("i18n")
}
