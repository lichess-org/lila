package lila.game

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi.{ StartGame, FinishGame }
import chess.format.FEN
import lila.user.User

final class GamesByUsersStream(system: ActorSystem) {

  import GamesByUsersStream._
  import lila.common.HttpStream._

  def apply(userIds: Set[User.ID]): Enumerator[String] = {

    def matches(game: Game) = game.userIds match {
      case List(u1, u2) if u1 != u2 => userIds(u1) && userIds(u2)
      case _ => false
    }
    var stream: Option[ActorRef] = None

    val enumerator = Concurrent.unicast[Game](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {
          def receive = {
            case StartGame(game) if matches(game) => channel push game
            case FinishGame(game, _, _) if matches(game) => channel push game
          }
        }))
        system.lilaBus.subscribe(actor, 'startGame, 'finishGame)
        stream = actor.some
      },
      onComplete = onComplete(stream, system)
    )

    enumerator &> withInitialFen &> toJson &> stringify
  }

  private val withInitialFen =
    Enumeratee.mapM[Game].apply[Game.WithInitialFen](GameRepo.withInitialFen)

  private val toJson =
    Enumeratee.map[Game.WithInitialFen].apply[JsObject](gameWithInitialFenWriter.writes)
}

object GamesByUsersStream {

  private implicit val fenWriter: Writes[FEN] = Writes[FEN] { f =>
    JsString(f.value)
  }

  private val gameWithInitialFenWriter: OWrites[Game.WithInitialFen] = OWrites {
    case Game.WithInitialFen(g, initialFen) =>
      Json.obj(
        "id" -> g.id,
        "initialFen" -> initialFen,
        "rated" -> g.rated,
        "variant" -> g.variant.key,
        "speed" -> g.speed.key,
        "perf" -> PerfPicker.key(g),
        "createdAt" -> g.createdAt,
        "status" -> g.status.id,
        "clock" -> g.clock.map { clock =>
          Json.obj(
            "initial" -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds,
            "totalTime" -> clock.estimateTotalSeconds
          )
        },
        "daysPerTurn" -> g.daysPerTurn,
        "players" -> JsObject(g.players.zipWithIndex map {
          case (p, i) => p.color.name -> Json.obj(
            "userId" -> p.userId,
            "name" -> p.name,
            "rating" -> p.rating
          ).add("provisional" -> p.provisional)
        })
      ).noNull
  }
}
