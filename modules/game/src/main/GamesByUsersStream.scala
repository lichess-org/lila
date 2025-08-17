package lila.game

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.common.Json.given
import lila.core.game.{ FinishGame, Game, StartGame, WithInitialFen }

final class GamesByUsersStream(gameRepo: lila.game.GameRepo)(using akka.stream.Materializer, Executor):

  def apply(userIds: Set[UserId], withCurrentGames: Boolean): Source[JsValue, ?] =
    if userIds.sizeIs < 2 then Source.empty
    else
      val initialGames = if withCurrentGames then currentGamesSource(userIds) else Source.empty
      val startStream =
        Source.queue[Game](150, akka.stream.OverflowStrategy.dropHead).mapMaterializedValue { queue =>
          def matches(game: Game) = game.userIds match
            case List(u1, u2) if u1 != u2 => userIds(u1) && userIds(u2)
            case _ => false
          val subStart = Bus.sub[StartGame]:
            case StartGame(game) if matches(game) => queue.offer(game)
          val subFinish = Bus.sub[FinishGame]:
            case FinishGame(game, _) if matches(game) => queue.offer(game)
          queue
            .watchCompletion()
            .addEffectAnyway:
              Bus.unsub[StartGame](subStart)
              Bus.unsub[FinishGame](subFinish)
        }
      initialGames
        .concat(startStream)
        .mapAsync(1)(gameRepo.withInitialFen)
        .map(GameStream.gameWithInitialFenWriter.writes)

  private def currentGamesSource(userIds: Set[UserId]): Source[Game, ?] =
    gameRepo.ongoingByUserIdsCursor(userIds).documentSource().throttle(30, 1.second)

private object GameStream:

  val gameWithInitialFenWriter: OWrites[WithInitialFen] = OWrites:
    case WithInitialFen(g, initialFen) =>
      Json
        .obj(
          "id" -> g.id,
          "rated" -> g.rated,
          "variant" -> g.variant.key,
          "speed" -> g.speed.key,
          "perf" -> g.perfKey,
          "createdAt" -> g.createdAt,
          "status" -> g.status.id,
          "statusName" -> g.status.name,
          "players" -> JsObject(g.players.mapList: p =>
            p.color.name -> Json
              .obj(
                "userId" -> p.userId,
                "rating" -> p.rating
              )
              .add("provisional" -> p.provisional)
              .add("ai" -> p.aiLevel))
        )
        .add("winner" -> g.winnerColor.map(_.name))
        .add("initialFen" -> initialFen)
        .add("clock" -> g.clock.map: clock =>
          Json.obj(
            "initial" -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds
          ))
        .add("daysPerTurn" -> g.daysPerTurn)
