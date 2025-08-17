package lila.activity

import alleycats.Zero

import lila.core.rating.{ RatingProg, Score }

object RatingProg:
  def add(rp1O: Option[RatingProg], rp2O: Option[RatingProg]): Option[lila.core.rating.RatingProg] =
    (rp1O, rp2O) match
      case (Some(rp1), Some(rp2)) => Some(rp1.copy(after = rp2.after))
      case _ => rp2O.orElse(rp1O)
  def make(player: lila.core.game.LightPlayer) =
    player.rating.map: rating =>
      val newRating = player.ratingDiff.fold(rating)(diff => rating.map(_ + diff.value))
      lila.core.rating.RatingProg(rating, newRating)

object Score:
  extension (s: Score)
    def plus(o: Score): Score =
      s.copy(
        win = s.win + o.win,
        loss = s.loss + o.loss,
        draw = s.draw + o.draw,
        rp = RatingProg.add(s.rp, o.rp)
      )

  def make(res: Option[Boolean], rp: Option[RatingProg]): Score =
    lila.core.rating.Score(
      win = res.has(true).so(1),
      loss = res.has(false).so(1),
      draw = res.isEmpty.so(1),
      rp = rp
    )
  def make(povs: List[lila.core.game.LightPov]): Score =
    povs.foldLeft(summon[Zero[Score]].zero):
      case (score, pov) if pov.game.finished =>
        score.plus(
          make(
            res = pov.game.win.map(_ == pov.color),
            rp = RatingProg.make(pov.player)
          )
        )
      case (score, _) => score
  val empty = lila.core.rating.Score(0, 0, 0, none)
  given Zero[Score] = Zero(empty)
