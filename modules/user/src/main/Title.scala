package lidraughts.user

import play.api.libs.ws.WS
import play.api.Play.current

case class Title(value: String) extends AnyVal with StringValue

object Title {

  // important: abbreviations are as stated on fmjd profile pages
  val all = Seq(
    "GMI" -> "International Grandmaster",
    "MI" -> "International Master",
    "MF" -> "FMJD Master",
    "GMN" -> "National Grandmaster",
    "MN" -> "National Master",
    "cMN" -> "Candidate National Master",
    "GMIF" -> "Woman International Grandmaster",
    "MIF" -> "Woman International Master",
    "MFF" -> "Woman FMJD Master",
    "MNF" -> "Woman National Master",
    "cMNF" -> "Woman Candidate National Master",
    "LM" -> "Lidraughts Master",
    "BOT" -> "Draughts Robot"
  )

  val bot = lidraughts.common.LightUser.botTitle

  val names = all.toMap
  lazy val fromNames = all.map(_.swap).toMap

  def titleName(title: String) = names get title getOrElse title

  object fromUrl {

    // https://www.fmjd.org/?p=pcard&id=16091
    private val FmjdProfileUrlRegex = """(?:https?://)(?:www\.)?fmjd\.org/\?p=pcard&id=(\d+)""".r
    //<tr>\n<td>TITLE<\/td>\n<td>RATING\s(POSITION)<td>\n<\/tr><tr>
    private val FmjdProfileTitleRegex = """<tr>\s*<td>(\w{2,4})</td>\s*<td>\d{1,4}\s+\(\d+\)<td>\s*</tr>\s*<tr>""".r.unanchored

    def apply(url: String): Fu[Option[String]] = url.trim match {
      case FmjdProfileUrlRegex(id) => parseIntOption(id) ?? fromFmjdProfile
      case _ => fuccess(none)
    }

    private def fromFmjdProfile(id: Int): Fu[Option[String]] = {
      WS.url(s"""https://www.fmjd.org/?p=pcard&id=$id""").get().map(_.body) map {
        case FmjdProfileTitleRegex(title) => Title.names.contains(title) option title
      }
    }
  }
}
