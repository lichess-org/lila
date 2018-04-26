package lila.study

case class MultiPgn(value: List[String]) extends AnyVal

object MultiPgn {

  def split(str: String, max: Int) = MultiPgn {
    """\n\n\[""".r.split(str.replace("\r\n", "\n")).toList take max match {
      case first :: rest => first :: rest.map(t => s"[$t")
      case Nil => Nil
    }
  }
}
