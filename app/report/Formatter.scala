package lila
package report

object Formatter {

  def dataLine(data: List[(String, Any)]) = new {

    def header = data map (_._1) mkString " "

    def line = data map {
      case (name, value) ⇒ {
        val s = value.toString
        List.fill(name.size - s.size)(" ").mkString + s + " "
      }
    } mkString
  }
}
