package lila.coach

object YoutubeList {

  case class Url(value: String) extends AnyVal

  def apply(text: String): List[Url] =
    text.lines.toList.map(_.trim).filter(_.nonEmpty) flatMap toUrl

  private val UrlRegex = """.*(?:youtube\.com|youtu\.be)/(?:watch)?(?:\?v=)(.+)$""".r

  /*
   * https://www.youtube.com/watch?v=wEwoyYp_iw8
   * https://www.youtube.com/embed/wEwoyYp_iw8
   */
  private def toUrl(line: String): Option[Url] = line match {
    case UrlRegex(id) => Url(s"https://www.youtube.com/embed/$id").some
    case _            => none
  }
}
