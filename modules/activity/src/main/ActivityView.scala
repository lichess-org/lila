package lila.activity

import org.joda.time.Interval

import lila.game.LightPov
import lila.practice.PracticeStudy
import lila.simul.Simul
import lila.study.Study
import lila.tournament.LeaderboardApi.{ Entry => TourEntry }

import activities._
import model._
import lila.swiss.Swiss

case class ActivityView(
    interval: Interval,
    games: Option[Games] = None,
    puzzles: Option[Puzzles] = None,
    storm: Option[Storm] = None,
    racer: Option[Racer] = None,
    streak: Option[Streak] = None,
    practice: Option[Map[PracticeStudy, Int]] = None,
    simuls: Option[List[Simul]] = None,
    patron: Option[Patron] = None,
    posts: Option[Map[lila.forum.Topic, List[lila.forum.Post]]] = None,
    corresMoves: Option[(Int, List[LightPov])] = None,
    corresEnds: Option[(Score, List[LightPov])] = None,
    follows: Option[Follows] = None,
    studies: Option[List[Study.IdName]] = None,
    teams: Option[Teams] = None,
    tours: Option[ActivityView.Tours] = None,
    swisses: Option[List[(Swiss.IdName, Int)]] = None,
    stream: Boolean = false,
    signup: Boolean = false
)

object ActivityView {

  case class Tours(
      nb: Int,
      best: List[TourEntry]
  )
}
