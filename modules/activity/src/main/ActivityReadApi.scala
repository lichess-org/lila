package lila.activity

import play.api.i18n.Lang

import lila.common.Heapsort
import lila.db.AsyncCollFailingSilently
import lila.db.dsl.*
import lila.game.LightPov
import lila.practice.PracticeStructure
import lila.swiss.Swiss
import lila.tournament.LeaderboardApi
import lila.user.User

final class ActivityReadApi(
    coll: AsyncCollFailingSilently,
    gameRepo: lila.game.GameRepo,
    practiceApi: lila.practice.PracticeApi,
    forumPostApi: lila.forum.ForumPostApi,
    ublogApi: lila.ublog.UblogApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi,
    swissApi: lila.swiss.SwissApi,
    teamRepo: lila.team.TeamRepo,
    lightUserApi: lila.user.LightUserApi,
    getTourName: lila.tournament.GetTourName
)(using Executor):

  import BSONHandlers.{ *, given }
  import model.*

  private given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  def recentAndPreload(u: User)(using lang: Lang): Fu[Vector[ActivityView]] = for
    activities <-
      coll(
        _.find(regexId(u.id))
          .sort($sort desc "_id")
          .cursor[Activity]()
          .vector(Activity.recentNb)
      ).dmap(_.filterNot(_.isEmpty))
        .mon(_.user segment "activity.raws")
    practiceStructure <- activities
      .exists(_.practice.isDefined)
      .soFu(practiceApi.structure.get)
    views <- activities
      .map: a =>
        one(practiceStructure, a).mon(_.user segment "activity.view")
      .parallel
    _ <- preloadAll(views)
  yield addSignup(u.createdAt, views)

  private def preloadAll(views: Seq[ActivityView])(using lang: Lang) = for
    _ <- lightUserApi.preloadMany(views.flatMap(_.follows.so(_.allUserIds)))
    _ <- getTourName.preload(views.flatMap(_.tours.so(_.best.map(_.tourId))))
  yield ()

  private def one(practiceStructure: Option[PracticeStructure], a: Activity): Fu[ActivityView] =
    for
      allForumPosts <- a.forumPosts.soFu: p =>
        forumPostApi
          .liteViewsByIds(p.value)
          .mon(_.user segment "activity.posts")
      hiddenForumTeamIds <- teamRepo.filterHideForum(
        (~allForumPosts).flatMap(_.topic.possibleTeamId).distinct
      )
      forumPosts = allForumPosts.map(
        _.filterNot(_.topic.possibleTeamId.exists(hiddenForumTeamIds.contains))
      )
      ublogPosts <- a.ublogPosts
        .soFu: p =>
          ublogApi
            .liveLightsByIds(p.value)
            .mon(_.user segment "activity.ublogs")
        .dmap(_.filter(_.nonEmpty))
      practice = for
        p      <- a.practice
        struct <- practiceStructure
      yield p.value.flatMap { (studyId, nb) =>
        struct study studyId map (_ -> nb)
      }.toMap
      forumPostView = forumPosts
        .map: p =>
          p.groupBy(_.topic)
            .view
            .mapValues: posts =>
              posts.view.map(_.post).sortBy(_.createdAt).toList
            .toMap
        .filter(_.nonEmpty)
      corresMoves <- a.corres.so: corres =>
        getLightPovs(a.id.userId, corres.movesIn).dmap:
          _.map(corres.moves -> _)
      corresEnds <- a.corres.so: corres =>
        getLightPovs(a.id.userId, corres.end).dmap:
          _.map: povs =>
            Score.make(povs) -> povs
      simuls <-
        a.simuls
          .soFu: simuls =>
            simulApi byIds simuls.value
          .dmap(_.filter(_.nonEmpty))
      studies <-
        a.studies
          .soFu: studies =>
            studyApi publicIdNames studies.value
          .dmap(_.filter(_.nonEmpty))
      tours <- a.games
        .exists(_.hasNonCorres)
        .so:
          val dateRange = TimeInterval(a.date, a.date.plusDays(1))
          tourLeaderApi
            .timeRange(a.id.userId, dateRange)
            .dmap: entries =>
              entries.nonEmpty option ActivityView.Tours(
                nb = entries.size,
                best = Heapsort.topN(entries, activities.maxSubEntries)(using
                  Ordering.by[LeaderboardApi.Entry, Double](-_.rankRatio.value)
                )
              )
            .mon(_.user segment "activity.tours")
      swisses <-
        a.swisses.so: swisses =>
          toSwissesView(swisses.value).dmap(_.some.filter(_.nonEmpty))
    yield ActivityView(
      interval = a.interval,
      games = a.games,
      puzzles = a.puzzles,
      storm = a.storm,
      racer = a.racer,
      streak = a.streak,
      practice = practice,
      forumPosts = forumPostView,
      ublogPosts = ublogPosts,
      simuls = simuls,
      patron = a.patron,
      corresMoves = corresMoves,
      corresEnds = corresEnds,
      follows = a.follows,
      studies = studies,
      teams = a.teams,
      tours = tours,
      swisses = swisses,
      stream = a.stream
    )

  def recentSwissRanks(userId: UserId): Fu[List[(Swiss.IdName, Rank)]] =
    coll(
      _.find(regexId(userId) ++ $doc(BSONHandlers.ActivityFields.swisses $exists true))
        .sort($sort desc "_id")
        .cursor[Activity]()
        .list(10)
    ).flatMap { activities =>
      toSwissesView(activities.flatMap(_.swisses.so(_.value)))
    }

  private def toSwissesView(swisses: List[activities.SwissRank]): Fu[List[(Swiss.IdName, Rank)]] =
    swissApi
      .idNames(swisses.map(_.id))
      .map {
        _.flatMap { idName =>
          swisses.find(_.id == idName.id) map { s =>
            (idName, s.rank)
          }
        }
      }

  private def addSignup(at: Instant, recent: Vector[ActivityView]) =
    val (found, views) = recent.foldLeft(false -> Vector.empty[ActivityView]) {
      case ((false, as), a) if a.interval contains at => (true, as :+ a.copy(signup = true))
      case ((found, as), a)                           => (found, as :+ a)
    }
    if !found && views.sizeIs < Activity.recentNb && nowInstant.minusDays(8).isBefore(at) then
      views :+ ActivityView(
        interval = TimeInterval(at.withTimeAtStartOfDay, at.withTimeAtStartOfDay plusDays 1),
        signup = true
      )
    else views

  private def getLightPovs(userId: UserId, gameIds: List[GameId]): Fu[Option[List[LightPov]]] =
    gameIds.nonEmpty.so:
      gameRepo.light
        .gamesFromSecondary(gameIds)
        .dmap:
          _.flatMap { LightPov(_, userId) }.some.filter(_.nonEmpty)
