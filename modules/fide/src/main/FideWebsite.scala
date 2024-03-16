package lila.fide

import chess.{ FideId, PlayerTitle }

object FideWebsite:

  // https://ratings.fide.com/card.phtml?event=740411
  private val FideProfileUrlRegex = """(?:https?://)?ratings\.fide\.com/card\.phtml\?event=(\d+)""".r
  // >&nbsp;FIDE title</td><td colspan=3 bgcolor=#efefef>&nbsp;Grandmaster</td>
  val names = PlayerTitle.names.values.mkString("|")
  s"""<div class="profile-top-info__block__row__data">($names)</div>""".r.unanchored

  // https://ratings.fide.com/profile/740411
  private val NewFideProfileUrlRegex = """(?:https?://)?ratings\.fide\.com/profile/(\d+)""".r

  def urlToFideId(url: String): Option[FideId] = FideId.from:
    url.trim match
      case FideProfileUrlRegex(id)    => id.toIntOption
      case NewFideProfileUrlRegex(id) => id.toIntOption
      case _                          => none
