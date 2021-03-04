package lila.racer

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.puzzle.Puzzle
import lila.socket.Socket
import lila.storm.StormPuzzle
import lila.storm.StormSelector
import lila.user.User

final class RacerApi(colls: RacerColls, selector: StormSelector, cacheApi: CacheApi)(implicit
    ec: ExecutionContext
) {

  import RacerRace.Id
  import RacerBsonHandlers._

  def create(sessionId: String, user: Option[User]): Fu[RacerRace] =
    selector.apply flatMap { puzzles =>
      val race = RacerRace.make(
        owner = user match {
          case Some(u) => RacerPlayer.Id.User(u.id)
          case None    => RacerPlayer.Id.Anon(sessionId)
        },
        puzzleIds = puzzles.grouped(2).flatMap(_.headOption.map(_.id)).toList
      )
      colls.race.insert.one(race) inject race
    }

  def get(id: Id): Fu[Option[RacerRace]] =
    colls.race.byId[RacerRace](id.value)

  private object puzzles {

    def apply(race: RacerRace) = cache get race.puzzleIds

    private val cache = cacheApi[List[Puzzle.Id], List[StormPuzzle]](64, "racer.puzzles") {
      _.refreshAfterWrite(2 minutes)
        .buildAsyncFuture { puzzleIds =>
          colls.puzzle {
            _.find($inIds(puzzleIds.map(_.value)), projection.some)
              .cursor[StormPuzzle]()
              .list()
          }
        }
    }

    private val projection = $doc("fen" -> true, "line" -> true, "glicko.r" -> true)
  }

  def withPuzzles(id: Id): Fu[Option[RacerRace.WithPuzzles]] =
    get(id) flatMap {
      _ ?? { race =>
        puzzles(race) dmap {
          RacerRace.WithPuzzles(race, _)
        } dmap some
      }
    }
}
