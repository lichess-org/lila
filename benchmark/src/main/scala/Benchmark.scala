package lila.benchmark

import annotation.tailrec
import com.google.caliper.Param
import ornicar.scalalib.OrnicarValidation

import lila.system._
import lila.chess.{ Game, Pos, Color, White, Black, PromotableRole, Queen }
import lila.chess.Pos._

// a caliper benchmark is a class that extends com.google.caliper.Benchmark
// the SimpleScalaBenchmark trait does it and also adds some convenience functionality
class Benchmark extends SimpleScalaBenchmark {

  @Param(Array("100"))
  val length: Int = 0

  def playMove(
    game: Game,
    orig: Pos,
    dest: Pos,
    promotion: PromotableRole = Queen): Valid[Game] =
    game.apply(orig, dest, promotion) map (_._1)

  def timeChessImmortal(reps: Int) = {

    def playMoves(moves: (Pos, Pos)*): Valid[Game] = {
      val game = moves.foldLeft(success(Game()): Valid[Game]) {
        (vg, move) ⇒
          vg flatMap { g ⇒
            // will be called to pass as playable squares to the client
            g.situation.destinations
            playMove(g, move._1, move._2)
          }
      }
      if (game.isFailure) throw new RuntimeException("Failure!")
      game
    }
    repeat(reps) {
      playMoves(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)
    }
  }

  def timeSystemImmortal(reps: Int) = {

    import model._
    import scala.util.Random

    val env = SystemEnv()
    val repo = env.gameRepo
    val server = env.server

    def move(game: DbGame, m: String = "d2 d4") = server.playMove(game fullIdOf White, m).unsafePerformIO

    val moves = List("e2 e4", "d7 d5", "e4 d5", "d8 d5", "b1 c3", "d5 a5", "d2 d4", "c7 c6", "g1 f3", "c8 g4", "c1 f4", "e7 e6", "h2 h3", "g4 f3", "d1 f3", "f8 b4", "f1 e2", "b8 d7", "a2 a3", "e8 c8", "a3 b4", "a5 a1", "e1 d2", "a1 h1", "f3 c6", "b7 c6", "e2 a6")

    def play(game: DbGame) = for (m ← moves) yield move(game, m)

    def randomString(len: Int) = List.fill(len)(randomChar) mkString
    def randomChar = (Random.nextInt(25) + 97).toChar

    def newDbPlayer(color: Color, ps: String) = DbPlayer(
      id = color.name take 4,
      color = color,
      ps = ps,
      aiLevel = None,
      isWinner = None,
      evts = "0s|1Msystem White creates the game|2Msystem Black joins the game",
      elo = Some(1280)
    )
    val white = newDbPlayer(White, "ip ar jp bn kp cb lp dq mp ek np fb op gn pp hr")
    val black = newDbPlayer(Black, "Wp 4r Xp 5n Yp 6b Zp 7q 0p 8k 1p 9b 2p !n 3p ?r")

    repeat(reps) {
      val game = DbGame(
        id = randomString(8),
        whitePlayer = white.copy(id = randomString(4)),
        blackPlayer = black.copy(id = randomString(4)),
        pgn = "",
        status = Created,
        turns = 0,
        clock = None,
        lastMove = None
      )
      repo insert game
      val result = sequenceValid(play(game))
      if (result.isFailure) throw new RuntimeException("Failure!")
      result
    }
  }
}
