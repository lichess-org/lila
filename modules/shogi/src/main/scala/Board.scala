package shogi

import Pos.posAt
import scalaz.Validation.FlatMap._
import scalaz.Validation.{ failureNel, success }
import variant.{ Standard, Variant }

case class Board(
    pieces: PieceMap,
    history: History,
    variant: Variant,
    crazyData: Option[Hands] = None
) {

  import implicitFailures._

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(x: Int, y: Int): Option[Piece] = posAt(x, y) flatMap pieces.get

  lazy val actors: Map[Pos, Actor] = pieces map { case (pos, piece) =>
    (pos, Actor(piece, pos, this))
  }

  lazy val actorsOf: Color.Map[Seq[Actor]] = {
    val (s, g) = actors.values.toSeq.partition { _.color.sente }
    Color.Map(s, g)
  }

  def rolesOf(c: Color): List[Role] =
    pieces.values
      .collect {
        case piece if piece.color == c => piece.role
      }
      .to(List)

  def occupiedPawnFiles(c: Color): List[Int] =
    pieces
      .collect {
        case (pos, piece) if (piece.color == c) && (piece.role == Pawn) => pos.x
      }
      .to(List)

  def rolesInPromotionZoneOf(c: Color): List[Role] =
    pieces
      .collect {
        case (pos, piece) if (piece.color == c) && (c.promotableZone contains pos.y) => piece.role
      }
      .to(List)

  def actorAt(at: Pos): Option[Actor] = actors get at

  def piecesOf(c: Color): Map[Pos, Piece] = pieces filter (_._2 is c)

  lazy val kingPos: Map[Color, Pos] = pieces.collect { case (pos, Piece(color, King)) =>
    color -> pos
  }

  def kingPosOf(c: Color): Option[Pos] = kingPos get c

  def check(c: Color): Boolean = c.fold(checkSente, checkGote)

  lazy val checkSente = checkOf(Sente)
  lazy val checkGote  = checkOf(Gote)

  private def checkOf(c: Color): Boolean =
    kingPosOf(c) exists { kingPos =>
      variant.kingThreatened(this, !c, kingPos)
    }

  def destsFrom(from: Pos): Option[List[Pos]] = actorAt(from) map (_.destinations)

  def seq(actions: Board => Option[Board]*): Option[Board] =
    actions.foldLeft(Option(this): Option[Board])(_ flatMap _)

  def place(piece: Piece, at: Pos): Option[Board] =
    if (pieces contains at) None
    else Some(copy(pieces = pieces + ((at, piece))))

  def take(at: Pos): Option[Board] =
    if (pieces contains at) Some(copy(pieces = pieces - at))
    else None

  def move(orig: Pos, dest: Pos): Option[Board] =
    if (pieces contains dest) None
    else
      pieces get orig map { piece =>
        copy(pieces = pieces - orig + ((dest, piece)))
      }

  def taking(orig: Pos, dest: Pos, taking: Option[Pos] = None): Option[Board] =
    for {
      piece <- pieces get orig
      takenPos = taking getOrElse dest
      if pieces contains takenPos
    } yield copy(pieces = pieces - takenPos - orig + (dest -> piece))

  lazy val occupation: Color.Map[Set[Pos]] = Color.Map { color =>
    pieces.collect { case (pos, piece) if piece is color => pos }.to(Set)
  }

  def hasPiece(p: Piece) = pieces.values exists (p ==)

  def promote(pos: Pos): Option[Board] =
    for {
      piece        <- apply(pos)
      promotedRole <- Role.promotesTo(piece.role)
      b2           <- take(pos)
      b3           <- b2.place(Piece(piece.color, promotedRole), pos)
    } yield b3

  def withHistory(h: History): Board = copy(history = h)

  def withPieces(newPieces: PieceMap) = copy(pieces = newPieces)

  def withVariant(v: Variant): Board = {
    copy(variant = v).ensureCrazyData
  }

  def withCrazyData(data: Hands)         = copy(crazyData = Some(data))
  def withCrazyData(data: Option[Hands]) = copy(crazyData = data)
  def withCrazyData(f: Hands => Hands): Board =
    withCrazyData(f(crazyData | Hands.init))

  def ensureCrazyData = withCrazyData(crazyData | Hands.init)

  def updateHistory(f: History => History) = copy(history = f(history))

  def count(p: Piece): Int = pieces.values count (_ == p)
  def count(c: Color): Int = pieces.values count (_.color == c)

  def kingEntered(c: Color): Boolean =
    kingPosOf(c) exists (pos => c.promotableZone contains pos.y)

  def enoughImpasseValue(c: Color): Boolean = {
    val rp = rolesInPromotionZoneOf(c)
    val piecesValue = rp.foldLeft(0) { (acc, r) => acc + Role.impasseValueOf(r) } +
      crazyData.fold(0)(h => h.impasseValueOf(c))
    rp.size > 10 && piecesValue >= c.fold(28, 27)
  }

  def impasse(c: Color): Boolean =
    kingEntered(c) &&
      !c.fold(checkSente, checkGote) &&
      enoughImpasseValue(c)

  def tryRule(c: Color): Boolean =
    kingPosOf(c) == c.fold(posAt(5, 9), posAt(5, 1))

  def perpetualCheck: Boolean = {
    val checks = history.checkCount
    history.fourfoldRepetition && (checks.sente >= 4 || checks.gote >= 4)
  }

  def autoDraw: Boolean = {
    (history.fourfoldRepetition && !perpetualCheck) || variant.isInsufficientMaterial(this)
  }

  def situationOf(color: Color) = Situation(this, color)

  def visual = format.Visual >> this

  def valid(strict: Boolean) = variant.valid(this, strict)

  def materialImbalance: Int = variant.materialImbalance(this)

  override def toString = s"$variant Position after ${history.lastMove}\n$visual\n$crazyData"
}

object Board {

  def apply(pieces: Iterable[(Pos, Piece)], variant: Variant): Board =
    Board(pieces.toMap, History(), variant, Some(Hands.init))

  def init(variant: Variant): Board = Board(variant.pieces, variant)

  def empty(variant: Variant): Board = Board(Nil, variant)
}
