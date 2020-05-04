package lila.swiss

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import lila.common.{ GreatPlayer, Uptime }
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.quote.Quote.quoteWriter
import lila.rating.PerfType
import lila.socket.Socket.SocketVersion
import lila.user.{ LightUserApi, User }

final class SwissJson(
    colls: SwissColls,
    standingApi: SwissStandingApi,
    rankingApi: SwissRankingApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: ExecutionContext) {

  import SwissJson._
  import BsonHandlers._

  def apply(
      swiss: Swiss,
      me: Option[User],
      reqPage: Option[Int], // None = focus on me
      socketVersion: Option[SocketVersion],
      isInTeam: Boolean
  )(implicit lang: Lang): Fu[JsObject] =
    for {
      myInfo <- me.?? { fetchMyInfo(swiss, _) }
      page = reqPage orElse myInfo.map(_.page) getOrElse 1
      standing <- standingApi(swiss, page)
    } yield Json
      .obj(
        "id"        -> swiss.id.value,
        "createdBy" -> swiss.createdBy,
        "startsAt"  -> formatDate(swiss.startsAt),
        "name"      -> swiss.name,
        "perf"      -> swiss.perfType,
        "clock"     -> swiss.clock,
        "variant"   -> swiss.variant.key,
        "round"     -> swiss.round,
        "nbRounds"  -> swiss.nbRounds,
        "nbPlayers" -> swiss.nbPlayers,
        "status" -> {
          if (swiss.isStarted) "started"
          else if (swiss.isFinished) "finished"
          else "created"
        },
        "standing" -> standing
      )
      .add("isFinished" -> swiss.isFinished)
      .add("socketVersion" -> socketVersion.map(_.value))
      .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
      .add("description" -> swiss.description)
      .add("secondsToStart" -> swiss.isCreated.option(swiss.secondsToStart))
      .add("me" -> myInfo.map(myInfoJson))
      .add("canJoin" -> (myInfo.isEmpty && isInTeam && swiss.isNotFinished))
      .add("greatPlayer" -> GreatPlayer.wikiUrl(swiss.name).map { url =>
        Json.obj("name" -> swiss.name, "url" -> url)
      })

  def fetchMyInfo(swiss: Swiss, me: User): Fu[Option[MyInfo]] =
    colls.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, me.id).value) flatMap {
      _ ?? { player =>
        SwissPairing.fields { f =>
          colls.pairing
            .find(
              $doc(f.swissId -> swiss.id, f.players -> player.number),
              $doc(f.id -> true).some
            )
            .sort($sort desc f.date)
            .one[Bdoc]
            .dmap { _.flatMap(_.getAsOpt[Game.ID](f.id)) }
            .flatMap { gameId =>
              getOrGuessRank(swiss, player) dmap { rank =>
                MyInfo(rank + 1, gameId, me).some
              }
            }
        }
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

  private def myInfoJson(i: MyInfo) =
    Json
      .obj(
        "rank"   -> i.rank,
        "gameId" -> i.gameId,
        "id"     -> i.user.id
      )
}

object SwissJson {

  private[swiss] def playerJson(
      swiss: Swiss,
      rankedPlayer: SwissPlayer.Ranked,
      user: lila.common.LightUser,
      pairings: Map[SwissRound.Number, SwissPairing]
  ): JsObject = {
    val p = rankedPlayer.player
    Json
      .obj(
        "rank"   -> rankedPlayer.rank,
        "user"   -> user,
        "rating" -> p.rating,
        "points" -> p.points,
        "score"  -> p.score,
        "pairings" -> swiss.allRounds.map(pairings.get).map {
          _ map { pairing =>
            Json
              .obj("g" -> pairing.gameId)
              .add("o" -> pairing.isOngoing)
              .add("w" -> pairing.isWinFor(p.number))
          }
        }
      )
      .add("provisional" -> p.provisional)
  }

  implicit private val roundNumberWriter: Writes[SwissRound.Number] = Writes[SwissRound.Number] { n =>
    JsNumber(n.value)
  }
  implicit private val playerNumberWriter: Writes[SwissPlayer.Number] = Writes[SwissPlayer.Number] { n =>
    JsNumber(n.value)
  }
  implicit private val pointsWriter: Writes[Swiss.Points] = Writes[Swiss.Points] { p =>
    JsNumber(p.value)
  }
  implicit private val scoreWriter: Writes[Swiss.Score] = Writes[Swiss.Score] { s =>
    JsNumber(s.value)
  }

  implicit private val pairingWrites: OWrites[SwissPairing] = OWrites { p =>
    Json.obj(
      "gameId" -> p.gameId,
      "white"  -> p.white,
      "black"  -> p.black,
      "winner" -> p.winner
    )
  }

  implicit private val clockWrites: OWrites[chess.Clock.Config] = OWrites { clock =>
    Json.obj(
      "limit"     -> clock.limitSeconds,
      "increment" -> clock.incrementSeconds
    )
  }

  implicit private def perfTypeWrites(implicit lang: Lang): OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "icon" -> pt.iconChar.toString,
      "name" -> pt.trans
    )
  }
}
