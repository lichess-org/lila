package lila.benchmark

import annotation.tailrec
import com.google.caliper.Param
import ornicar.scalalib.OrnicarValidation

import lila.chess.{ Game, Pos }
import lila.chess.Pos._

// a caliper benchmark is a class that extends com.google.caliper.Benchmark
// the SimpleScalaBenchmark trait does it and also adds some convenience functionality
class Benchmark extends SimpleScalaBenchmark with OrnicarValidation {

  @Param(Array("100"))
  val length: Int = 0

  def timeImmortal(reps: Int) = repeat(reps) {
    val s = playMoves(E2 -> E4, D7 -> D5, E4 -> D5, D8 -> D5, B1 -> C3, D5 -> A5, D2 -> D4, C7 -> C6, G1 -> F3, C8 -> G4, C1 -> F4, E7 -> E6, H2 -> H3, G4 -> F3, D1 -> F3, F8 -> B4, F1 -> E2, B8 -> D7, A2 -> A3, E8 -> C8, A3 -> B4, A5 -> A1, E1 -> D2, A1 -> H1, F3 -> C6, B7 -> C6, E2 -> A6)
    if (s.isFailure) throw new Exception("success")
    "haha"
  }

  //def timeDeepBlue(reps: Int) = repeat(reps) {
    //playMoves(E2 -> E4, C7 -> C5, C2 -> C3, D7 -> D5, E4 -> D5, D8 -> D5, D2 -> D4, G8 -> F6, G1 -> F3, C8 -> G4, F1 -> E2, E7 -> E6, H2 -> H3, G4 -> H5, E1 -> G1, B8 -> C6, C1 -> E3, C5 -> D4, C3 -> D4, F8 -> B4)
  //}

  def playMoves(moves: (Pos, Pos)*): Valid[Game] = {
    val game = moves.foldLeft(success(Game()): Valid[Game]) {
      (vg, move) ⇒ {
        vg flatMap { g ⇒
          // will be called to pass as playable squares to the client
          g.situation.destinations
          g.playMove(move._1, move._2)
        }
      }
    }
    if (game.isFailure) throw new RuntimeException("Failure!")
    game
  }

}
