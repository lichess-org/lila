package lila.game

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi.{ StartGame, FinishGame }
import chess.format.FEN
import lila.user.User

final class GamesByUsersStream(system: ActorSystem) {

  import GamesByUsersStream._

  def apply(userIds: Set[User.ID]): Enumerator[JsObject] = {

    def matches(game: Game) = game.userIds match {
      case List(u1, u2) if u1 != u2 => userIds(u1) && userIds(u2)
      case _ => false
    }
    var subscriber: Option[lila.common.Tellable] = None

    val enumerator = Concurrent.unicast[Game](
      onStart = channel => {
        subscriber = system.lilaBus.subscribeFun(classifiers: _*) {
          case StartGame(game) if matches(game) => channel push game
          case FinishGame(game, _, _) if matches(game) => channel push game
        } some
      },
      onComplete = subscriber foreach { system.lilaBus.unsubscribe(_, classifiers) }
    )

    enumerator &> withInitialFen &> toJson
  }

  private val withInitialFen =
    Enumeratee.mapM[Game].apply[Game.WithInitialFen](GameRepo.withInitialFen)

  private val toJson =
    Enumeratee.map[Game.WithInitialFen].apply[JsObject](gameWithInitialFenWriter.writes)
}

private object GamesByUsersStream {

  private val classifiers = List('startGame, 'finishGame)

  private implicit val fenWriter: Writes[FEN] = Writes[FEN] { f =>
    JsString(f.value)
  }

  private val gameWithInitialFenWriter: OWrites[Game.WithInitialFen] = OWrites {
    case Game.WithInitialFen(g, initialFen) =>
      Json.obj(
        "id" -> g.id,
        "rated" -> g.rated,
        "variant" -> g.variant.key,
        "speed" -> g.speed.key,
        "perf" -> PerfPicker.key(g),
        "createdAt" -> g.createdAt,
        "status" -> g.status.id,
        "players" -> JsObject(g.players.zipWithIndex map {
          case (p, i) => p.color.name -> Json.obj(
            "userId" -> p.userId,
            "rating" -> p.rating
          ).add("provisional" -> p.provisional)
            .add("name" -> p.name)
        })
      ).add("initialFen" -> initialFen)
        .add("clock" -> g.clock.map { clock =>
          Json.obj(
            "initial" -> clock.limitSeconds,
            "increment" -> clock.incrementSeconds,
            "totalTime" -> clock.estimateTotalSeconds
          )
        })
        .add("daysPerTurn" -> g.daysPerTurn)
  }
}
