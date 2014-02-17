package lila.game

import scala.concurrent.duration._

import chess.{ AllPieces }
import org.specs2.mutable._
import org.specs2.specification._
import reactivemongo.api.collections.default.BSONCollection

import lila.db.WithColl

class BinaryPieceDbTest extends Specification with WithColl {

  val id = "00000000"

  private def saveAndGet(repo: GameRepo, game: Game): AllPieces =
    (repo.insertDenormalized(game) >> repo.game(game.id))
      .flatten(s"Game ${game.id} not found")
      .map(_.toChess.allPieces)
      .await(Duration(1, SECONDS))

  private def makeRepo(c: BSONCollection) =
    new GameRepo { def gameTube = Game.tube inColl c }

  "binary pieces" should {
    "standard initial" in {
      val g = Game.make(
        game = chess.Game(chess.Variant.Standard),
        whitePlayer = Player.white,
        blackPlayer = Player.black,
        mode = chess.Mode.default,
        variant = chess.Variant.default,
        source = Source.Lobby,
        pgnImport = None)
      withColl { c =>
        saveAndGet(makeRepo(c), g) must_== g.toChess.allPieces
      }
    }
    // "support real game" in {
    //   val moveString = "d4 e6 e3 d6 a3 Nf6 Nf3 Be7 Nc3 O-O h3 Nfd7 g3 d5 Bg2 c5 O-O cxd4 Qxd4 b6 Qd1 Ba6 Re1 Nc6 Bf1 f6 Bxa6 Nde5 Nxe5 Nxe5 b3 Rc8 Bb2 Rc6 Bb5 Rc5 b4 Rc7 f4 Nc4 Bxc4 Rxc4 h4 a6 b5 axb5 Nxb5 Rc5 a4 e5 Ba3 Rc4 Bxe7 Qxe7 fxe5 fxe5 Rf1 d4 Rxf8+ Qxf8 exd4 Qf3 Qf1 Qe3+ Qf2 Qxf2+ Kxf2 exd4 Rc1 d3 cxd3 Rxc1 Ke3 Ra1 Nc3 Ra3 Kd4 g6 Kc4 Kf7 d4 Ke6 d5+ Kd6 Kb5 Rb3+ Ka6 Rxc3 Kxb6 Kxd5 a5 Ra3 a6 Kd6 a7 Kd7 Kb7 Rb3+ Ka6 Ra3+ Kb7 Rb3+ Ka8 Kc7 g4 Rb4 h5 gxh5 gxh5 Ra4 h6 Rxa7+ Kxa7 Kd6 Kb7 Ke6 Kc6 Kf6 Kd5 Kg6 Ke5 Kxh6 Kf4 Kg6 Kg4 h6 Kh4 h5 Kg3 Kg5 Kh3 h4 Kg2 Kg4 Kh2 h3 Kg1 Kg3 Kh1 Kh4 Kh2 Kg4 Kh1 Kg3 Kg1 Kf3 Kh1 Kg4 Kh2 Kh4 Kg1 Kg3 Kh1 h2"
    //   val moves = moveString.split(' ').toList
    //   withColl { c =>
    //     val repo = new PgnRepo { def coll = c }
    //     saveAndGet(repo, moves) must_== moves
    //   }
    // }
  }
}
