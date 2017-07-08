package views.html.game

import play.twirl.api.Html

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._

object crosstable {

  def apply(ct: lila.game.Crosstable, currentId: Option[String])(implicit ctx: Context) = Html {

    val users = ct.users.map { u =>

      val fill = ct.fill.map { i =>
        s"""<td${if (i == 20) " class=\"last\"" else ""}><a>&nbsp;</a></td>"""
      } mkString ""

      val results = ct.results.zipWithIndex.map {
        case (r, i) =>
          val href = s"""${routes.Round.watcher(r.gameId, "white")}?pov=${u.id}"""
          val (klass, text) = r.winnerId match {
            case Some(w) if w == u.id => "glpt win" -> "1"
            case None => "glpt" -> "½"
            case _ => "glpt loss" -> "0"
          }
          val link = s"""<a href="$href" class="$klass">$text</a>"""
          s"""<td${if (currentId contains r.gameId) " class=\"current\"" else ""}>$link</td>"""
      } mkString ""

      val score = s"""<th class="score${ct.winnerId.fold("")(w => if (w == u.id) " win" else " loss")}">${ct.showScore(u.id)}</th>"""

      val user = s"""<th class="user">${userIdLink(u.id.some, withOnline = false)}</th>"""

      s"""<tr>$fill$results$score$user</tr>"""

    } mkString ""

    s"""<table><tbody>$users</tbody></table>"""
  }
}
