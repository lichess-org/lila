package lidraughts.study

case class MultiPdn(value: List[String]) extends AnyVal

object MultiPdn {

  private[this] val splitPat = """\n\n(?=\[)""".r.pattern
  def split(str: String, max: Int) = MultiPdn {
    splitPat.split(str.replaceIf('\r', ""), max + 1).take(max).toList
  }
}
