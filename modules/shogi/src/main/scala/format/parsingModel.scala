package shogi
package format

import cats.data.Validated
import cats.syntax.option._

case class ParsedNotation(
    initialPosition: InitialPosition,
    tags: Tags,
    parsedMoves: ParsedMoves
)

case class ParsedMoves(value: List[ParsedMove]) extends AnyVal

object ParsedMoves {
  val empty = ParsedMoves(Nil)
}

sealed trait ParsedMove {

  def apply(situation: Situation): Validated[String, MoveOrDrop]

  def getDest: Pos

  def metas: Metas

  def withMetas(m: Metas): ParsedMove

  def withSuffixes(s: Suffixes): ParsedMove = withMetas(metas withSuffixes s)

  def withComments(s: List[String]): ParsedMove = withMetas(metas withComments s)

  def withVariations(s: List[ParsedMoves]): ParsedMove = withMetas(metas withVariations s)

  def withTimeSpent(ts: Option[Centis]): ParsedMove = withMetas(metas withTimeSpent ts)

  def withTimeTotal(tt: Option[Centis]): ParsedMove = withMetas(metas withTimeTotal tt)

  def mergeGlyphs(glyphs: Glyphs): ParsedMove =
    withMetas(
      metas.withGlyphs(metas.glyphs merge glyphs)
    )
}

case class KifStd(
    dest: Pos,
    orig: Pos,
    role: Role,
    promotion: Boolean = false,
    metas: Metas = Metas.empty
) extends ParsedMove {

  def apply(situation: Situation) = move(situation) map Left.apply

  def withMetas(m: Metas) = copy(metas = m)

  def getDest = dest

  def move(situation: Situation): Validated[String, shogi.Move] =
    situation.board.actorAt(orig) flatMap { a =>
      a.trustedMoves find { m =>
        m.dest == dest && m.promotion == promotion && a.board.variant.kingSafety(a, m)
      }
    } match {
      case None       => Validated invalid s"No move found: $this\n$situation"
      case Some(move) => Validated valid move
    }

}

case class CsaStd(
    dest: Pos,
    orig: Pos,
    role: Role,
    metas: Metas = Metas.empty
) extends ParsedMove {

  def apply(situation: Situation) = move(situation) map Left.apply

  def withMetas(m: Metas) = copy(metas = m)

  def getDest = dest

  def move(situation: Situation): Validated[String, shogi.Move] =
    situation.board.actorAt(orig) flatMap { a =>
      a.trustedMoves find { m =>
        m.dest == dest && m.promotion == (role != m.piece.role) && a.board.variant.kingSafety(a, m)
      }
    } match {
      case None       => Validated invalid s"No move found: $this\n$situation"
      case Some(move) => Validated valid move
    }

}

case class PGNStd(
    dest: Pos,
    role: Role,
    capture: Boolean = false,
    file: Option[Int] = None,
    rank: Option[Int] = None,
    promotion: Boolean = false,
    metas: Metas = Metas.empty
) extends ParsedMove {

  def apply(situation: Situation) = move(situation) map Left.apply

  override def withSuffixes(s: Suffixes) =
    copy(
      metas = metas withSuffixes s,
      promotion = s.promotion
    )

  def withMetas(m: Metas) = copy(metas = m)

  def getDest = dest

  def move(situation: Situation): Validated[String, shogi.Move] =
    situation.board.pieces.foldLeft(none[shogi.Move]) {
      case (None, (pos, piece))
          if piece.color == situation.color && piece.role == role && compare(file, pos.x) && compare(
            rank,
            pos.y
          ) && piece.eyes(pos, dest) =>
        val a = Actor(piece, pos, situation.board)
        a.trustedMoves find { m =>
          m.dest == dest && m.promotion == promotion && a.board.variant.kingSafety(a, m)
        }
      case (m, _) => m
    } match {
      case None       => Validated invalid s"No move found: $this\n$situation"
      case Some(move) => Validated valid move
    }

  private def compare[A](a: Option[A], b: A) = a.fold(true)(b ==)
}

// All notations can share drop
case class Drop(
    role: Role,
    pos: Pos,
    metas: Metas = Metas.empty
) extends ParsedMove {

  def apply(situation: Situation) = drop(situation) map Right.apply

  def getDest = pos

  def withMetas(m: Metas) = copy(metas = m)

  def drop(situation: Situation): Validated[String, shogi.Drop] =
    situation.drop(role, pos)
}

case class InitialPosition(
    comments: List[String]
)

case class Metas(
    check: Boolean,
    checkmate: Boolean,
    comments: List[String],
    glyphs: Glyphs,
    variations: List[ParsedMoves],
    timeSpent: Option[Centis],
    timeTotal: Option[Centis]
) {

  def withSuffixes(s: Suffixes) =
    copy(
      glyphs = s.glyphs
    )

  def withGlyphs(g: Glyphs) = copy(glyphs = g)

  def withComments(c: List[String]) = copy(comments = c)

  def withVariations(v: List[ParsedMoves]) = copy(variations = v)

  def withTimeSpent(ts: Option[Centis]) = copy(timeSpent = ts)

  def withTimeTotal(tt: Option[Centis]) = copy(timeTotal = tt)
}

object Metas {
  val empty = Metas(false, false, Nil, Glyphs.empty, Nil, None, None)
}

case class Suffixes(
    promotion: Boolean,
    glyphs: Glyphs
)
