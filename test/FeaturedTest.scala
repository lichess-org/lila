package lila.app
package game

import lila.game._
import lila.user._

class FeaturedTest extends LilaSpec {

  import Featured._

  "Featured" should {

    "box 0 to 1" in {
      foreach(List(
        0f -> 0f,
        1f -> 1f,
        0.5f -> 0.5f,
        0.9f -> 0.9f,
        -1f -> 0f,
        2f -> 1f)) {
        case (a, b) ⇒ box(0 to 1)(a) must_== b
      }
    }

    "box 1200 to 2000" in {
      foreach(List(
        1200f -> 0f,
        2000f -> 1f,
        1600f -> 0.5f,
        1900f -> 0.875f,
        -1f -> 0f,
        800f -> 0f,
        2200f -> 1f)) {
        case (a, b) ⇒ box(1200 to 2000)(a) must_== b
      }
    }

    val game1 = Game.make(
      game = chess.Game(chess.Variant.default),
      whitePlayer = Player.white.copy(elo = 1600.some),
      blackPlayer = Player.black,
      ai = None,
      creatorColor = chess.Color.White,
      mode = chess.Mode.default,
      variant = chess.Variant.default,
      source = Source.Lobby,
      pgnImport = None)

    val game2 = game1.copy(
      clock = chess.Clock(180,0).some,
      turns = 11)

    val game3 = game1.copy(
      clock = chess.Clock(60,0).some,
      turns = 21)

    val games = List(game1, game2, game3)

    "elo" in {
      "game1 white" in {
        eloHeuristic(chess.Color.White)(game1) must_== 0.6f
      }
      "game1 black" in {
        eloHeuristic(chess.Color.Black)(game1) must_== 0f
      }
    }
    "speed" in {
      "game1" in {
        speedHeuristic(game1) must_== 0
      }
      "game2" in {
        speedHeuristic(game2) must_== 0.5f
      }
      "game3" in {
        speedHeuristic(game3) must_== 1f
      }
    }
    "progress" in {
      "game1" in {
        progressHeuristic(game1) must_== 1f
      }
      "game2" in {
        progressHeuristic(game2) must_== 0.5f
      }
      "game3" in {
        progressHeuristic(game3) must_== 0f
      }
    }
    "score" in {
      "game1" in {
        score(game1) must_== 0.6f + 0f + 1f * 0.5f
      }
      "game2" in {
        score(game2) must_== 0.6f + 0.5f + 0.5f * 0.5f
      }
      "game3" in {
        score(game3) must_== 0.6f + 1f + 0f * 0.5f
      }
    }
    "best" in {
      "3 games" in {
        best(games) must_== game3.some
      }
      "3 games reversed" in {
        best(games.reverse) must_== game3.some
      }
    }
  }
}
