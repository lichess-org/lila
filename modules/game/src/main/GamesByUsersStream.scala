package lila.game

import actorApi.{ FinishGame, StartGame }
import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.common.Json.given
import lila.db.dsl.given

final class GamesByUsersStream(gameRepo: lila.game.GameRepo)(using
    mat: akka.stream.Materializer,
    ec: Executor
):

  private val chans = List("startGame", "finishGame")

  def apply(userIds: Set[UserId], withCurrentGames: Boolean): Source[JsValue, ?] =
    val initialGames = if (withCurrentGames) currentGamesSource(userIds) else Source.empty
    val startStream = Source.queue[Game](150, akka.stream.OverflowStrategy.dropHead) mapMaterializedValue {
      queue =>
        def matches(game: Game) =
          game.userIds match
            case List(u1, u2) if u1 != u2 => userIds(u1) && userIds(u2)
            case _                        => false
        val sub = Bus.subscribeFun(chans*) {
          case StartGame(game) if matches(game)        => queue.offer(game).unit
          case FinishGame(game, _, _) if matches(game) => queue.offer(game).unit
        }
        queue.watchCompletion().addEffectAnyway {
          Bus.unsubscribe(sub, chans)
        }
    }
    initialGames
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map(GameStream.gameWithInitialFenWriter.writes)

  private def currentGamesSource(userIds: Set[UserId]): Source[Game, ?] =
    import lila.db.dsl.*
    import BSONHandlers.given
    import reactivemongo.api.ReadPreference
    import reactivemongo.akkastream.cursorProducer
    gameRepo.coll
      .aggregateWith[Game](
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework.*
        List(
          Match($doc(Game.BSONFields.playingUids $in userIds)),
          AddFields(
            $doc(
              "both" -> $doc("$setIsSubset" -> $arr("$" + Game.BSONFields.playingUids, userIds))
            )
          ),
          Match($doc("both" -> true))
        )
      }
      .documentSource()
      .throttle(30, 1.second)

private object GameStream:

  val gameWithInitialFenWriter: OWrites[Game.WithInitialFen] = OWrites {
    case Game.WithInitialFen(g, initialFen) =>
      Json
        .obj(
          "id"         -> g.id,
          "rated"      -> g.rated,
          "variant"    -> g.variant.key,
          "speed"      -> g.speed.key,
          "perf"       -> PerfPicker.key(g),
          "createdAt"  -> g.createdAt,
          "status"     -> g.status.id,
          "statusName" -> g.status.name,
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
            "increment" -> clock.incrementSeconds
          )
        })
        .add("daysPerTurn" -> g.daysPerTurn)
  }
