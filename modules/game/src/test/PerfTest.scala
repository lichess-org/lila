package lila.game

import org.specs2.mutable._
import org.specs2.specification._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

import lila.db.ByteArray

class PerfTest extends Specification {

  val nb = 200
  // val nb = 2
  val iterations = 10
  // val iterations = 1

  def runOne = Game.tube read getBson map (_.toChess)
  def run { for (i ← 1 to nb) runOne }

  "game model" should {
    "work" in {
      (Game.gameBSONHandler read getBson).turns must_== 12
    }
    "be fast" in {
      if (nb * iterations > 1) {
        print("warming up")
        run
        println(" again")
        run
      }
      println("measuring")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb games in $duration ms")
        duration
      }
      val nbGames = iterations * nb
      val gameMicros = (1000 * durations.sum) / nbGames
      println(s"Average = $gameMicros micros per game")
      println(s"          ${1000000 / gameMicros} games per second")
      true must_== true
    }
  }

  private def getBson = Game.gameBSONHandler write getGame

  private def getGame = {
    import chess.{ RunningClock, Board, Variant, Color, Pos, Status, Castles, Mode }
    import org.joda.time.DateTime
    Game(
      id = "s9kgwwwq",
      clock = RunningClock(
        color = Color.Black,
        limit = 420,
        increment = 0,
        whiteTime = 13.557001f,
        blackTime = 68.216995f,
        timer = 1371840910d).some,
      createdAt = DateTime.now,
      castleLastMoveTime = CastleLastMoveTime(
        Castles.all,
        lastMove = Some(Pos.A7 -> Pos.A5),
        lastMoveTime = Some(300),
        check = None),
      metadata = Metadata(Source.Lobby.some, None, None, None),
      whitePlayer = Player(
        color = Color.White,
        id = "4xqm",
        elo = 1151.some,
        blurs = 1,
        userId = "red1".some,
        aiLevel = None),
      blackPlayer = Player(
        color = Color.Black,
        id = "ufh0",
        elo = 968.some,
        userId = "japr".some,
        aiLevel = None),
      moveTimes = Vector(100, 20, 10, 60, 30, 30, 40, 100, 120, 10, 30),
      binaryPieces = BinaryFormat.piece write {
        Board.init(Variant.Standard).pieces -> Nil
      },
      binaryPgn = ByteArray.empty,
      mode = Mode(true),
      status = Status.Started,
      turns = 12,
      updatedAt = DateTime.now.some)
  }
}
