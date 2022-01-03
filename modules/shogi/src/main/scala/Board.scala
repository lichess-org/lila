package shogi

import variant.Variant

case class Board(
    pieces: PieceMap,
    history: History,
    variant: Variant,
    handData: Option[Hands] = None
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

  def actorAt(at: Pos): Option[Actor] = actors get at

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

  def kingEntered(c: Color): Boolean =
    kingPosOf(c) exists (pos => variant.promotionRanks(c) contains pos.y)

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

  def taking(orig: Pos, dest: Pos): Option[Board] =
    pieces.get(orig) map { piece =>
      copy(pieces = pieces - orig + (dest -> piece))
    }

  lazy val occupation: Color.Map[Set[Pos]] = Color.Map { color =>
    pieces.collect { case (pos, piece) if piece is color => pos }.to(Set)
  }

  def hasPiece(p: Piece) = pieces.values exists (p ==)

  def promote(pos: Pos): Option[Board] =
    for {
      piece         <- apply(pos)
      promotedPiece <- piece.updateRole(variant.promote)
      b2            <- take(pos)
      b3            <- b2.place(promotedPiece, pos)
    } yield b3

  def withHistory(h: History): Board = copy(history = h)

  def withPieces(newPieces: PieceMap) = copy(pieces = newPieces)

  def withVariant(v: Variant): Board = {
    copy(variant = v).ensureHandData
  }

  def withHandData(data: Hands)         = copy(handData = Option(data))
  def withHandData(data: Option[Hands]) = copy(handData = data)
  def withHandData(f: Hands => Hands): Board =
    withHandData(f(handData | Hands.init(variant)))

  def ensureHandData = withHandData(handData | Hands.init(variant))

  def updateHistory(f: History => History) = copy(history = f(history))

  def count(p: Piece): Int = pieces.values count (_ == p)
  def count(r: Role): Int  = pieces.values count (_.role == r)
  def count(c: Color): Int = pieces.values count (_.color == c)

  def autoDraw: Boolean =
    (history.fourfoldRepetition && !history.perpetualCheck) || variant.isInsufficientMaterial(this)

  def situationOf(color: Color) = Situation(this, color)

  def visual = format.Visual >> this

  def valid(strict: Boolean) = variant.valid(this, strict)

  def materialImbalance: Int = variant.materialImbalance(this)

  override def toString = s"$variant, Position after ${history.lastMove}\n$visual"
}

object Board {

  def apply(pieces: Iterable[(Pos, Piece)], variant: Variant): Board =
    Board(pieces.toMap, History(), variant, variantHandData(variant))

  def init(variant: Variant): Board = Board(variant.pieces, variant)

  def empty(variant: Variant): Board = Board(Nil, variant)

  private def variantHandData(variant: Variant) =
    (variant.hasHandData) option Hands.init(variant)
}
