package lila.activity

import activities._
import model._
import org.joda.time.Interval

import lila.game.LightPov
import lila.practice.PracticeStudy
import lila.simul.Simul
import lila.study.Study
import lila.swiss.Swiss
import lila.tournament.LeaderboardApi.{ Entry => TourEntry }
import lila.ublog.UblogPost

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
    forumPosts: Option[Map[lila.forum.Topic, List[lila.forum.Post]]] = None,
    ublogPosts: Option[List[UblogPost.LightPost]] = None,
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
