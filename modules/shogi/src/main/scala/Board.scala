package shogi

import variant.Variant

case class Board(
    pieces: PieceMap,
    history: History,
    variant: Variant,
    crazyData: Option[Hands] = None
) {

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(x: Int, y: Int): Option[Piece] = Pos.at(x, y) flatMap pieces.get

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
        case piece if piece is c => piece.role
      }
      .to(List)

  def occupiedPawnFiles(c: Color): List[Int] =
    pieces
      .collect {
        case (pos, piece) if (piece is c) && (piece is Pawn) => pos.x
      }
      .to(List)

  def rolesInPromotionZoneOf(c: Color): List[Role] =
    pieces
      .collect {
        case (pos, piece) if (piece is c) && (variant.promotionRanks(c) contains pos.y) => piece.role
      }
      .to(List)

  def actorAt(at: Pos): Option[Actor] = actors get at

  def piecesOf(c: Color): Map[Pos, Piece] = pieces filter (_._2 is c)
  def piecesOf(r: Role): Map[Pos, Piece]  = pieces filter (_._2 is r)

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
    else Option(copy(pieces = pieces + ((at, piece))))

  def take(at: Pos): Option[Board] =
    if (pieces contains at) Option(copy(pieces = pieces - at))
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
      promotedRole <- variant.promote(piece.role)
      b2           <- take(pos)
      b3           <- b2.place(Piece(piece.color, promotedRole), pos)
    } yield b3

  def withHistory(h: History): Board = copy(history = h)

  def withPieces(newPieces: PieceMap) = copy(pieces = newPieces)

  def withVariant(v: Variant): Board = {
    copy(variant = v).ensureCrazyData
  }

  def withCrazyData(data: Hands)         = copy(crazyData = Option(data))
  def withCrazyData(data: Option[Hands]) = copy(crazyData = data)
  def withCrazyData(f: Hands => Hands): Board =
    withCrazyData(f(crazyData | Hands.init(variant)))

  def ensureCrazyData = withCrazyData(crazyData | Hands.init(variant))

  def updateHistory(f: History => History) = copy(history = f(history))

  def count(p: Piece): Int = pieces.values count (_ == p)
  def count(r: Role): Int  = pieces.values count (_.role == r)
  def count(c: Color): Int = pieces.values count (_.color == c)

  def kingEntered(c: Color): Boolean =
    kingPosOf(c) exists (pos => variant.promotionRanks(c) contains pos.y)

  def enoughImpasseValue(c: Color): Boolean = {
    val rp = rolesInPromotionZoneOf(c)
    val piecesValue = rp.foldLeft(0) { (acc, r) => acc + Role.impasseValueOf(r) } +
      crazyData.fold(0)(h => h.impasseValueOf(c))
    rp.size > 10 && piecesValue >= c.fold(28, 27)
  }

  def tryRule(c: Color): Boolean =
    kingPosOf(c) == c.fold(Pos.at(5, 1), Pos.at(5, 9))

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
    Board(pieces.toMap, History(), variant, Option(Hands.init(variant)))

  def init(variant: Variant): Board = Board(variant.pieces, variant)

  def empty(variant: Variant): Board = Board(Nil, variant)
}
