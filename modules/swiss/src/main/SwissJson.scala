package lila.swiss

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import lila.common.{ GreatPlayer, LightUser }
import lila.db.dsl._
import lila.game.Game
import lila.quote.Quote.quoteWriter
import lila.socket.Socket.SocketVersion
import lila.user.User

final class SwissJson(
    colls: SwissColls,
    standingApi: SwissStandingApi,
    rankingApi: SwissRankingApi,
    boardApi: SwissBoardApi,
    statsApi: SwissStatsApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: ExecutionContext) {

  import SwissJson._
  import BsonHandlers._

  def apply(
      swiss: Swiss,
      me: Option[User],
      reqPage: Option[Int], // None = focus on me
      socketVersion: Option[SocketVersion],
      isInTeam: Boolean,
      playerInfo: Option[SwissPlayer.ViewExt]
  ): Fu[JsObject] = {
    for {
      myInfo <- me.?? { fetchMyInfo(swiss, _) }
      page = reqPage orElse myInfo.map(_.page) getOrElse 1
      standing <- standingApi(swiss, page)
      podium   <- podiumJson(swiss)
      boards   <- boardApi.withGames(swiss.id)
      stats    <- statsApi(swiss)
    } yield Json
      .obj(
        "id"        -> swiss.id.value,
        "createdBy" -> swiss.createdBy,
        "startsAt"  -> formatDate(swiss.startsAt),
        "name"      -> swiss.name,
        "clock"     -> swiss.clock,
        "variant"   -> swiss.variant.key,
        "round"     -> swiss.round,
        "nbRounds"  -> swiss.actualNbRounds,
        "nbPlayers" -> swiss.nbPlayers,
        "nbOngoing" -> swiss.nbOngoing,
        "status" -> {
          if (swiss.isStarted) "started"
          else if (swiss.isFinished) "finished"
          else "created"
        },
        "canJoin" -> {
          (swiss.isNotFinished && myInfo.exists(_.player.absent)) ||
          (myInfo.isEmpty && swiss.isEnterable && isInTeam)
        },
        "standing" -> standing,
        "boards"   -> boards.map(boardJson)
      )
      .add("joinTeam" -> (!isInTeam).option(swiss.teamId))
      .add("socketVersion" -> socketVersion.map(_.value))
      .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
      .add("nextRound" -> swiss.nextRoundAt.map { next =>
        Json.obj(
          "at" -> formatDate(next),
          "in" -> (next.getSeconds - nowSeconds).toInt.atLeast(0)
        )
      })
      .add("me" -> myInfo.map(myInfoJson))
      .add("greatPlayer" -> GreatPlayer.wikiUrl(swiss.name).map { url =>
        Json.obj("name" -> swiss.name, "url" -> url)
      })
      .add("playerInfo" -> playerInfo.map { playerJsonExt(swiss, _) })
      .add("podium" -> podium)
      .add("isRecentlyFinished" -> swiss.isRecentlyFinished)
      .add("stats" -> stats)
  }.monSuccess(_.swiss.json)

  def fetchMyInfo(swiss: Swiss, me: User): Fu[Option[MyInfo]] =
    colls.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, me.id).value) flatMap {
      _ ?? { player =>
        updatePlayerRating(swiss, player, me) >>
          SwissPairing.fields { f =>
            (swiss.nbOngoing > 0)
              .?? {
                colls.pairing
                  .find(
                    $doc(f.swissId -> swiss.id, f.players -> player.number, f.status -> SwissPairing.ongoing),
                    $doc(f.id -> true).some
                  )
                  .sort($sort desc f.date)
                  .one[Bdoc]
                  .dmap { _.flatMap(_.getAsOpt[Game.ID](f.id)) }
              }
              .flatMap { gameId =>
                getOrGuessRank(swiss, player) dmap { rank =>
                  MyInfo(rank + 1, gameId, me, player).some
                }
              }
          }
      }
    }

  private def updatePlayerRating(swiss: Swiss, player: SwissPlayer, user: User): Funit =
    swiss.perfType
      .ifTrue(swiss.settings.rated)
      .map(user.perfs.apply)
      .filter(_.intRating != player.rating)
      .?? { perf =>
        SwissPlayer.fields { f =>
          colls.player.update
            .one(
              $id(SwissPlayer.makeId(swiss.id, user.id)),
              $set(f.rating -> perf.intRating, f.provisional -> perf.provisional)
            )
            .void
        }
      }

  // if the user is not yet in the cached ranking,
  // guess its rank based on other players scores in the DB
  private def getOrGuessRank(swiss: Swiss, player: SwissPlayer): Fu[Int] =
    rankingApi(swiss) flatMap {
      _ get player.number match {
        case Some(rank) => fuccess(rank)
        case None =>
          SwissPlayer.fields { f =>
            colls.player.countSel($doc(f.swissId -> player.swissId, f.score $gt player.score))
          }
      }
    }

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date

  private def podiumJson(swiss: Swiss): Fu[Option[JsArray]] =
    swiss.isFinished ?? {
      SwissPlayer.fields { f =>
        colls.player.ext
          .find($doc(f.swissId -> swiss.id))
          .sort($sort desc f.score)
          .list[SwissPlayer](3) map { top3 =>
          // check that the winner is still correctly denormalized
          top3.headOption.map(_.userId).filter(w => swiss.winnerId.fold(true)(w !=)) foreach {
            colls.swiss.updateField($id(swiss.id), "winnerId", _).void
          }
          JsArray {
            top3.map { player =>
              playerJsonBase(
                swiss,
                player,
                lightUserApi.sync(player.userId) | LightUser.fallback(player.userId)
              )
            }
          }.some
        }
      }
    }
}

