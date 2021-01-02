package lila.game

import akka.stream.scaladsl._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi.{ FinishGame, StartGame }
import chess.format.FEN
import lila.common.Bus
import lila.common.Json.jodaWrites
import lila.game.Game
import lila.user.User

final class GamesByUsersStream(gameRepo: lila.game.GameRepo)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val keepAliveInterval = 70.seconds // play's idleTimeout = 75s
  private val chans             = List("startGame", "finishGame")

  private val blueprint = Source
    .queue[Game](64, akka.stream.OverflowStrategy.dropHead)
    .mapAsync(1)(gameRepo.withInitialFen)
    .map(gameWithInitialFenWriter.writes)
    .map(some)
    .keepAlive(keepAliveInterval, () => none)

  def apply(userIds: Set[User.ID]): Source[Option[JsValue], _] =
    blueprint mapMaterializedValue { queue =>
      def matches(game: Game) =
        game.userIds match {
          case List(u1, u2) if u1 != u2 => userIds(u1) && userIds(u2)
          case _                        => false
        }
      val sub = Bus.subscribeFun(chans: _*) {
        case StartGame(game) if matches(game)        => queue offer game
        case FinishGame(game, _, _) if matches(game) => queue offer game
      }
      queue.watchCompletion().foreach { _ =>
        Bus.unsubscribe(sub, chans)
      }
    }

  implicit private val fenWriter: Writes[FEN] = Writes[FEN] { f =>
    JsString(f.value)
  }

  private val gameWithInitialFenWriter: OWrites[Game.WithInitialFen] = OWrites {
    case Game.WithInitialFen(g, initialFen) =>
      Json
        .obj(
          "id"        -> g.id,
          "rated"     -> g.rated,
          "variant"   -> g.variant.key,
          "speed"     -> g.speed.key,
          "perf"      -> PerfPicker.key(g),
          "createdAt" -> g.createdAt,
          "status"    -> g.status.id,
          "players" -> JsObject(g.players map { p =>
            p.color.name -> Json
              .obj(
                "userId" -> p.userId,
                "rating" -> p.rating
              )
              .add("provisional" -> p.provisional)
          })
        )
        .add("initialFen" -> initialFen)
        .add("clock" -> g.clock.map { clock =>
          Json.obj(
            "initial"   -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds,
            "byoyomi"   -> clock.byoyomiSeconds,
            "periods"   -> clock.periods
          )
        })
        .add("daysPerTurn" -> g.daysPerTurn)
  }
}
