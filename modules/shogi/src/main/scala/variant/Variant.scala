package shogi
package variant

import cats.data.Validated
import cats.syntax.option._
import scala.annotation.nowarn

import format.usi.Usi
import Pos._

// Correctness depends on singletons for each variant ID
abstract class Variant private[variant] (
    val id: Int,
    val key: String,
    val name: String,
    val shortName: String,
    val title: String,
    val standardInitialPosition: Boolean
) {

  def standard     = this == Standard
  def minishogi    = this == Minishogi
  def fromPosition = this == FromPosition

  def exotic        = !standard
  def standardBased = standard || fromPosition

  def initialFen = format.Forsyth.initial

  def pieces: Map[Pos, Piece]
  def hand: Map[Role, Int]

  def allSquares: List[Pos]

  def numberOfRanks: Int
  def numberOfFiles: Int

  // list of ranks where pieces of color can promote
  def promotionRanks(color: Color): List[Int]

  // furthest rank for color
  def backrank(color: Color) = if (color == Sente) 1 else numberOfRanks

  // true if piece will never be able to move from pos
  def pieceInDeadZone(piece: Piece, pos: Pos): Boolean =
    piece.role match {
      case Pawn | Knight | Lance if backrank(piece.color) == pos.y => true
      case Knight if Math.abs(backrank(piece.color) - pos.y) == 1  => true
      case _                                                       => false
    }

  def canPromote(move: Move): Boolean =
    promotableRoles.contains(move.piece.role) &&
      promotionRanks(move.color).exists(sq => sq == move.dest.y || sq == move.orig.y)

  // Whether after capture the piece goes to players hand
  def hasHandData = true

  // Some variants could have an extra effect on the board on a move
  def hasMoveEffects = false

  // Applies a variant specific effect to the move.
  // This helps decide whether a king is endangered by a move, for example
  def addVariantEffect(move: Move): Move = move

  // Optimized for performance
  def pieceThreatened(board: Board, color: Color, to: Pos, filter: Piece => Boolean = _ => true): Boolean =
    board.pieces exists {
      case (pos, piece) if piece.color == color && filter(piece) && piece.eyes(pos, to) =>
        !piece.longRangeDirs.nonEmpty || (pos touches to) || piece.role.dir(pos, to).exists {
          longRangeThreatens(board, pos, _, to)
        }
      case _ => false
    }

  def kingThreatened(board: Board, color: Color, to: Pos, filter: Piece => Boolean = _ => true) =
    pieceThreatened(board, color, to, filter)

  def kingSafety(m: Move, filter: Piece => Boolean, kingPos: Option[Pos]): Boolean =
    ! {
      kingPos exists { kingThreatened(m.after, !m.color, _, filter) }
    }

  def kingSafety(a: Actor, m: Move): Boolean =
    kingSafety(
      m,
      if ((a.piece is King) || a.check) (_ => true) else (_.longRangeDirs.nonEmpty),
      if (a.piece.role == King) None else a.board kingPosOf a.color
    )

  def longRangeThreatens(board: Board, p: Pos, dir: Direction, to: Pos): Boolean =
    dir(p) exists { next =>
      next == to || (!board.pieces.contains(next) && longRangeThreatens(board, next, dir, to))
    }

  def move(situation: Situation, from: Pos, to: Pos, promotion: Boolean): Validated[String, Move] = {

    def findMove(from: Pos, to: Pos) =
      situation.board.actorAt(from) flatMap (_.moves.find(m => m.dest == to && m.promotion == promotion))

    for {
      actor <- situation.board.actors get from toValid s"No piece on $from"
      _ <-
        if (actor is situation.color) Validated.valid(actor)
        else Validated.invalid(s"Not my piece on $from")
      move1 <- findMove(from, to) toValid s"Piece on $from cannot move to $to"
    } yield move1
  }

  def isValidPieceDrop(piece: Piece, pos: Pos, sit: Situation) =
    !pieceInDeadZone(piece, pos) &&
      (
        piece.role != Pawn ||
          !(
            sit.board.pieces.exists { case (pPos, p) =>
              (p is Pawn) && (p is sit.color) && (pPos.x == pos.x)
            }
          )
      )

  def drop(sit: Situation, role: Role, pos: Pos): Validated[String, Drop] =
    for {
      d1 <- sit.board.handData toValid "Board has no hand data"
      piece = Piece(sit.color, role)
      _ <-
        if (isValidPieceDrop(piece, pos, sit)) Validated.valid(d1)
        else Validated.invalid(s"Can't drop $role on $pos")
      d2     <- d1.drop(piece) toValid s"No $piece to drop on $pos"
      board1 <- sit.board.place(piece, pos) toValid s"Can't drop $role on $pos, it's occupied"
      _ <-
        if (!board1.check(sit.color)) Validated.valid(board1)
        else Validated.invalid(s"Dropping $role on $pos doesn't uncheck the king")
    } yield Drop(
      piece = piece,
      pos = pos,
      situationBefore = sit,
      after = board1 withHandData d2
    )

  private def canDropStuff(sit: Situation) =
    sit.board.handData.fold(false) { (hands: Hands) =>
      val hand = hands(sit.color)
      hand.size > 0 && possibleDrops(sit).fold(true) { squares =>
        squares.nonEmpty && {
          squares.exists(s =>
            hand.handMap.exists(kv => kv._2 > 0 && isValidPieceDrop(Piece(sit.color, kv._1), s, sit))
          )
        }
      }
    }

  // None means all, since we don't want to send all empty squares every round for every role
  def possibleDrops(situation: Situation): Option[List[Pos]] =
    if (!situation.check) None
    else situation.kingPos.map { blockades(situation, _) }

  private def blockades(situation: Situation, kingPos: Pos): List[Pos] = {
    def attacker(piece: Piece) = piece.longRangeDirs.nonEmpty && piece.color != situation.color
    @scala.annotation.tailrec
    def forward(p: Pos, dir: Direction, squares: List[Pos]): List[Pos] =
      dir(p) match {
        case None                                                 => Nil
        case Some(next) if situation.board(next).exists(attacker) => next :: squares
        case Some(next) if situation.board(next).isDefined        => Nil
        case Some(next)                                           => forward(next, dir, next :: squares)
      }
    Pos.allDirections flatMap { forward(kingPos, _, Nil) } filter { square =>
      situation.board.place(Piece(situation.color, Knight), square) exists { defended =>
        !defended.check(situation.color)
      }
    }
  }

  private def impasseEligible(sit: Situation): Boolean = {
    val c = sit.color
    val valuesOfRoles = sit.board.pieces.collect {
      case (pos, piece) if (piece is c) && (sit.board.variant.promotionRanks(c) contains pos.y) =>
        Role.impasseValueOf(piece.role)
    }.toList
    val impasseValue = valuesOfRoles.sum + sit.board.handData.fold(0)(h => h.impasseValueOf(c))
    valuesOfRoles.size > 10 && impasseValue >= c.fold(28, 27)
  }

  def impasse(sit: Situation): Boolean =
    sit.board.kingEntered(sit.color) &&
      !sit.board.check(sit.color) &&
      impasseEligible(sit)

  def staleMate(sit: Situation): Boolean =
    !sit.check && sit.moves.isEmpty && !canDropStuff(sit)

  def checkmate(sit: Situation): Boolean =
    sit.check && sit.moves.isEmpty && !canDropStuff(sit)

  // Player wins or loses after their move
  def winner(sit: Situation): Option[Color] = {
    val pawnDrop = sit.board.history.lastMove.fold(false) { l => l.usi.startsWith("P") }
    if (sit.checkMate && pawnDrop) Option(sit.color)
    else if (sit.checkMate || sit.staleMate) Option(!sit.color)
    else if (sit.impasse || sit.perpetualCheck) Option(sit.color)
    else None
  }

  @nowarn
  def specialEnd(sit: Situation): Boolean = false

  @nowarn
  def specialDraw(sit: Situation) = false

  // Returns the material imbalance in pawns
  def materialImbalance(board: Board): Int =
    board.pieces.values.foldLeft(0) { case (acc, Piece(color, role)) =>
      acc + Role.valueOf(role) * color.fold(1, -1)
    } + board.handData.fold(0) { hs =>
      hs.value
    }

  // Returns true if neither player can win. The game should end immediately.
  def isInsufficientMaterial(board: Board) =
    ((board.handData.fold(0) { _.size } + board.pieces.size) <= 2) &&
      board.pieces.forall { p => p._2 is King }

  // Returns true if the other player cannot win. This is relevant when the
  // side to move times out or disconnects. Instead of losing on time,
  // the game should be drawn.
  def opponentHasInsufficientMaterial(situation: Situation) =
    (situation.board.handData
      .fold(0) { hs => hs(!situation.color).size } + situation.board.rolesOf(!situation.color).size) <= 2

  // Once a move has been decided upon from the available legal moves, the board is finalized
  def finalizeBoard(board: Board, usi: Usi, capture: Option[Piece], color: Color): Board = {
    val board2 = board updateHistory {
      _.withCheck(color, board.check(color))
    }
    usi match {
      case Usi.Move(_, _, _) =>
        board2.handData.fold(board2) { data =>
          capture.fold(board2) { p =>
            val unpromotedPiece = p.updateRole(unpromote).getOrElse(p)
            board2 withHandData data.store(unpromotedPiece)
          }
        }
      case _ => board2
    }
  }

  protected def unmovablePieces(board: Board) =
    board.pieces.exists { case (pos, piece) =>
      pieceInDeadZone(piece, pos)
    }

  protected def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    val pawnFiles = board.pieces.collect {
      case (pos, piece) if (piece is Pawn) && (piece is color) =>
        pos.x
    }.toList
    roles.length > 0 && roles.forall(allRoles contains _) &&
    (!strict || {
      roles.length <= pieces.size && roles.count(_ == King) == 1
    }) &&
    !unmovablePieces(board) && pawnFiles.distinct.length == pawnFiles.length &&
    roles.count(_ == King) <= 1 && board.handData.fold(true)(_.roles.forall(handRoles contains _))
  }

  def valid(board: Board, strict: Boolean) = Color.all forall validSide(board, strict) _

  val allRoles = List(
    Pawn,
    Lance,
    Knight,
    Silver,
    Gold,
    King,
    Bishop,
    Rook,
    PromotedLance,
    PromotedKnight,
    PromotedSilver,
    Dragon,
    Horse,
    Tokin
  )

  // Correct order
  val handRoles: List[Role] = List(
    Rook,
    Bishop,
    Gold,
    Silver,
    Knight,
    Lance,
    Pawn
  )

  val promotableRoles: List[Role] = List(
    Pawn,
    Lance,
    Knight,
    Silver,
    Bishop,
    Rook
  )

  def promote(r: Role): Option[Role] =
    r match {
      case Pawn   => Option(Tokin)
      case Lance  => Option(PromotedLance)
      case Knight => Option(PromotedKnight)
      case Silver => Option(PromotedSilver)
      case Bishop => Option(Horse)
      case Rook   => Option(Dragon)
      case _      => None
    }

  def unpromote(r: Role): Option[Role] = {
    r match {
      case Tokin          => Option(Pawn)
      case PromotedLance  => Option(Lance)
      case PromotedSilver => Option(Silver)
      case PromotedKnight => Option(Knight)
      case Horse          => Option(Bishop)
      case Dragon         => Option(Rook)
      case _              => None
    }
  }

  override def toString = s"Variant($name)"

  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]

  override def hashCode: Int = id

}

