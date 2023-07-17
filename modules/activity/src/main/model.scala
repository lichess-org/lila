package lila.activity

import alleycats.Zero

object model:

  case class RatingProg(before: IntRating, after: IntRating):
    def add(o: RatingProg) = copy(after = o.after)
    def diff               = IntRatingDiff(after.value - before.value)
    def isEmpty            = diff == IntRatingDiff(0)
  object RatingProg:
    def add(rp1O: Option[RatingProg], rp2O: Option[RatingProg]) =
      (rp1O, rp2O) match
        case (Some(rp1), Some(rp2)) => Some(rp1 add rp2)
        case _                      => rp2O orElse rp1O
    def make(player: lila.game.LightPlayer) =
      player.rating map { rating =>
        RatingProg(rating, rating.applyDiff(~player.ratingDiff))
      }

  case class Score(win: Int, loss: Int, draw: Int, rp: Option[RatingProg]):
    def add(s: Score) =
      copy(
        win = win + s.win,
        loss = loss + s.loss,
        draw = draw + s.draw,
        rp = RatingProg.add(rp, s.rp)
      )
    def size = win + loss + draw
  object Score:
    def make(res: Option[Boolean], rp: Option[RatingProg]): Score =
      Score(
        win = res.has(true) so 1,
        loss = res.has(false) so 1,
        draw = res.isEmpty so 1,
        rp = rp
      )
    def make(povs: List[lila.game.LightPov]): Score =
      povs.foldLeft(summon[Zero[Score]].zero) {
        case (score, pov) if pov.game.finished =>
          score add make(
            res = pov.game.win.map(_ == pov.color),
            rp = RatingProg.make(pov.player)
          )
        case (score, _) => score
      }
    val empty         = Score(0, 0, 0, none)
    given Zero[Score] = Zero(empty)
