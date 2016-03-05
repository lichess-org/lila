package lila

package object i18n extends PackageObject with WithPlay {

  type Messages = Map[String, Map[String, String]]

  object tube {

    private[i18n] implicit lazy val translationTube =
      Translation.tube inColl Env.current.translationColl
  }

  import scala.concurrent.Future

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit): Funit = Future {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter => Unit): Funit =
    printToFile(new java.io.File(f))(op)
}