object Variant {

  val all = List(
    Standard,
    Minishogi,
    FromPosition
  )

  val byId = all map { v =>
    (v.id, v)
  } toMap

  val byKey = all map { v =>
    (v.key, v)
  } toMap

  val default = Standard

  def apply(id: Int): Option[Variant]     = byId get id
  def apply(key: String): Option[Variant] = byKey get key
  def orDefault(id: Int): Variant         = apply(id) | default
  def orDefault(key: String): Variant     = apply(key) | default

  def byName(name: String): Option[Variant] =
    all find (_.name.toLowerCase == name.toLowerCase)

  def exists(id: Int): Boolean = byId contains id

  val openingSensibleVariants: Set[Variant] = Set(
    shogi.variant.Standard
  )

  val divisionSensibleVariants: Set[Variant] = Set(
    shogi.variant.Standard,
    shogi.variant.FromPosition
  )

  private[variant] def defaultPieces: PieceMap =
    Map(
      SQ9I -> Sente.lance,
      SQ8I -> Sente.knight,
      SQ7I -> Sente.silver,
      SQ6I -> Sente.gold,
      SQ5I -> Sente.king,
      SQ4I -> Sente.gold,
      SQ3I -> Sente.silver,
      SQ2I -> Sente.knight,
      SQ1I -> Sente.lance,
      SQ8H -> Sente.bishop,
      SQ2H -> Sente.rook,
      SQ9G -> Sente.pawn,
      SQ8G -> Sente.pawn,
      SQ7G -> Sente.pawn,
      SQ6G -> Sente.pawn,
      SQ5G -> Sente.pawn,
      SQ4G -> Sente.pawn,
      SQ3G -> Sente.pawn,
      SQ2G -> Sente.pawn,
      SQ1G -> Sente.pawn,
      SQ9C -> Gote.pawn,
      SQ8C -> Gote.pawn,
      SQ7C -> Gote.pawn,
      SQ6C -> Gote.pawn,
      SQ5C -> Gote.pawn,
      SQ4C -> Gote.pawn,
      SQ3C -> Gote.pawn,
      SQ2C -> Gote.pawn,
      SQ1C -> Gote.pawn,
      SQ8B -> Gote.rook,
      SQ2B -> Gote.bishop,
      SQ9A -> Gote.lance,
      SQ8A -> Gote.knight,
      SQ7A -> Gote.silver,
      SQ6A -> Gote.gold,
      SQ5A -> Gote.king,
      SQ4A -> Gote.gold,
      SQ3A -> Gote.silver,
      SQ2A -> Gote.knight,
      SQ1A -> Gote.lance
    )

  private[variant] def defaultHand: Map[Role, Int] =
    Map(
      Rook   -> 0,
      Bishop -> 0,
      Gold   -> 0,
      Silver -> 0,
      Knight -> 0,
      Lance  -> 0,
      Pawn   -> 0
    )
}
