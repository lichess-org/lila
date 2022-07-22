package lila.activity

import org.joda.time.{ DateTime, Interval }

import lila.common.Heapsort
import lila.db.AsyncCollFailingSilently
import lila.db.dsl._
import lila.game.LightPov
import lila.practice.PracticeStructure
import lila.swiss.Swiss
import lila.tournament.LeaderboardApi
import lila.ublog.UblogPost
import lila.user.User
import lila.forum.Categ

final class ActivityReadApi(
    coll: AsyncCollFailingSilently,
    gameRepo: lila.game.GameRepo,
    practiceApi: lila.practice.PracticeApi,
    forumPostApi: lila.forum.PostApi,
    ublogApi: lila.ublog.UblogApi,
    simulApi: lila.simul.SimulApi,
    studyApi: lila.study.StudyApi,
    tourLeaderApi: lila.tournament.LeaderboardApi,
    swissApi: lila.swiss.SwissApi,
    teamRepo: lila.team.TeamRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._
  import model._

  implicit private val ordering = scala.math.Ordering.Double.TotalOrdering

  def recent(u: User): Fu[Vector[ActivityView]] =
    for {
      activities <-
        coll(
          _.find(regexId(u.id))
            .sort($sort desc "_id")
            .cursor[Activity]()
            .vector(Activity.recentNb)
        ).dmap(_.filterNot(_.isEmpty))
          .mon(_.user segment "activity.raws")
      practiceStructure <- activities.exists(_.practice.isDefined) ?? {
        practiceApi.structure.get dmap some
      }
      views <- activities.map { a =>
        one(practiceStructure, a).mon(_.user segment "activity.view")
      }.sequenceFu
    } yield addSignup(u.createdAt, views)

  private def one(practiceStructure: Option[PracticeStructure], a: Activity): Fu[ActivityView] =
    for {
      allForumPosts <- a.forumPosts ?? { p =>
        forumPostApi
          .liteViewsByIds(p.value.map(_.value))
          .mon(_.user segment "activity.posts") dmap some
      }
      hiddenForumTeamIds <- teamRepo.filterHideForum(
        (~allForumPosts).flatMap(_.topic.possibleTeamId).distinct
      )
      forumPosts = allForumPosts.map(
        _.filterNot(_.topic.possibleTeamId.exists(hiddenForumTeamIds.contains))
      )
      ublogPosts <- a.ublogPosts ?? { p =>
        ublogApi
          .liveLightsByIds(p.value.map(_.value).map(UblogPost.Id))
          .mon(_.user segment "activity.ublogs")
          .dmap(_.some.filter(_.nonEmpty))
      }
      practice = (for {
        p      <- a.practice
        struct <- practiceStructure
      } yield p.value flatMap { case (studyId, nb) =>
        struct study studyId map (_ -> nb)
      } toMap)
      forumPostView = forumPosts.map { p =>
        p.groupBy(_.topic)
          .view
          .mapValues { posts =>
            posts.view.map(_.post).sortBy(_.createdAt).toList
          }
          .toMap
      } filter (_.nonEmpty)
      corresMoves <- a.corres ?? { corres =>
        getLightPovs(a.id.userId, corres.movesIn) dmap {
          _.map(corres.moves -> _)
        }
      }
      corresEnds <- a.corres ?? { corres =>
        getLightPovs(a.id.userId, corres.end) dmap {
          _.map { povs =>
            Score.make(povs) -> povs
          }
        }
      }
      simuls <-
        a.simuls
          .?? { simuls =>
            simulApi byIds simuls.value.map(_.value) dmap some
          }
          .dmap(_.filter(_.nonEmpty))
      studies <-
        a.studies
          .?? { studies =>
            studyApi publicIdNames studies.value dmap some
          }
          .dmap(_.filter(_.nonEmpty))
      tours <- a.games.exists(_.hasNonCorres) ?? {
        val dateRange = a.date -> a.date.plusDays(1)
        tourLeaderApi
          .timeRange(a.id.userId, dateRange)
          .dmap { entries =>
            entries.nonEmpty option ActivityView.Tours(
              nb = entries.size,
              best = Heapsort.topN(
                entries,
                activities.maxSubEntries,
                Ordering.by[LeaderboardApi.Entry, Double](-_.rankRatio.value)
              )
            )
          }
          .mon(_.user segment "activity.tours")
      }
      swisses <-
        a.swisses
          .?? { swisses =>
            toSwissesView(swisses.value).dmap(_.some.filter(_.nonEmpty))
          }

    } yield ActivityView(
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

  def recentSwissRanks(userId: User.ID): Fu[List[(Swiss.IdName, Int)]] =
    coll(
      _.find(regexId(userId) ++ $doc(BSONHandlers.ActivityFields.swisses $exists true))
        .sort($sort desc "_id")
        .cursor[Activity]()
        .list(10)
    ).flatMap { activities =>
      toSwissesView(activities.flatMap(_.swisses.??(_.value)))
    }

  private def toSwissesView(swisses: List[activities.SwissRank]): Fu[List[(Swiss.IdName, Int)]] =
    swissApi
      .idNames(swisses.map(_.id))
      .map {
        _.flatMap { idName =>
          swisses.find(_.id == idName.id) map { s =>
            (idName, s.rank)
          }
        }
      }

  private def addSignup(at: DateTime, recent: Vector[ActivityView]) = {
    val (found, views) = recent.foldLeft(false -> Vector.empty[ActivityView]) {
      case ((false, as), a) if a.interval contains at => (true, as :+ a.copy(signup = true))
      case ((found, as), a)                           => (found, as :+ a)
    }
    if (!found && views.sizeIs < Activity.recentNb && DateTime.now.minusDays(8).isBefore(at))
      views :+ ActivityView(
        interval = new Interval(at.withTimeAtStartOfDay, at.withTimeAtStartOfDay plusDays 1),
        signup = true
      )
    else views
  }

  private def getLightPovs(userId: User.ID, gameIds: List[GameId]): Fu[Option[List[LightPov]]] =
    gameIds.nonEmpty ?? {
      gameRepo.light.gamesFromSecondary(gameIds.map(_.value)).dmap {
        _.flatMap { LightPov.ofUserId(_, userId) }.some.filter(_.nonEmpty)
      }
    }
}
