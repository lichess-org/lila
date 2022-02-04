package shogi
package opening

import cats.syntax.option._

import shogi.format.forsyth.Sfen

object FullOpeningDB {

  private lazy val bySfen: collection.Map[String, FullOpening] =
    all
      .map { o =>
        o.sfen -> o
      }
      .toMap

  def findBySfen(sfen: Sfen): Option[FullOpening] =
    bySfen get sfen.truncate.value

  val SEARCH_MAX_PLIES = 40

  // assumes standard initial SFEN and variant
  def search(usis: Seq[shogi.format.usi.Usi]): Option[FullOpening.AtPly] =
    shogi.Replay
      .situations(
        usis.take(SEARCH_MAX_PLIES),
        None,
        variant.Standard
      )
      .toOption
      .flatMap {
        _.zipWithIndex.tail.reverse.foldLeft[Option[FullOpening.AtPly]](None) {
          case (None, (situation, ply)) =>
            val sfen = situation.toSfen.truncate
            bySfen get sfen.value map (_ atPly ply)
          case (found, _) => found
        }
      }

  def searchInSfens(sfens: Seq[Sfen]): Option[FullOpening] =
    sfens.foldRight(none[FullOpening]) {
      case (sfen, None) => findBySfen(sfen)
      case (_, found)  => found
    }

  // todo
  // format: off
  private def all: Vector[FullOpening] =
    Vector(
      new FullOpening("角換わり", "Bishop Exchange", "rn1qkbnr/ppp2ppp/8/3p4/5p2/6PB/PPPPP2P/RNBQK2R w KQkq -"),
      new FullOpening("3三角", "Bishop-33 opening", "rn1qkbnr/ppp2ppp/8/3p4/5p2/6PB/PPPPP2P/RNBQK2R w KQkq -"),
    )
}
