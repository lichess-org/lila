package lila.tournament

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.core.user.UserRepo
import lila.db.dsl.{ *, given }

final class TournamentModeration(playerRepo: PlayerRepo, userRepo: UserRepo)(using Executor):
  import userRepo.given
  import BSONHandlers.given
  import TournamentModeration.*
  import playerRepo.coll.AggregationFramework
  val max = Max(100)

  def apply(tourId: TourId, viewStr: String): Fu[(View, List[Player.WithUser])] =
    val view = View.values.find(_.toString == viewStr).getOrElse(View.values.head)
    val players = view match
      case View.recentlyCreated => aggregate(tourId, ordering = some(_.Descending("user.createdAt")))
      case View.fewGamesPlayed => aggregate(tourId, ordering = some(_.Ascending("user.count.game")))
      case View.provisional =>
        aggregate(tourId, playerSelect = $doc("pr" -> true).some, ordering = some(_.Descending("r")))
    players.map(view -> _)

  private def aggregate(
      tourId: TourId,
      playerSelect: Option[Bdoc] = none,
      ordering: Option[AggregationFramework => AggregationFramework.SortOrder]
  ): Fu[List[Player.WithUser]] =
    playerRepo.coll
      .aggregateList(maxDocs = max.value, _.sec): framework =>
        import framework.*
        Match(playerRepo.selectTour(tourId) ++ ~playerSelect) -> List(
          PipelineOperator(
            $lookup.simple(
              from = userRepo.coll.name,
              as = "user",
              local = "uid",
              foreign = "_id"
            )
          ).some,
          UnwindField("user").some,
          ordering.map(o => Sort(o(framework)))
        ).flatten
      .map(readPlayersWithUsers)

  private def readPlayersWithUsers(docs: List[Bdoc]): List[Player.WithUser] = for
    doc <- docs
    user <- doc.getAsOpt[User]("user")
    player <- doc.asOpt[Player]
  yield Player.WithUser(player, user)

object TournamentModeration:
  enum View(val name: String):
    case recentlyCreated extends View("Recently created")
    case fewGamesPlayed extends View("Few games played")
    case provisional extends View("Provisional rating")
