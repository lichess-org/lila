package lila.study

case class MultiNotation(value: List[String]) extends AnyVal

object MultiNotation {

  private[this] val splitPat = """\n\n(?=[^変化\r\n]*:)""".r.pattern
  def split(str: String, max: Int) =
    MultiNotation {
      splitPat.split(str.replaceIf('\r', "").replaceIf('：', ':'), max + 1).take(max).toList
    }
}
