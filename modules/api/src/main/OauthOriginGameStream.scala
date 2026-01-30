package lila.api

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.core.game.{ FinishGame, Game, StartGame, WithInitialFen }

final class OauthOriginGameStream(gameRepo: lila.game.GameRepo, tokenApi: lila.oauth.AccessTokenApi)(using
    akka.stream.Materializer,
    Executor
):

  case class Client(origin: String)
  val clients = Map(
    UserId("taketaketakeapp") -> Client("https://auth.taketaketake.com"),
    UserId("thibault") -> Client("org.lichess.mobile://")
  )

  def apply(origin: String, since: Option[Instant])(using me: Me): Option[Source[JsValue, ?]] =
    clients
      .get(me.userId)
      .pp
      .map: client =>
        Source.futureSource:
          tokenApi
            .userIdsByClientOrigin(client.origin)
            .thenPp
            .map: userIds =>
              val initialGames = currentGamesSource(userIds)
              val startStream =
                Source.queue[Game](300, akka.stream.OverflowStrategy.dropHead).mapMaterializedValue { queue =>
                  def matches(game: Game) = game.twoUserIds.exists: (u1, u2) =>
                    userIds(u1) || userIds(u2)

                  val subStart = Bus.sub[StartGame]:
                    case StartGame(game, _) if matches(game) => queue.offer(game)

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
                .map(lila.game.GameStream.gameWithInitialFenWriter.writes)

  private def currentGamesSource(userIds: Set[UserId]): Source[Game, ?] =
    gameRepo.ongoingByOneOfUserIdsCursor(userIds).documentSource().throttle(100, 1.second)
