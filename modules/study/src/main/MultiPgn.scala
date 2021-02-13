package lila.study

case class MultiPgn(value: List[String]) extends AnyVal

object MultiPgn {

  private[this] val splitPat = """\n\n(?=\[)""".r.pattern
  def split(str: String, max: Int) =
    MultiPgn {
      splitPat.split(str.replaceIf('\r', ""), max + 1).take(max).toList
    }
}
