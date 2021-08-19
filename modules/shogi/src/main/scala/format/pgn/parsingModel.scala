package shogi
package format.pgn

import scalaz.Validation.FlatMap._

case class ParsedPgn(
    initialPosition: InitialPosition,
    tags: Tags,
    sans: Sans
)

case class Sans(value: List[San]) extends AnyVal

object Sans {
  val empty = Sans(Nil)
}

// Standard Algebraic Notation
sealed trait San {

  def apply(situation: Situation): Valid[MoveOrDrop]

  def getDest: Pos

  def metas: Metas

  def withMetas(m: Metas): San

  def withSuffixes(s: Suffixes): San = withMetas(metas withSuffixes s)

  def withComments(s: List[String]): San = withMetas(metas withComments s)

  def withVariations(s: List[Sans]): San = withMetas(metas withVariations s)

  def withTimeSpent(ts: Option[Centis]): San = withMetas(metas withTimeSpent ts)

  def withTimeTotal(tt: Option[Centis]): San = withMetas(metas withTimeTotal tt)

  def mergeGlyphs(glyphs: Glyphs): San =
    withMetas(
      metas.withGlyphs(metas.glyphs merge glyphs)
    )
}

case class Std(
    dest: Pos,
    role: Role,
    capture: Boolean = false,
    file: Option[Int] = None,
    rank: Option[Int] = None,
    promotion: Boolean = false,
    metas: Metas = Metas.empty
) extends San {

  def apply(situation: Situation) = move(situation) map Left.apply

  override def withSuffixes(s: Suffixes) =
    copy(
      metas = metas withSuffixes s,
      promotion = s.promotion
    )

  def withMetas(m: Metas) = copy(metas = m)

  def getDest = dest

  def move(situation: Situation): Valid[shogi.Move] =
    situation.board.pieces.foldLeft(none[shogi.Move]) {
      case (None, (pos, piece))
          if piece.color == situation.color && piece.role == role && compare(file, pos.x) && compare(
            rank,
            pos.y
          ) && piece.eyesMovable(pos, dest) =>
        val a = Actor(piece, pos, situation.board)
        a.trustedMoves() find { m =>
          m.dest == dest && a.board.variant.kingSafety(a, m)
        }
      case (m, _) => m
    } match {
      case None => {
        s"No move found: $this\n$situation".failureNel
      }
      case Some(move) => move withPromotion (Role.promotesTo(role), promotion) toValid "Wrong promotion"
    }

  private def compare[A](a: Option[A], b: A) = a.fold(true)(b ==)
}

case class Drop(
    role: Role,
    pos: Pos,
    metas: Metas = Metas.empty
) extends San {

  def apply(situation: Situation) = drop(situation) map Right.apply

  def getDest = pos

  def withMetas(m: Metas) = copy(metas = m)

  def drop(situation: Situation): Valid[shogi.Drop] =
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
    variations: List[Sans],
    timeSpent: Option[Centis],
    timeTotal: Option[Centis]
) {

  def withSuffixes(s: Suffixes) =
    copy(
      glyphs = s.glyphs
    )

  def withGlyphs(g: Glyphs) = copy(glyphs = g)

  def withComments(c: List[String]) = copy(comments = c)

  def withVariations(v: List[Sans]) = copy(variations = v)

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
