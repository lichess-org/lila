package lila.user

import play.api.libs.ws.WS
import play.api.Play.current

case class Title(value: String) extends AnyVal with StringValue

object Title {

  // important: names are as stated on FIDE profile pages
  val all = Seq(
    "GM" -> "Grandmaster",
    "WGM" -> "Woman Grandmaster",
    "IM" -> "International Master",
    "WIM" -> "Woman Intl. Master",
    "FM" -> "FIDE Master",
    "WFM" -> "Woman FIDE Master",
    "NM" -> "National Master",
    "CM" -> "Candidate Master",
    "WCM" -> "Woman Candidate Master",
    "WNM" -> "Woman National Master",
    "LM" -> "Lichess Master",
    "BOT" -> "Chess Robot"
  )

  val bot = lila.common.LightUser.botTitle

  val names = all.toMap
  lazy val fromNames = all.map(_.swap).toMap

  def titleName(title: String) = names get title getOrElse title

  object fromUrl {

    // https://ratings.fide.com/card.phtml?event=740411
    private val FideProfileUrlRegex = """(?:https?://)ratings\.fide\.com/card\.phtml\?event=(\d+)""".r
    // >&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;Grandmaster</td>
    private val FideProfileTitleRegex = """>&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;([^<]+)</td>""".r.unanchored

    def apply(url: String): Fu[Option[String]] = url.trim match {
      case FideProfileUrlRegex(id) => parseIntOption(id) ?? fromFideProfile
      case _ => fuccess(none)
    }

    private def fromFideProfile(id: Int): Fu[Option[String]] = {
      WS.url(s"""http://ratings.fide.com/card.phtml?event=$id""").get().map(_.body) map {
        case FideProfileTitleRegex(name) => Title.fromNames get name
      }
    }
  }
}
