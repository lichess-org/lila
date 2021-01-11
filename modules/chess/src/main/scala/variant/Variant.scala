package chess
package variant

import scala.annotation.nowarn
import scalaz.Validation.failureNel
import scalaz.Validation.FlatMap._

import Pos.posAt
import format.Uci


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

  def standard      = this == Standard
  def chess960      = this == Chess960
  def fromPosition  = this == FromPosition
  def kingOfTheHill = this == KingOfTheHill
  def threeCheck    = this == ThreeCheck
  def antichess     = this == Antichess
  def atomic        = this == Atomic
  def horde         = this == Horde
  def racingKings   = this == RacingKings
  def crazyhouse    = this == Crazyhouse

  def exotic = !standard

  def allowsCastling = !castles.isEmpty

  protected val backRank  = Vector(Lance, Knight, Silver, Gold, King, Gold, Silver, Knight, Lance)
  protected val backRank2 = Vector(Rook, Bishop)

  def castles: Castles = Castles.all

  def initialFen = format.Forsyth.initial

  def isValidPromotion(piece: Piece, promotion: Boolean, orig: Pos, dest: Pos) = {
    piece match {
      case p if (p.role == Pawn && p.color.backrankY == dest.y && !promotion) => false
      case p if (p.role == Lance && p.color.backrankY == dest.y && !promotion) => false
      case p if (p.role == Knight && (p.color.backrankY == dest.y || p.color.backrankY2 == dest.y) && !promotion) => false
      case p if promotion && (p.color.promotableZone contains orig.y) || (p.color.promotableZone contains dest.y) => true
      case _ if !promotion => true
      case _ => false
    }
  }

  def validMoves(situation: Situation): Map[Pos, List[Move]] =
    situation.actors
      .collect {
        case actor if actor.moves.nonEmpty => actor.pos -> actor.moves
      }
      .to(Map)

  // Optimized for performance
  def pieceThreatened(board: Board, color: Color, to: Pos, filter: Piece => Boolean = _ => true): Boolean = {
    board.pieces exists {
      case (pos, piece) if piece.color == color && filter(piece) && piece.eyes(pos, to) =>
        (!piece.role.projection) || (piece.role.dir(pos, to).exists {
          longRangeThreatens(board, pos, _, to)
        }) || ((piece.role == Horse || piece.role == Dragon) && (pos touches to))
      case _ => false
    }
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
      if ((a.piece is King) || a.check) (_ => true) else (_.role.projection),
      if (a.piece.role == King) None else a.board kingPosOf a.color
    )

  def longRangeThreatens(board: Board, p: Pos, dir: Direction, to: Pos): Boolean =
    dir(p) exists { next =>
      next == to || (!board.pieces.contains(next) && longRangeThreatens(board, next, dir, to))
    }

  def move(situation: Situation, from: Pos, to: Pos, promotion: Boolean): Valid[Move] = {

    // Find the move in the variant specific list of valid moves
    def findMove(from: Pos, to: Pos) = situation.moves get from flatMap (_.find(_.dest == to))

    for {
      actor <- situation.board.actors get from toValid "No piece on " + from
      _     <- actor.validIf(actor is situation.color, "Not my piece on " + from)
      m1    <- findMove(from, to) toValid "Piece on " + from + " cannot move to " + to
      m2    <- m1 withPromotion(Role.promotesTo(actor.piece.role), promotion) toValid "Piece on " + from + " cannot promote to " + promotion
      m3    <- m2 validIf (isValidPromotion(actor.piece, promotion, from, to), "Cannot promote to " + promotion + " in this game mode")
    } yield m3
  }

  def validPieceDrop(role: Role, pos: Pos, situation: Situation) = {
    role match {
      case Pawn => pos.y != situation.color.backrankY &&
      !(situation.board.occupiedPawnFiles(situation.color) contains pos.x)
      case Lance => pos.y != situation.color.backrankY
      case Knight => pos.y != situation.color.backrankY && pos.y != situation.color.backrankY2
      case _ => true
    }
  }

  def drop(situation: Situation, role: Role, pos: Pos): Valid[Drop] =
    for {
      d1 <- situation.board.crazyData toValid "Board has no crazyhouse data"
      _  <- d1.validIf(validPieceDrop(role, pos, situation), s"Can't drop $role on $pos")
      piece = Piece(situation.color, role)
      d2     <- d1.drop(piece) toValid s"No $piece to drop on $pos"
      board1 <- situation.board.place(piece, pos) toValid s"Can't drop $role on $pos, it's occupied"
      _      <- board1.validIf(!board1.check(situation.color), s"Dropping $role on $pos doesn't uncheck the king")
    } yield Drop(
      piece = piece,
      pos = pos,
      situationBefore = situation,
      after = board1 withCrazyData d2
    )

  private def canDropStuff(situation: Situation) =
    situation.board.crazyData.fold(false) { (data: Data) =>
      val roles = data.pockets(situation.color).roles
      roles.nonEmpty && possibleDrops(situation).fold(true) { squares =>
        squares.nonEmpty && {
          squares.exists(s => roles.exists(r => validPieceDrop(r, s, situation)))
        }
      }
    }

  def possibleDrops(situation: Situation): Option[List[Pos]] =
    if (!situation.check) None
    else situation.kingPos.map { blockades(situation, _) }

    private def blockades(situation: Situation, kingPos: Pos): List[Pos] = {
    def attacker(piece: Piece) = piece.role.projection && piece.color != situation.color
    def forward(p: Pos, dir: Direction, squares: List[Pos]): List[Pos] =
      dir(p) match {
        case None                                                 => Nil
        case Some(next) if situation.board(next).exists(attacker) => next :: squares
        case Some(next) if situation.board(next).isDefined        => Nil
        case Some(next)                                           => forward(next, dir, next :: squares)
      }
    King.dirs flatMap { forward(kingPos, _, Nil) } filter { square =>
      situation.board.place(Piece(situation.color, Knight), square) exists { defended =>
        !defended.check(situation.color)
      }
    }
  }

  def staleMate(situation: Situation): Boolean = !situation.check && situation.moves.isEmpty && !canDropStuff(situation)

  def checkmate(situation: Situation) = situation.check && situation.moves.isEmpty && !canDropStuff(situation)

  // In most variants, the winner is the last player to have played and there is a possibility of either a traditional
  // checkmate or a variant end condition
  def winner(situation: Situation): Option[Color] = {
    val lastMove = situation.board.history.lastMove
    if(situation.checkMate && lastMove.isDefined && lastMove.get.uci(0) == 'P') Some(situation.color)
    else if (situation.checkMate) Some(!situation.color)
    else if (situation.staleMate) Some(!situation.color)
    else if(situation.board.tryRule) situation.board.tryRuleColor(!situation.color)
    else if(situation.board.perpetualCheck) situation.board.perpetualCheckColor
    else None
  }

  @nowarn
  def specialEnd(situation: Situation) : Boolean = false

  @nowarn def specialDraw(situation: Situation) = false

  /**
    * Returns the material imbalance in pawns (overridden in Antichess)
    */
  def materialImbalance(board: Board): Int =
    board.pieces.values.foldLeft(0) {
      case (acc, Piece(color, role)) =>
        Role.valueOf(role).fold(acc) { value =>
          acc + value * color.fold(1, -1)
        }
    }

  /**
    * Returns true if neither player can win. The game should end immediately.
    */
  def isInsufficientMaterial(board: Board) = false

  /**
    * Returns true if the other player cannot win. This is relevant when the
    * side to move times out or disconnects. Instead of losing on time,
    * the game should be drawn.
    */
  def opponentHasInsufficientMaterial(situation: Situation) = false
  // Some variants have an extra effect on the board on a move. For example, in Atomic, some
  // pieces surrounding a capture explode
  def hasMoveEffects = false

  /**
    * Applies a variant specific effect to the move. This helps decide whether a king is endangered by a move, for
    * example
    */
  def addVariantEffect(move: Move): Move = move

  def fiftyMoves(history: History): Boolean = false

  def isIrreversible(move: Move): Boolean = false

  /**
    * Once a move has been decided upon from the available legal moves, the board is finalized
    */
  def finalizeBoard(board: Board, uci: format.Uci, capture: Option[Piece], color: Color): Board = {
    val board2 = board updateHistory {
      _.withCheck(color, board.check(color))
    }
    uci match {
      case Uci.Move(orig, dest, promOption) =>
        board2.crazyData.fold(board2) { data =>
          val d1 = capture.fold(data) { data.store(_, dest) }
          board2 withCrazyData d1
        }
      case _ => board2
    }
  }

  protected def pieceInPromotionRank(board: Board, color: Color) = {
    board.pieces.exists {
      case (pos, Piece(c, r)) if c == color && (r == Pawn || r == Lance) && (c.backrankY == pos.y) => true
      case (pos, Piece(c, r)) if c == color && (r == Knight) && (c.backrankY == pos.y || c.backrankY2 == pos.y) => true
      case _                                                                                       => false
    }
  }

  protected def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    roles.size > 0 &&
    (!strict || { roles.count(_ == Pawn) <= 9 && roles.size <= 40 }) &&
    !pieceInPromotionRank(board, color)
  }

  def valid(board: Board, strict: Boolean) = Color.all forall validSide(board, strict) _

  val roles = List(Lance, Knight, Silver, Gold, King, Rook, Bishop, Pawn,
  PromotedLance, PromotedKnight, PromotedSilver, Dragon, Horse, Tokin)

  val promotableRoles: List[PromotableRole] = List(Rook, Bishop, Knight, Lance, Silver, Pawn,
  Dragon, Horse, PromotedKnight, PromotedLance, PromotedSilver, Tokin)

  lazy val rolesByPgn: Map[Char, Role] = roles
    .map { r =>
      (r.pgn, r)
    }
    .to(Map)

  lazy val rolesPromotableByPgn: Map[Char, PromotableRole] =
    promotableRoles
      .map { r =>
        (r.pgn, r)
      }
      .to(Map)

  override def toString = s"Variant($name)"

  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]

  override def hashCode: Int = id

}

object Variant {

  val all = List(
    Standard,
    Crazyhouse,
    Chess960,
    FromPosition,
    KingOfTheHill,
    ThreeCheck,
    Antichess,
    Atomic,
    Horde,
    RacingKings
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
    chess.variant.Standard,
  )

  val divisionSensibleVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.FromPosition
  )

  private[variant] def symmetricRank(rank1: IndexedSeq[Role], rank2: IndexedSeq[Role]): Map[Pos, Piece] =
    (for (y <- Seq(1, 3, 7, 9); x <- 1 to 9) yield {
      posAt(x, y) map { pos =>
        (
          pos,
          y match {
            case 1 => White - rank1(x - 1)
            case 3 => White.pawn
            case 7 => Black.pawn
            case 9 => Black - rank1(x - 1)
          }
        )
      }
    }).flatten.toMap ++ Map(
      posAt(2, 2).get -> (White - rank2(1)),
      posAt(8, 2).get -> (White - rank2(0)),
      posAt(2, 8).get -> (Black - rank2(0)),
      posAt(8, 8).get -> (Black - rank2(1))
    )
}
