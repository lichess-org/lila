package lila.mod

import play.api.data.*
import play.api.data.Forms.{ list as formList, * }

import lila.common.Form.{ perfKey, stringIn, given }
import lila.db.dsl.{ *, given }
import lila.game.Query
import lila.rating.PerfType

object GameMod:

  case class Filter(
      arena: Option[String],
      swiss: Option[String],
      perf: Option[PerfKey],
      opponents: Option[String],
      nbGamesOpt: Option[Int]
  ):
    def opponentIds: List[UserId] = UserStr
      .from:
        (~opponents)
          .take(800)
          .replace(",", " ")
          .split(' ')
          .map(_.trim)
      .flatMap(_.validateId)
      .toList
      .distinct

    def nbGames = nbGamesOpt | 100

  val emptyFilter = Filter(none, none, none, none, none)

  def toDbSelect(user: lila.user.User, filter: Filter): Bdoc =
    Query.notSimul ++ Query.createdSince(nowInstant.minusYears(3)) ++
      filter.perf.so { perf =>
        Query.clock(perf != PerfType.Correspondence.key)
      } ++ filter.arena.so { id =>
        $doc(lila.game.Game.BSONFields.tournamentId -> id)
      } ++ filter.swiss.so { id =>
        $doc(lila.game.Game.BSONFields.swissId -> id)
      } ++ $and(
        Query.user(user),
        filter.opponentIds.match
          case Nil => Query.noAnon
          case List(id) => Query.user(id)
          case ids => Query.users(ids)
      )

  val maxGames = 500

  val filterForm = Form:
    mapping(
      "arena" -> optional(nonEmptyText),
      "swiss" -> optional(nonEmptyText),
      "perf" -> optional(perfKey),
      "opponents" -> optional(nonEmptyText),
      "nbGamesOpt" -> optional(
        number(min = 1).transform(
          _.atMost(maxGames),
          identity
        )
      )
    )(Filter.apply)(unapply)

  val actionForm = Form:
    tuple(
      "game" -> formList(of[GameId]),
      "action" -> optional(stringIn(Set("pgn", "analyse")))
    )
