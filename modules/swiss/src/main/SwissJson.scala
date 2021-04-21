package lila.swiss

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import lila.common.{ GreatPlayer, LightUser }
import lila.db.dsl._
import lila.game.Game
import lila.quote.Quote.quoteWriter
import lila.socket.Socket.SocketVersion
import lila.user.{ User, UserRepo }

final class SwissJson(
    colls: SwissColls,
    standingApi: SwissStandingApi,
    rankingApi: SwissRankingApi,
    boardApi: SwissBoardApi,
    statsApi: SwissStatsApi,
    userRepo: UserRepo,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: ExecutionContext) {

  import SwissJson._
  import BsonHandlers._

  def api(swiss: Swiss) =
    swissJsonBase(swiss) ++ Json.obj(
      "rated" -> swiss.settings.rated
    )

  def apply(
      swiss: Swiss,
      me: Option[User],
      isInTeam: Boolean,
      verdicts: SwissCondition.All.WithVerdicts,
      reqPage: Option[Int] = None, // None = focus on me
      socketVersion: Option[SocketVersion] = None,
      playerInfo: Option[SwissPlayer.ViewExt] = None
  )(implicit lang: Lang): Fu[JsObject] = {
    for {
      myInfo <- me.?? { fetchMyInfo(swiss, _) }
      page = reqPage orElse myInfo.map(_.page) getOrElse 1
      standing <- standingApi(swiss, page)
      podium   <- podiumJson(swiss)
      boards   <- boardApi(swiss.id)
      stats    <- statsApi(swiss)
    } yield swissJsonBase(swiss) ++ Json
      .obj(
        "canJoin" -> {
          {
            (swiss.isNotFinished && myInfo.exists(_.player.absent)) ||
            (myInfo.isEmpty && swiss.isEnterable && isInTeam)
          } && verdicts.accepted
        },
        "standing" -> standing,
        "boards"   -> boards.map(boardJson)
      )
      .add("me" -> myInfo.map(myInfoJson))
      .add("joinTeam" -> (!isInTeam).option(swiss.teamId))
      .add("socketVersion" -> socketVersion.map(_.value))
      .add("playerInfo" -> playerInfo.map { playerJsonExt(swiss, _) })
      .add("podium" -> podium)
      .add("isRecentlyFinished" -> swiss.isRecentlyFinished)
      .add("password" -> swiss.settings.password.isDefined)
      .add("stats" -> stats)
      .add("greatPlayer" -> GreatPlayer.wikiUrl(swiss.name).map { url =>
        Json.obj("name" -> swiss.name, "url" -> url)
      })
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
                    $doc(f.swissId -> swiss.id, f.players -> player.userId, f.status -> SwissPairing.ongoing),
                    $doc(f.id -> true).some
                  )
                  .one[Bdoc]
                  .dmap { _.flatMap(_.getAsOpt[Game.ID](f.id)) }
              }
              .flatMap { gameId =>
                rankingApi(swiss).dmap(_ get player.userId) map2 { rank =>
                  MyInfo(rank, gameId, me, player)
                }
              }
          }
      }
    }

  private def updatePlayerRating(swiss: Swiss, player: SwissPlayer, user: User): Funit =
    swiss.settings.rated
      .option(user perfs swiss.perfType)
      .filter(_.intRating != player.rating)
      .?? { perf =>
        SwissPlayer.fields { f =>
          colls.player.update
            .one(
              $id(SwissPlayer.makeId(swiss.id, user.id)),
              $set(f.rating -> perf.intRating)
            )
            .void
        }
      }

  private def podiumJson(swiss: Swiss): Fu[Option[JsArray]] =
    swiss.isFinished ?? {
      SwissPlayer.fields { f =>
        colls.player
          .find($doc(f.swissId -> swiss.id))
          .sort($sort desc f.score)
          .cursor[SwissPlayer]()
          .list(3) flatMap { top3 =>
          // check that the winner is still correctly denormalized
          top3.headOption
            .map(_.userId)
            .filter(w => swiss.winnerId.fold(true)(w !=))
            .foreach {
              colls.swiss.updateField($id(swiss.id), "winnerId", _).void
            }
            .unit
          userRepo.filterEngine(top3.map(_.userId)) map { engines =>
            JsArray(
              top3.map { player =>
                playerJsonBase(
                  player,
                  lightUserApi.sync(player.userId) | LightUser.fallback(player.userId),
                  performance = true
                ).add("engine", engines(player.userId))
              }
            ).some
          }
        }
      }
    }

  def playerResult(player: SwissPlayer, rank: Int): Fu[JsObject] =
    lightUserApi.asyncFallback(player.userId) map { user =>
      Json
        .obj(
          "rank"     -> rank,
          "score"    -> player.score.value,
          "tieBreak" -> player.tieBreak.value,
          "rating"   -> player.rating,
          "username" -> user.name
        )
        .add("title" -> user.title)
        .add("performance" -> player.performance)
        .add("absent" -> player.absent)
    }
}

