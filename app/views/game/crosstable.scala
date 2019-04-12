package views.html.game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Crosstable

import controllers.routes

object crosstable {

  def apply(ct: Crosstable.WithMatchup, currentId: Option[String])(implicit ctx: Context): Frag =
    apply(ct.crosstable, ct.matchup, currentId)(ctx)

  def apply(ct: Crosstable, trueMatchup: Option[Crosstable.Matchup], currentId: Option[String])(implicit ctx: Context): Frag = raw {

    val matchup = trueMatchup.filter(_.users != ct.users)

    val matchupSepAt: Option[Int] = matchup map { m =>
      (ct.nbGames min Crosstable.maxGames) - m.users.nbGames
    }

    val fill = (ct.fillSize > 0) ?? s"""<fill style="flex:${ct.fillSize * 0.75} 1 auto"></fill>"""

    val results = ct.results.zipWithIndex.map {
      case (r, i) =>
        val links = ct.users.toList.map { u =>
          val href = s"""${routes.Round.watcher(r.gameId, "white")}?pov=${u.id}"""
          val (linkClass, text) = r.winnerId match {
            case Some(w) if w == u.id => "glpt win" -> "1"
            case None => "glpt" -> "½"
            case _ => "glpt loss" -> "0"
          }
          s"""<a href="$href" class="$linkClass">$text</a>"""
        } mkString ""
        val outClass = matchupSepAt.has(i) ?? "sep"
        val current = if (currentId contains r.gameId) " current" else ""
        val cls = s"$outClass$current"
        val clsDec = cls.nonEmpty ?? s""" class="$outClass$current""""
        s"""<povs$clsDec>$links</povs>"""
    } mkString ""

    val matchScore = matchup ?? { m =>
      val scores = ct.users.toList.map { u =>
        s"""<span class="${m.users.winnerId.fold("")(w => if (w == u.id) "win" else "loss")}">${m.users.showScore(u.id)}</span>"""
      }
      s"""<div title="Current match score" class="crosstable__matchup">${scores mkString ""}</div>"""
    }

    val users = ct.users.toList.map { u =>
      userIdLink(u.id.some, withOnline = false).render
    }
    val usersDiv = s"""<div class="crosstable__users">${users mkString ""}</div>"""

    val scores = ct.users.toList.map { u =>
      s"""<span class="${ct.users.winnerId.fold("")(w => if (w == u.id) "win" else "loss")}">${ct.showScore(u.id)}</span>"""
    }
    val scoreDiv = s"""<div title="Lifetime score" class="crosstable__score">${scores mkString ""}</div>"""

    s"""<div class="crosstable">$fill$results$matchScore$usersDiv$scoreDiv</div>"""
  }
}
