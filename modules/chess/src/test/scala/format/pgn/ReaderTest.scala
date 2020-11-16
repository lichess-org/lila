package chess
package format.pgn

class ReaderTest extends ChessTest {

  import Fixtures._
  import Reader.Result._

  "only raw moves" should {
    "many games" in {
      forall(raws) { (c: String) =>
        Reader.full(c) must beSuccess.like {
          case Complete(replay) => replay.moves must have size (c.split(' ').size)
        }
      }
    }
    "example from prod 1" in {
      Reader.full(fromProd1) must beSuccess
    }
    "example from prod 2" in {
      Reader.full(fromProd2) must beSuccess
    }
    "rook promotion" in {
      Reader.full(promoteRook) must beSuccess
    }
    "castle check O-O-O+" in {
      Reader.full(castleCheck1) must beSuccess
    }
    "castle checkmate O-O#" in {
      Reader.full(castleCheck2) must beSuccess
    }
    "and delimiters" in {
      Reader.full(withDelimiters) must beSuccess.like {
        case Complete(replay) => replay.moves must have size 33
      }
    }
    "and delimiters on new lines" in {
      Reader.full(withDelimitersOnNewLines) must beSuccess.like {
        case Complete(replay) => replay.moves must have size 33
      }
    }
  }
  "tags and moves" should {
    "chess960" in {
      Reader.full(complete960) must beSuccess
    }
    "with empty lines" in {
      Reader.full("\n" + complete960 + "\n") must beSuccess
    }
    "example from wikipedia" in {
      Reader.full(fromWikipedia) must beSuccess
    }
    "with inline comments" in {
      Reader.full(inlineComments) must beSuccess
    }
    "example from chessgames.com" in {
      Reader.full(fromChessgames) must beSuccess
    }
    "example from chessgames.com with escape chars" in {
      Reader.full(fromChessgamesWithEscapeChar) must beSuccess
    }
    "immortal with NAG" in {
      Reader.full(withNag) must beSuccess
    }
    "example from TCEC" in {
      Reader.full(fromTcec) must beSuccess
    }
    "from https://chessprogramming.wikispaces.com/Kasparov+versus+Deep+Blue+1996" in {
      Reader.full(fromChessProgrammingWiki) must beSuccess
    }
    "comments and variations" in {
      Reader.full(commentsAndVariations) must beSuccess
    }
    "comments and variations by smartchess" in {
      Reader.full(bySmartChess) must beSuccess
    }
    "invalid variant" in {
      Reader.full(invalidVariant) must beSuccess.like {
        case Complete(replay) => replay.setup.board.variant must_== variant.Standard
      }
    }
    "promoting to a rook" in {
      Reader.full(fromLichessBadPromotion) must beSuccess.like {
        case Complete(replay) =>
          replay.chronoMoves lift 10 must beSome.like {
            case move => move.fold(_.promotion, _ => None) must_== Some(Rook)
          }
      }
    }
    "chessbase arrows" in {
      Reader.full(chessbaseArrows) must beSuccess
    }
    "atomic regression" in {
      Reader.full(atomicRegression) must beSuccess
    }
    "atomic promotion" in {
      Reader.full(atomicPromotion) must beSuccess
    }
    "lichobile export" in {
      Reader.full(lichobile) must beSuccess
    }
    "crazyhouse 1" in {
      Reader.full(crazyhouse1) must beSuccess.like {
        case Complete(replay) =>
          replay.chronoMoves lift 11 must beSome.like {
            case move => move.fold(_.toUci.uci, _.toUci.uci) must_== "P@c6"
          }
      }
    }
    "crazyhouse 2" in {
      Reader.full(crazyhouse2) must beSuccess.like {
        case Complete(replay) => replay.chronoMoves.size must_== 111
      }
    }
    "crazyhouse without variant tag" in {
      Reader.full(crazyhouseNoVariantTag) must beSuccess.like {
        case Incomplete(replay, _) => replay.chronoMoves.size must_== 8
      }
    }
    "crazyhouse from chess.com" in {
      Reader.full(chessComCrazyhouse) must beSuccess
    }
  }
  "from prod" in {
    "from position close chess" in {
      Reader.full(fromPosProdCloseChess) must beSuccess.like {
        case Complete(replay) => replay.chronoMoves.size must_== 152
      }
    }
    "from position empty FEN" in {
      Reader.full(fromPositionEmptyFen) must beSuccess.like {
        case Complete(replay) => replay.chronoMoves.size must_== 164
      }
    }
    "preserves initial ply" in {
      Reader.full(caissa) must beSuccess.like {
        case Complete(replay) =>
          replay.setup.startedAtTurn must_== 43
          replay.state.startedAtTurn must_== 43
      }
    }
  }
  "partial from broadcast" in {
    Reader.full(festivalFigueira) must beSuccess.like {
      case Incomplete(replay, _) => replay.chronoMoves.size must_== 113
    }
  }
  "invisible char" in {
    Reader.full(invisibleChar) must beSuccess.like {
      case Complete(replay) => replay.chronoMoves.size must_== 19
    }
  }
}