object SwissJson {

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date

  private def swissJsonBase(swiss: Swiss) =
    Json
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
        }
      )
      .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
      .add("nextRound" -> swiss.nextRoundAt.map { next =>
        Json.obj(
          "at" -> formatDate(next),
          "in" -> (next.getSeconds - nowSeconds).toInt.atLeast(0)
        )
      })

  private[swiss] def playerJson(swiss: Swiss, view: SwissPlayer.View): JsObject =
    playerJsonBase(view, performance = false) ++ Json
      .obj(
        "sheetMin" -> swiss.allRounds
          .map(view.pairings.get)
          .zip(view.sheet.outcomes)
          .map {
            pairingJsonOrOutcome(view.player)
          }
          .mkString("|")
      )

  def playerJsonExt(swiss: Swiss, view: SwissPlayer.ViewExt): JsObject =
    playerJsonBase(view, performance = true) ++ Json.obj(
      "sheet" -> swiss.allRounds
        .zip(view.sheet.outcomes)
        .reverse
        .map { case (round, outcome) =>
          view.pairings.get(round).fold[JsValue](JsString(outcomeJson(outcome))) { p =>
            pairingJson(view.player, p.pairing) ++
              Json.obj(
                "user"   -> p.player.user,
                "rating" -> p.player.player.rating
              )
          }
        }
    )

  private def playerJsonBase(
      view: SwissPlayer.Viewish,
      performance: Boolean
  ): JsObject =
    playerJsonBase(view.player, view.user, performance) ++
      Json.obj("rank" -> view.rank)

  private def playerJsonBase(
      p: SwissPlayer,
      user: LightUser,
      performance: Boolean
  ): JsObject =
    Json
      .obj(
        "user"     -> user,
        "rating"   -> p.rating,
        "points"   -> p.points,
        "tieBreak" -> p.tieBreak
      )
      .add("performance" -> (performance ?? p.performance))
      .add("provisional" -> p.provisional)
      .add("absent" -> p.absent)

  private def outcomeJson(outcome: SwissSheet.Outcome): String =
    outcome match {
      case SwissSheet.Absent => "absent"
      case SwissSheet.Late   => "late"
      case SwissSheet.Bye    => "bye"
      case _                 => ""
    }

  private def pairingJsonMin(player: SwissPlayer, pairing: SwissPairing): String = {
    val status =
      if (pairing.isOngoing) "o"
      else pairing.resultFor(player.userId).fold("d") { r => if (r) "w" else "l" }
    s"${pairing.gameId}$status"
  }

  private def pairingJson(player: SwissPlayer, pairing: SwissPairing) =
    Json
      .obj(
        "g" -> pairing.gameId
      )
      .add("o" -> pairing.isOngoing)
      .add("w" -> pairing.resultFor(player.userId))
      .add("c" -> (pairing.white == player.userId))

  private def pairingJsonOrOutcome(
      player: SwissPlayer
  ): ((Option[SwissPairing], SwissSheet.Outcome)) => String = {
    case (Some(pairing), _) => pairingJsonMin(player, pairing)
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
    Json
      .obj(
        "id"          -> b.game.id,
        "fen"         -> chess.format.Forsyth.boardAndColor(b.game.situation),
        "lastMove"    -> ~b.game.lastMoveKeys,
        "orientation" -> b.game.naturalOrientation.name,
        "white"       -> boardPlayerJson(b.board.white),
        "black"       -> boardPlayerJson(b.board.black)
      )
      .add(
        "clock" -> b.game.clock.ifTrue(b.game.isBeingPlayed).map { c =>
          Json.obj(
            "white" -> c.remainingTime(chess.White).roundSeconds,
            "black" -> c.remainingTime(chess.Black).roundSeconds
          )
        }
      )
      .add("winner" -> b.game.winnerColor.map(_.name))

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
