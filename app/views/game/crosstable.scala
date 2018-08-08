package views.html.game

import play.twirl.api.Html

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.game.Crosstable

object crosstable {

  def apply(ct: Crosstable.WithMatchup, currentId: Option[String])(implicit ctx: Context): Html =
    apply(ct.crosstable, ct.matchup, currentId)(ctx)

  def apply(ct: Crosstable, trueMatchup: Option[Crosstable.Matchup], currentId: Option[String])(implicit ctx: Context): Html = Html {

    val matchup = trueMatchup.filter(_.users != ct.users)

    val users = ct.users.toList.map { u =>

      val fill = ct.fill.map { i =>
        s"""<td${if (i == Crosstable.maxGames) " class=\"last\"" else ""}><a>&nbsp;</a></td>"""
      } mkString ""

      val matchupSepAt: Option[Int] = matchup map { m =>
        (ct.nbGames min Crosstable.maxGames) - m.users.nbGames
      }

      val results = ct.results.zipWithIndex.map {
        case (r, i) =>
          val href = s"""${routes.Round.watcher(r.gameId, "white")}?pov=${u.id}"""
          val (linkClass, text) = r.winnerId match {
            case Some(w) if w == u.id => "glpt win" -> "1"
            case None => "glpt" -> "½"
            case _ => "glpt loss" -> "0"
          }
          val link = s"""<a href="$href" class="$linkClass">$text</a>"""
          val outClass = matchupSepAt.fold("") { at =>
            if (at == i) "sep new"
            else if (at > i) "old"
            else "new"
          }
          val current = if (currentId contains r.gameId) " current" else ""
          s"""<td class="$outClass$current">$link</td>"""
      } mkString ""

      val matchScore = matchup ?? { m =>
        s"""<th title="Current match score" class="matchup${m.users.winnerId.fold("")(w => if (w == u.id) " win" else " loss")}">${m.users.showScore(u.id)}</th>"""
      }

      val user = s"""<th class="user">${userIdLink(u.id.some, withOnline = false)}</th>"""

      val score = s"""<th title="Lifetime score" class="score${ct.users.winnerId.fold("")(w => if (w == u.id) " win" else " loss")}">${ct.showScore(u.id)}</th>"""

      s"""<tr>$fill$results$matchScore$user$score</tr>"""

    } mkString ""

    s"""<table><tbody>$users</tbody></table>"""
  }
}
