package lila.activity

import lila.activity.activities.*
import lila.core.chess.Rank
import lila.core.forum.{ ForumPostMini, ForumTopicMini }
import lila.core.game.LightPov
import lila.core.rating.Score
import lila.core.simul.Simul
import lila.core.swiss.IdName as SwissIdName
import lila.core.ublog.UblogPost

case class ActivityView(
    interval: TimeInterval,
    games: Option[Games] = None,
    puzzles: Option[Puzzles] = None,
    storm: Option[Storm] = None,
    racer: Option[Racer] = None,
    streak: Option[Streak] = None,
    practice: Option[Map[lila.core.practice.Study, Int]] = None,
    simuls: Option[List[Simul]] = None,
    patron: Option[Patron] = None,
    forumPosts: Option[Map[ForumTopicMini, List[ForumPostMini]]] = None,
    ublogPosts: Option[List[UblogPost.LightPost]] = None,
    corresMoves: Option[(Int, List[LightPov])] = None,
    corresEnds: Option[Map[PerfKey, (Score, List[LightPov])]] = None,
    follows: Option[Follows] = None,
    studies: Option[List[lila.core.study.IdName]] = None,
    teams: Option[Teams] = None,
    tours: Option[ActivityView.Tours] = None,
    swisses: Option[List[(SwissIdName, Rank)]] = None,
    stream: Boolean = false,
    signup: Boolean = false
):
  def isEmpty = !stream && List(
    games,
    puzzles,
    storm,
    racer,
    streak,
    practice,
    simuls,
    patron,
    forumPosts,
    ublogPosts,
    corresMoves,
    corresEnds,
    follows,
    studies,
    teams,
    tours,
    swisses
  ).forall(_.isEmpty)

object ActivityView:

  case class Tours(nb: Int, best: List[lila.core.tournament.leaderboard.Entry])