object SwissJson {

  private[swiss] def playerJson(swiss: Swiss, view: SwissPlayer.View): JsObject =
    playerJsonBase(swiss, view) ++ Json
      .obj(
        "sheet" -> swiss.allRounds.map(view.pairings.get).zip(view.sheet.outcomes).map {
          pairingJsonOrOutcome(view.player)
        }
      )

  def playerJsonExt(swiss: Swiss, view: SwissPlayer.ViewExt): JsObject =
    playerJsonBase(swiss, view) ++ Json.obj(
      "sheet" -> swiss.allRounds
        .zip(view.sheet.outcomes)
        .reverse
        .map {
          case (round, outcome) =>
            view.pairings.get(round).fold(outcomeJson(outcome)) { p =>
              pairingJson(view.player, p.pairing) ++
                Json.obj(
                  "user"   -> p.player.user,
                  "rating" -> p.player.player.rating
                )
            }
        }
    )

  private def playerJsonBase(swiss: Swiss, view: SwissPlayer.Viewish): JsObject =
    playerJsonBase(swiss, view.player, view.user) ++
      Json.obj("rank" -> view.rank)

  private def playerJsonBase(swiss: Swiss, p: SwissPlayer, user: LightUser): JsObject =
    Json
      .obj(
        "user"     -> user,
        "rating"   -> p.rating,
        "points"   -> p.points,
        "tieBreak" -> p.tieBreak
      )
      .add("performance" -> p.performance)
      .add("provisional" -> p.provisional)
      .add("absent" -> p.absent)

  private def outcomeJson(outcome: SwissSheet.Outcome): JsValue =
    outcome match {
      case SwissSheet.Absent => JsString("absent")
      case SwissSheet.Late   => JsString("late")
      case SwissSheet.Bye    => JsString("bye")
      case _                 => JsNull
    }

  private def pairingJsonBase(player: SwissPlayer, pairing: SwissPairing) =
    Json
      .obj(
        "g" -> pairing.gameId
      )
      .add("o" -> pairing.isOngoing)
      .add("w" -> pairing.resultFor(player.number))

  private def pairingJson(player: SwissPlayer, pairing: SwissPairing) =
    pairingJsonBase(player, pairing) + ("c" -> JsBoolean(pairing.white == player.number))

  private def pairingJsonOrOutcome(
      player: SwissPlayer
  ): ((Option[SwissPairing], SwissSheet.Outcome)) => JsValue = {
    case (Some(pairing), _) => pairingJsonBase(player, pairing)
    case (_, outcome)       => outcomeJson(outcome)
  }

  private def myInfoJson(i: MyInfo) =
    Json
      .obj(
        "rank"   -> i.rank,
        "gameId" -> i.gameId,
        "id"     -> i.user.id,
        "name"   -> i.user.username,
        "absent" -> i.player.absent
      )

  private[swiss] def boardJson(b: SwissBoard.WithGame) =
    Json.obj(
      "id"       -> b.game.id,
      "fen"      -> (chess.format.Forsyth exportBoard b.game.board),
      "lastMove" -> ~b.game.lastMoveKeys,
      "white"    -> boardPlayerJson(b.board.white),
      "black"    -> boardPlayerJson(b.board.black)
    )

  private def boardPlayerJson(player: SwissBoard.Player) =
    Json.obj(
      "rank"   -> player.rank,
      "rating" -> player.rating,
      "user"   -> player.user
    )

  implicit private val roundNumberWriter: Writes[SwissRound.Number] = Writes[SwissRound.Number] { n =>
    JsNumber(n.value)
  }
  implicit private val pointsWriter: Writes[Swiss.Points] = Writes[Swiss.Points] { p =>
    JsNumber(p.value)
  }
  implicit private val tieBreakWriter: Writes[Swiss.TieBreak] = Writes[Swiss.TieBreak] { t =>
    JsNumber(t.value)
  }
  implicit private val performanceWriter: Writes[Swiss.Performance] = Writes[Swiss.Performance] { t =>
    JsNumber(t.value.toInt)
  }

  implicit private val clockWrites: OWrites[chess.Clock.Config] = OWrites { clock =>
    Json.obj(
      "limit"     -> clock.limitSeconds,
      "increment" -> clock.incrementSeconds
    )
  }

  implicit private val statsWrites: Writes[SwissStats] = Json.writes[SwissStats]
}
