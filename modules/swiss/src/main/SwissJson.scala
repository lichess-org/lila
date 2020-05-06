package lila.swiss

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.ExecutionContext

import lila.common.{ GreatPlayer }
import lila.db.dsl._
import lila.game.Game
import lila.quote.Quote.quoteWriter
import lila.rating.PerfType
import lila.socket.Socket.SocketVersion
import lila.user.User

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
      isInTeam: Boolean,
      playerInfo: Option[SwissPlayer.ViewExt]
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
        "nbRounds"  -> (if (swiss.isFinished) swiss.round else swiss.guessNbRounds),
        "nbPlayers" -> swiss.nbPlayers,
        "nbOngoing" -> swiss.nbOngoing,
        "status" -> {
          if (swiss.isStarted) "started"
          else if (swiss.isFinished) "finished"
          else "created"
        },
        "canJoin"  -> (myInfo.isEmpty && swiss.isEnterable && isInTeam),
        "standing" -> standing
      )
      .add("joinTeam" -> (!isInTeam).option(swiss.teamId))
      .add("socketVersion" -> socketVersion.map(_.value))
      .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
      .add("description" -> swiss.settings.description)
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

  def fetchMyInfo(swiss: Swiss, me: User): Fu[Option[MyInfo]] =
    colls.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, me.id).value) flatMap {
      _ ?? { player =>
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

  private[swiss] def playerJson(swiss: Swiss, view: SwissPlayer.View): JsObject =
    playerJsonBase(swiss, view) ++ Json
      .obj(
        "pairings" -> swiss.allRounds.map(view.pairings.get).map(_ map pairingJson(view.player))
      )
      .add("absent" -> view.player.absent)

  def playerJsonExt(swiss: Swiss, view: SwissPlayer.ViewExt): JsObject =
    playerJsonBase(swiss, view) ++ Json.obj(
      "pairings" -> swiss.allRounds.reverse.map(view.pairings.get).map {
        _ map { p =>
          pairingJson(view.player)(p.pairing) ++ Json.obj(
            "user"   -> p.player.user,
            "rating" -> p.player.player.rating
          )
        }
      }
    )

  private def playerJsonBase(swiss: Swiss, view: SwissPlayer.Viewish): JsObject = {
    val p = view.player
    Json
      .obj(
        "rank"     -> view.rank,
        "user"     -> view.user,
        "rating"   -> p.rating,
        "points"   -> p.points,
        "tieBreak" -> p.tieBreak
      )
      .add("performance" -> p.performance)
      .add("provisional" -> p.provisional)
  }

  private def pairingJson(player: SwissPlayer)(pairing: SwissPairing) =
    Json
      .obj(
        "g" -> pairing.gameId,
        "c" -> (pairing.white == player.number)
      )
      .add("o" -> pairing.isOngoing)
      .add("w" -> pairing.resultFor(player.number))

  implicit private val roundNumberWriter: Writes[SwissRound.Number] = Writes[SwissRound.Number] { n =>
    JsNumber(n.value)
  }
  // implicit private val playerNumberWriter: Writes[SwissPlayer.Number] = Writes[SwissPlayer.Number] { n =>
  //   JsNumber(n.value)
  // }
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

  implicit private def perfTypeWrites(implicit lang: Lang): OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "icon" -> pt.iconChar.toString,
      "name" -> pt.trans
    )
  }
}
