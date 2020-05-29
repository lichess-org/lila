package draughts

import play.api.libs.openid.Errors.AUTH_CANCEL

import scala.collection.breakOut
import scalaz.Validation.FlatMap._
import scalaz.Validation.{ failureNel, success }
import variant.Variant

case class Board(
    pieces: PieceMap,
    history: DraughtsHistory,
    variant: Variant
) {

  import implicitFailures._

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(at: List[Pos]): Option[List[Piece]] = {
    val pieceList = at.flatMap(pieces.get)
    if (pieceList.isEmpty) None
    else Some(pieceList)
  }

  def apply(x: Int, y: Int): Option[Piece] = posAt(x, y) flatMap pieces.get
  def apply(field: Int): Option[Piece] = posAt(field) flatMap pieces.get

  def boardSize = variant.boardSize

  def posAt(x: Int, y: Int): Option[PosMotion] = variant.boardSize.pos.posAt(x, y)
  def posAt(field: Int): Option[PosMotion] = variant.boardSize.pos.posAt(field)
  def posAt(pos: Pos): PosMotion = variant.boardSize.pos.posAt(pos.fieldNumber).get

  lazy val actors: Map[Pos, Actor] = pieces map {
    case (pos, piece) => (pos, Actor(piece, posAt(pos), this))
  }

  lazy val actorsOf: Color.Map[Seq[Actor]] = {
    val (w, b) = actors.values.toSeq.partition {
      _.color.white
    }
    Color.Map(w, b)
  }

  lazy val ghosts = pieces.values.count(_.isGhost)

  def roleCount(r: Role): Int = pieces.values.count(_.role == r)

  def rolesOf(c: Color): List[Role] = pieces.values.collect {
    case piece if piece.color == c => piece.role
  }(breakOut)

  def actorAt(at: Pos): Option[Actor] = actors get at

  def piecesOf(c: Color): Map[Pos, Piece] = pieces filter (_._2 is c)

  lazy val kingPos: Map[Color, Pos] = pieces.collect {
    case (pos, Piece(color, King)) => color -> pos
  }(breakOut)

  def kingPosOf(c: Color): Option[Pos] = kingPos get c

  def seq(actions: (Board => Valid[Board])*): Valid[Board] =
    actions.foldLeft(success(this): Valid[Board])(_ flatMap _)

  def place(piece: Piece) = new {
    def at(at: Pos): Valid[Board] =
      if (pieces contains at) failureNel("Cannot place at occupied " + at)
      else success(copy(pieces = pieces + ((at, piece))))
  }

  def place(piece: Piece, at: Pos): Option[Board] =
    if (pieces contains at) None
    else Some(copy(pieces = pieces + ((at, piece))))

  def take(at: Pos): Option[Board] = pieces get at map { piece =>
    copy(pieces = pieces - at)
  }

  def withoutGhosts = copy(
    pieces = pieces.filterValues(!_.isGhost)
  )

  def move(orig: Pos, dest: Pos): Option[Board] =
    if (pieces contains dest) None
    else pieces get orig map { piece =>
      copy(pieces = pieces - orig + ((dest, piece)))
    }

  def taking(orig: Pos, dest: Pos, taking: Pos): Option[Board] = for {
    piece ← pieces get orig
    if (pieces contains taking)
    taken ← pieces get taking
  } yield copy(pieces = pieces - taking - orig + (dest -> piece) + (taking -> Piece(taken.color, taken.ghostRole)))

  def move(orig: Pos) = new {
    def to(dest: Pos): Valid[Board] = {
      if (pieces contains dest) failureNel("Cannot move to occupied " + dest)
      else pieces get orig map { piece =>
        copy(pieces = pieces - orig + (dest -> piece))
      } toSuccess ("No piece at " + orig + " to move")
    }
  }

  lazy val occupation: Color.Map[Set[Pos]] = Color.Map { color =>
    pieces.collect { case (pos, piece) if piece is color => pos }(breakOut)
  }

  def hasPiece(p: Piece) = pieces.values exists (p ==)

  def promote(pos: Pos): Option[Board] = for {
    piece ← apply(pos)
    if (piece is Man)
    b2 ← take(pos)
    b3 ← b2.place(piece.color.king, pos)
  } yield b3

  def withHistory(h: DraughtsHistory): Board = copy(history = h)

  def withPieces(newPieces: PieceMap) = copy(pieces = newPieces)

  def withVariant(v: Variant): Board = {
    copy(variant = v)
  }

  def updateHistory(f: DraughtsHistory => DraughtsHistory) = copy(history = f(history))

  def count(r: Role, c: Color): Int = pieces.values count (p => p.role == r && p.color == c)

  def count(p: Piece): Int = pieces.values count (_ == p)

  def count(c: Color): Int = pieces.values count (_.color == c)

  def autoDraw: Boolean = variant.maxDrawingMoves(this).fold(false)(m => history.halfMoveClock >= m)

  def situationOf(color: Color) = Situation(this, color)

  def valid(strict: Boolean) = variant.valid(this, strict)

  def materialImbalance: Int = pieces.values.foldLeft(0) {
    case (acc, Piece(color, role)) => Role.valueOf(role).fold(acc) { value =>
      acc + value * color.fold(1, -1)
    }
  }

  override def toString = s"$variant Position after ${history.lastMove}: $pieces"
}

object Board {

  def apply(pieces: Traversable[(Pos, Piece)], variant: Variant): Board = Board(pieces.toMap, DraughtsHistory(), variant)

  def init(variant: Variant): Board = Board(variant.pieces, variant)

  def empty(variant: Variant): Board = Board(Nil, variant)

  sealed abstract class BoardSize(
      val pos: BoardPos,
      val width: Int,
      val height: Int
  ) {
    val key = (width * height).toString
    val sizes = List(width, height)

    val fields = (width * height) / 2
    val promotableYWhite = 1
    val promotableYBlack = height
  }
  object BoardSize {
    val all: List[BoardSize] = List(D100, D64)
    val max = D100.pos
  }

  case object D100 extends BoardSize(
    pos = Pos100,
    width = 10,
    height = 10
  )
  case object D64 extends BoardSize(
    pos = Pos64,
    width = 8,
    height = 8
  )
}
