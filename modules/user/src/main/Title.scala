package lila.user

case class Title(value: String) extends AnyVal with StringValue

object Title {

  implicit val titleIso = lila.common.Iso.string[Title](Title.apply, _.value)
  implicit val titleBsonHandler = lila.db.dsl.stringIsoHandler(Title.titleIso)
  implicit val titleJsonWrites = lila.common.PimpedJson.stringIsoWriter(Title.titleIso)

  val LM = Title("LM")
  val BOT = Title("BOT")

  // important: names are as stated on FIDE profile pages
  val all = Seq(
    Title("GM") -> "Grandmaster",
    Title("WGM") -> "Woman Grandmaster",
    Title("IM") -> "International Master",
    Title("WIM") -> "Woman Intl. Master",
    Title("FM") -> "FIDE Master",
    Title("WFM") -> "Woman FIDE Master",
    Title("NM") -> "National Master",
    Title("CM") -> "Candidate Master",
    Title("WCM") -> "Woman Candidate Master",
    Title("WNM") -> "Woman National Master",
    LM -> "Lichess Master",
    BOT -> "Chess Robot"
  )

  val names = all.toMap
  lazy val fromNames = all.map(_.swap).toMap

  def titleName(title: Title): String = names.getOrElse(title, title.value)

  def get(str: String): Option[Title] = Title(str.toUpperCase).some filter names.contains
  def get(strs: List[String]): List[Title] = strs flatMap { get(_) }

  object fromUrl {

    import play.api.libs.ws.WS
    import play.api.Play.current

    // https://ratings.fide.com/card.phtml?event=740411
    private val FideProfileUrlRegex = """(?:https?://)ratings\.fide\.com/card\.phtml\?event=(\d+)""".r
    // >&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;Grandmaster</td>
    private val FideProfileTitleRegex = """>&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;([^<]+)</td>""".r.unanchored

    def apply(url: String): Fu[Option[Title]] = url.trim match {
      case FideProfileUrlRegex(id) => parseIntOption(id) ?? fromFideProfile
      case _ => fuccess(none)
    }

    private def fromFideProfile(id: Int): Fu[Option[Title]] = {
      WS.url(s"""http://ratings.fide.com/card.phtml?event=$id""").get().map(_.body) map {
        case FideProfileTitleRegex(name) => Title.fromNames get name
      }
    }
  }
}
