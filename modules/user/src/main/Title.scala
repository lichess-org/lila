package lila.user

case class Title(value: String) extends AnyVal with StringValue

object Title {

  implicit val titleIso         = lila.common.Iso.string[Title](Title.apply, _.value)
  implicit val titleBsonHandler = lila.db.dsl.stringIsoHandler(Title.titleIso)
  implicit val titleJsonWrites  = lila.common.Json.stringIsoWriter(Title.titleIso)

  val LM  = Title("LM")
  val BOT = Title("BOT")

  // important: names are as stated on FIDE profile pages
  val all = Seq(
    Title("GM")  -> "Grandmaster",
    Title("WGM") -> "Woman Grandmaster",
    Title("IM")  -> "International Master",
    Title("WIM") -> "Woman Intl. Master",
    Title("FM")  -> "FIDE Master",
    Title("WFM") -> "Woman FIDE Master",
    Title("NM")  -> "National Master",
    Title("CM")  -> "Candidate Master",
    Title("WCM") -> "Woman Candidate Master",
    Title("WNM") -> "Woman National Master",
    LM           -> "Lichess Master",
    BOT          -> "Chess Robot"
  )

  val names          = all.toMap
  lazy val fromNames = all.map(_.swap).toMap

  val acronyms = all.map { case (Title(a), _) => a }

  def titleName(title: Title): String = names.getOrElse(title, title.value)

  def get(str: String): Option[Title]      = Title(str.toUpperCase).some filter names.contains
  def get(strs: List[String]): List[Title] = strs flatMap { get(_) }

  object fromUrl {

    // https://ratings.fide.com/card.phtml?event=740411
    private val FideProfileUrlRegex = """(?:https?://)?ratings\.fide\.com/card\.phtml\?event=(\d+)""".r
    // >&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;Grandmaster</td>
    private val FideProfileTitleRegex =
      s"""<div class="profile-top-info__block__row__data">(${names.values mkString "|"})</div>""".r.unanchored

    // https://ratings.fide.com/profile/740411
    private val NewFideProfileUrlRegex = """(?:https?://)?ratings\.fide\.com/profile/(\d+)""".r

    import play.api.libs.ws.StandaloneWSClient

    def toFideId(url: String): Option[Int] =
      url.trim match {
        case FideProfileUrlRegex(id)    => id.toIntOption
        case NewFideProfileUrlRegex(id) => id.toIntOption
        case _                          => none
      }

    def apply(url: String)(implicit ws: StandaloneWSClient): Fu[Option[Title]] =
      toFideId(url) ?? fromFideProfile

    private def fromFideProfile(id: Int)(implicit ws: StandaloneWSClient): Fu[Option[Title]] = {
      ws.url(s"""https://ratings.fide.com/profile/$id""").get().dmap(_.body) dmap {
        case FideProfileTitleRegex(name) => Title.fromNames get name
        case _                           => none
      }
    }
  }
}
