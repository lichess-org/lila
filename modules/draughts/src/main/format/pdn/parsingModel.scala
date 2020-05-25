package draughts
package format.pdn

import scalaz.Validation.FlatMap._

case class ParsedPdn(
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

  def apply(situation: Situation, iteratedCapts: Boolean = false, forbiddenUci: Option[List[String]] = None): Valid[draughts.Move]

  def metas: Metas

  def withMetas(m: Metas): San

  def withSuffixes(s: Suffixes): San = withMetas(metas withSuffixes s)

  def withComments(s: List[String]): San = withMetas(metas withComments s)

  def withVariations(s: List[Sans]): San = withMetas(metas withVariations s)

  def mergeGlyphs(glyphs: Glyphs): San = withMetas(
    metas.withGlyphs(metas.glyphs merge glyphs)
  )
}

case class Std(
    src: Pos,
    dest: Pos,
    capture: Boolean = false,
    metas: Metas = Metas.empty
) extends San {

  def apply(situation: Situation, iteratedCapts: Boolean = false, forbiddenUci: Option[List[String]] = None) = move(situation, iteratedCapts, forbiddenUci)

  override def withSuffixes(s: Suffixes) = copy(
    metas = metas withSuffixes s
  )

  def withMetas(m: Metas) = copy(metas = m)

  def move(situation: Situation, iteratedCapts: Boolean = false, forbiddenUci: Option[List[String]] = None, captures: Option[List[Pos]] = None): Valid[draughts.Move] =
    situation.board.pieces.foldLeft(none[draughts.Move]) {
      case (None, (pos, piece)) if piece.color == situation.color && pos == src =>
        val a = Actor(piece, situation.board.posAt(pos), situation.board)
        val m = a.validMoves.find { m => m.dest == dest && (!iteratedCapts || m.situationAfter.ghosts == 0) }
        if (m.isEmpty && capture && iteratedCapts)
          a.capturesFinal.find { m => m.dest == dest && captures.fold(true)(m.capture.contains) && !forbiddenUci.fold(false)(_.contains(m.toUci.uci)) }
        else m
      case (m, _) => m
    } match {
      case None => s"No move found ($iteratedCapts): $this - in situation $situation".failureNel
      case Some(move) => Some(move) toValid s"Invalide move: $move"
    }

  private def compare[A](a: Option[A], b: A) = a.fold(true)(b ==)

}

case class InitialPosition(
    comments: List[String]
)

case class Metas(
    checkmate: Boolean,
    comments: List[String],
    glyphs: Glyphs,
    variations: List[Sans]
) {

  def withSuffixes(s: Suffixes) = copy(
    checkmate = s.checkmate,
    glyphs = s.glyphs
  )

  def withGlyphs(g: Glyphs) = copy(glyphs = g)

  def withComments(c: List[String]) = copy(comments = c)

  def withVariations(v: List[Sans]) = copy(variations = v)
}

object Metas {
  val empty = Metas(false, Nil, Glyphs.empty, Nil)
}

case class Suffixes(
    checkmate: Boolean,
    glyphs: Glyphs
)
