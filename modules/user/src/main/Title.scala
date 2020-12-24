package lila.user

case class Title(value: String) extends AnyVal with StringValue

object Title {

  implicit val titleIso         = lila.common.Iso.string[Title](Title.apply, _.value)
  implicit val titleBsonHandler = lila.db.dsl.stringIsoHandler(Title.titleIso)
  implicit val titleJsonWrites  = lila.common.Json.stringIsoWriter(Title.titleIso)

  val LM  = Title("LM")
  val BOT = Title("BOT")

  val all = Seq(
    Title("PRO") -> "Professional",
    Title("プロ") -> "Professional",
    Title("九段") -> "9th dan",
    Title("八段") -> "8th dan",
    Title("七段") -> "7th dan",
    Title("六段") -> "6th dan",
    Title("五段") -> "5th dan",
    Title("四段") -> "4th dan",
    Title("三段") -> "3rd dan",
    Title("二段") -> "2nd dan",
    Title("初段") -> "1st dan",
    Title("１級") -> "1st kyu",
    Title("２級") -> "2nd kyu",
    Title("３級") -> "3rd kyu",
    Title("LP") -> "Ladies Pro",
    Title("女流") -> "Ladies Pro",
    Title("女流五段") -> "Ladies 5th dan",
    Title("女流四段") -> "Ladies 4th dan",
    Title("女流三段") -> "Ladies 3rd dan",
    Title("女流二段") -> "Ladies 2nd dan",
    Title("女流初段") -> "Ladies 1st dan",
    Title("女流１級") -> "Ladies 1st kyu",
    Title("女流２級") -> "Ladies 2nd kyu",
    Title("女流３級") -> "Ladies 3rd kyu",
    LM           -> "Lishogi Master",
    BOT          -> "Chess Robot"
  )

  val names          = all.toMap
  lazy val fromNames = all.map(_.swap).toMap

  val acronyms = all.map { case (Title(a), _) => a }

  def titleName(title: Title): String = names.getOrElse(title, title.value)

  def get(str: String): Option[Title]      = Title(str.toUpperCase).some filter names.contains
  def get(strs: List[String]): List[Title] = strs flatMap { get(_) }

  object fromUrl {

    // https://www.shogi.or.jp/player/pro/93.html
    private val JSAProProfileUrlRegex = """(?:https?://)?www\.shogi\.or\.jp/player/pro/(\d+).html""".r
    private val JSAProProfileTitleRegex = """(.[段級])""".r.unanchored

    // https://www.shogi.or.jp/player/lady/59.html
    private val JSALadyProfileUrlRegex = """(?:https?://)?www\.shogi\.or\.jp/player/lady/(\d+).html""".r
    private val JSALadyProfileTitleRegex = """(女流.[段級])""".r.unanchored

    import play.api.libs.ws.WSClient

    def toJSAId(url: String): Option[Int] =
      url.trim match {
        case JSAProProfileUrlRegex(id) => id.toIntOption
        case _                      => none
      }

    def toJSALadyId(url: String): Option[Int] =
      url.trim match {
        case JSALadyProfileUrlRegex(id) => id.toIntOption
        case _                      => none
      }

    def apply(url: String)(implicit ws: WSClient): Fu[Option[Title]] = {
      toJSAId(url) ?? fromJSAProProfile
      toJSALadyId(url) ?? fromJSALadyProfile
    }

    private def fromJSAProProfile(id: Int)(implicit ws: WSClient): Fu[Option[Title]] = {
      ws.url(s"""https://www.shogi.or.jp/player/pro/$id.html""").get().dmap(_.body) dmap {
        case JSAProProfileTitleRegex(name) => Title.fromNames get name
      }
    }

    private def fromJSALadyProfile(id: Int)(implicit ws: WSClient): Fu[Option[Title]] = {
      ws.url(s"""https://www.shogi.or.jp/player/lady/$id.html""").get().dmap(_.body) dmap {
        case JSALadyProfileTitleRegex(name) => Title.fromNames get name
      }
    }
  }
}
