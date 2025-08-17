package lila.coach

object UrlList:

  object youtube:

    val max = 6

    opaque type Url = String
    object Url extends OpaqueString[Url]

    def apply(text: String): List[Url] =
      text.linesIterator.toList.view.map(_.trim).filter(_.nonEmpty).flatMap(toUrl).take(max) to List

    private val UrlRegex = """(?:youtube\.com|youtu\.be)/(?:watch)?(?:\?v=)?([^"&?/ ]{11})""".r.unanchored

    /*
     * https://www.youtube.com/watch?v=wEwoyYp_iw8
     * https://www.youtube.com/embed/wEwoyYp_iw8
     */
    private def toUrl(line: String): Option[Url] =
      line match
        case UrlRegex(id) => Url(s"https://www.youtube.com/embed/$id").some
        case _ => none

  object study:

    val max = 6

    private val UrlRegex = """(?:lichess\.org)/study/(\w{8})""".r.unanchored

    def apply(text: String): List[StudyId] =
      text.linesIterator.toList.view.map(_.trim).filter(_.nonEmpty).flatMap(toId).take(max) to List

    private def toId(line: String): Option[StudyId] =
      line match
        case UrlRegex(id) => StudyId(id).some
        case _ => none
