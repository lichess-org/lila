package lila.coach

object UrlList:

  private val max = 6

  object youtube:

    def apply(text: String): List[Url] = parse(text)(toUrl)

    private val UrlRegex = """(?:youtube\.com|youtu\.be)/(?:watch)?(?:\?v=)?([^"&?/ ]{11})""".r.unanchored

    /*
     * https://www.youtube.com/watch?v=wEwoyYp_iw8
     * https://www.youtube.com/embed/wEwoyYp_iw8
     * https://www.youtube-nocookie.com/embed/wEwoyYp_iw8
     */
    private def toUrl(line: String): Option[Url] =
      line match
        case UrlRegex(id) => Url(s"https://www.youtube-nocookie.com/embed/$id").some
        case _ => none

  object study:

    private val UrlRegex = """(?:lichess\.org)/study/(\w{8})""".r.unanchored

    def apply(text: String): List[StudyId] = parse(text)(toId)

    private def toId(line: String): Option[StudyId] =
      line match
        case UrlRegex(id) => StudyId(id).some
        case _ => none

  private def parse[A](text: String)(read: String => Option[A]): List[A] =
    text.linesIterator.toList.view.map(_.trim).filter(_.nonEmpty).flatMap(read).take(max) to List
