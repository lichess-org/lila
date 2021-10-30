package shogi
package variant

import cats.data.Validated
import cats.syntax.option._
import scala.annotation.nowarn

import format.Uci
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

  def pieces: Map[Pos, Piece]
  def hand: Map[Role, Int]

  def standard     = this == Standard
  def miniShogi    = this == MiniShogi
  def fromPosition = this == FromPosition

  def exotic = !standard

  def allSquares: List[Pos] = Pos.all9x9

  val numberOfRanks: Int = 9
  val numberOfFiles: Int = 9

  def initialFen = format.Forsyth.initial

  def backrank(color: Color)  = if(color == Sente) 1 else 9
  private def backrank2(color: Color) = if(color == Sente) 2 else 8

  // where color pieces promote
  def promotionZone(color: Color) = if(color == Sente) List(1, 2, 3) else List(7, 8, 9) 

  // true if piece will never be able to move from pos
  def pieceInDeadZone(piece: Piece, pos: Pos): Boolean =
    piece.role match {
      case Pawn | Lance if backrank(piece.color) == pos.y => true
      case Knight if (backrank(piece.color) == pos.y || backrank2(piece.color) == pos.y) => true
      case _ => false
    }

  def canPromote(move: Move): Boolean =
    promotableRoles.contains(move.piece.role) &&
      promotionZone(move.color).exists(sq => sq == move.dest.y || sq == move.orig.y)


  def validMoves(situation: Situation): Map[Pos, List[Move]] =
    situation.actors
      .collect {
        case actor if actor.moves.nonEmpty => actor.pos -> actor.moves
      }
      .to(Map)

  // Optimized for performance
  def pieceThreatened(board: Board, color: Color, to: Pos, filter: Piece => Boolean = _ => true): Boolean =
    board.pieces exists {
      case (pos, piece) if piece.color == color && filter(piece) && piece.eyes(pos, to) =>
        (!piece.longRangeDirs.nonEmpty) || (pos touches to) || piece.role.dir(pos, to).exists {
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

  def isValidPromotion(piece: Piece, promotion: Boolean, orig: Pos, dest: Pos): Boolean = 
    if (!promotion && pieceInDeadZone(piece, dest)) false
    else if (!promotion || promotionZone(piece.color).exists(sq => sq == dest.y || sq == orig.y))
      true
    else false


  def move(situation: Situation, from: Pos, to: Pos, promotion: Boolean): Validated[String, Move] = {

    // Find the move in the variant specific list of valid moves
    def findMove(from: Pos, to: Pos) = situation.moves get from flatMap (_.find(m => m.dest == to && m.promotion == promotion))

    for {
      actor <- situation.board.actors get from toValid s"No piece on $from"
      _ <- if (actor is situation.color) Validated.valid(actor)
        else Validated.invalid(s"Not my piece on $from")
      move1 <- findMove(from, to) toValid s"Piece on $from cannot move to $to"
    } yield move1
  }

  def isValidPieceDrop(piece: Piece, pos: Pos, sit: Situation) = 
    !pieceInDeadZone(piece, pos) &&
    (piece.role != Pawn || !(sit.board.occupiedPawnFiles(piece.color) contains pos.x))

  def drop(sit: Situation, role: Role, pos: Pos): Validated[String, Drop] =
    for {
      d1 <- sit.board.crazyData toValid "Board has no hand data"
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
      after = board1 withCrazyData d2
    )

  private def canDropStuff(sit: Situation) =
    sit.board.crazyData.fold(false) { (hands: Hands) =>
      val hand = hands(sit.color)
      hand.size > 0 && possibleDrops(sit).fold(true) { squares =>
        squares.nonEmpty && {
          squares.exists(s => hand.handMap.exists(kv => kv._2 > 0 && isValidPieceDrop(Piece(sit.color, kv._1), s, sit)))
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

  def staleMate(situation: Situation): Boolean =
    !situation.check && situation.moves.isEmpty && !canDropStuff(situation)

  def checkmate(situation: Situation) = situation.check && situation.moves.isEmpty && !canDropStuff(situation)

  // Player wins or loses after their move
  def winner(situation: Situation): Option[Color] = {
    val pawnDrop = situation.board.history.lastMove.fold(false) { l => l.uci(0) == 'P' }
    if (situation.checkMate && pawnDrop) Option(situation.color)
    else if (situation.checkMate || situation.staleMate) Option(!situation.color)
    else if (situation.impasse || situation.perpetualCheck) Option(situation.color)
    else None
  }

  @nowarn
  def specialEnd(situation: Situation): Boolean = false

  @nowarn def specialDraw(situation: Situation) = false

  /** Returns the material imbalance in pawns
    */
  def materialImbalance(board: Board): Int =
    board.pieces.values.foldLeft(0) { case (acc, Piece(color, role)) =>
      acc + Role.valueOf(role) * color.fold(1, -1)
    } + board.crazyData.fold(0) { hs =>
      hs.value
    }

  /** Returns true if neither player can win. The game should end immediately.
    */
  def isInsufficientMaterial(board: Board) =
    ((board.crazyData.fold(0) { _.size } + board.pieces.size) <= 2) &&
      board.pieces.forall { p => p._2 is King }

  /** Returns true if the other player cannot win. This is relevant when the
    * side to move times out or disconnects. Instead of losing on time,
    * the game should be drawn.
    */
  def opponentHasInsufficientMaterial(situation: Situation) =
    (situation.board.crazyData
      .fold(0) { hs => hs(!situation.color).size } + situation.board.piecesOf(!situation.color).size) <= 2

  // Some variants could have an extra effect on the board on a move
  def hasMoveEffects = false

  /** Applies a variant specific effect to the move. This helps decide whether a king is endangered by a move, for
    * example
    */
  def addVariantEffect(move: Move): Move = move

  /** Once a move has been decided upon from the available legal moves, the board is finalized
    */
  def finalizeBoard(board: Board, uci: format.Uci, capture: Option[Piece], color: Color): Board = {
    val board2 = board updateHistory {
      _.withCheck(color, board.check(color))
    }
    uci match {
      case Uci.Move(_, _, _) =>
        board2.crazyData.fold(board2) { data =>
          capture.fold(board2) { p =>
            val unpromotedRole = demote(p.role).getOrElse(p.role)
            val unpromotedPiece = Piece(p.color, unpromotedRole)
            board2 withCrazyData data.store(unpromotedPiece)
          }
        }
      case _ => board2
    }
  }

  protected def unmovablePieces(board: Board) =
    board.pieces.exists {
      case (pos, piece) => pieceInDeadZone(piece, pos)
    }

  protected def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles     = board rolesOf color
    val pawnFiles = board occupiedPawnFiles color
    roles.length > 0 &&
    (!strict || { roles.count(_ == Pawn) <= numberOfFiles && roles.length <= pieces.size && roles.count(_ == King) == 1 }) &&
    !unmovablePieces(board) && pawnFiles.distinct.length == pawnFiles.length &&
    roles.count(_ == King) <= 1 && board.crazyData.fold(true)(_.roles.forall(handRoles contains _))
  }

  def valid(board: Board, strict: Boolean) = Color.all forall validSide(board, strict) _

  val roles = List(
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

  def demote(r: Role): Option[Role] = {
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

  lazy val rolesByPgn: Map[Char, Role] = roles
    .map { r =>
      (r.pgn, r)
    }
    .to(Map)

  lazy val rolesByForsyth: Map[Char, Role] = roles
    .map { r =>
      (r.forsyth, r)
    }
    .to(Map)

  lazy val rolesByFullForsyth: Map[String, Role] = roles
    .map { r =>
      (r.forsythFull.toUpperCase, r)
    }
    .to(Map)

  lazy val rolesByCsa: Map[String, Role] = roles
    .map { r =>
      (r.csa, r)
    }
    .to(Map)

  lazy val rolesByEverything: Map[String, Role] =
    Role.allByKif ++ rolesByFullForsyth ++ rolesByCsa

  override def toString = s"Variant($name)"

  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]

  override def hashCode: Int = id

}

object Variant {

  val all = List(
    Standard,
    MiniShogi,
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

  private[variant] def defaultPieces: Map[Pos, Piece] =
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
