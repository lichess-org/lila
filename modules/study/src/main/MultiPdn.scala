package lidraughts.study

case class MultiPdn(value: List[String]) extends AnyVal

object MultiPdn {

  def split(str: String, max: Int) = MultiPdn {
    """\n\n\[""".r.split(str.replace("\r\n", "\n")).toList take max match {
      case first :: rest => first :: rest.map(t => s"[$t")
      case Nil => Nil
    }
  }
}
