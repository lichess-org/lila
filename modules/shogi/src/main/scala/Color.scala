package shogi

sealed trait Color {

  def -(role: Role) = Piece(this, role)

  def fold[A](s: => A, g: => A): A = if (sente) s else g

  val unary_! : Color

  val passablePawnY: Int
  val promotableZone: List[Int]
  val backrankY: Int
  val backrankY2: Int

  val letter: Char
  val name: String
  val engName: String

  def pawn           = this - Pawn
  def gold           = this - Gold
  def silver         = this - Silver
  def lance          = this - Lance
  def bishop         = this - Bishop
  def knight         = this - Knight
  def rook           = this - Rook
  def tokin          = this - Tokin
  def dragon         = this - Dragon
  def horse          = this - Horse
  def promotedsilver = this - PromotedSilver
  def promotedknight = this - PromotedKnight
  def promotedlance  = this - PromotedLance
  def king           = this - King

  val sente = this == Color.Sente
  val gote  = this == Color.Gote
}

object Color {

  case class Map[A](sente: A, gote: A) {
    def apply(color: Color) = if (color.sente) sente else gote

    def update(color: Color, f: A => A) = {
      if (color.sente) copy(sente = f(sente))
      else copy(gote = f(gote))
    }

    def map[B](fs: A => B, fg: A => B) = copy(sente = fs(sente), gote = fg(gote))

    def map[B](f: A => B): Map[B] = map(f, f)

    def all: Seq[A] = Seq(sente, gote)

    def reduce[B](f: (A, A) => B) = f(sente, gote)

    def forall(pred: A => Boolean) = pred(sente) && pred(gote)

    def exists(pred: A => Boolean) = pred(sente) || pred(gote)
  }

  object Map {
    def apply[A](f: Color => A): Map[A] = Map(sente = f(Sente), gote = f(Gote))
  }

  case object Sente extends Color {

    lazy val unary_! = Gote

    val passablePawnY  = 5
    val promotableZone = List(7, 8, 9)
    val backrankY      = 9
    val backrankY2     = 8

    val letter  = 'b'
    val engName = "black"
    val name    = "sente"

    override val hashCode = 1
  }

  case object Gote extends Color {

    val unary_! = Sente

    val passablePawnY  = 4
    val promotableZone = List(1, 2, 3)
    val backrankY      = 1
    val backrankY2     = 2

    val letter  = 'w'
    val engName = "white"
    val name    = "gote"

    override val hashCode = 2
  }

  def fromPly(ply: Int) = apply((ply & 1) == 0)

  def apply(b: Boolean): Color = if (b) Sente else Gote

  def apply(n: String): Option[Color] =
    if (n == "black" || n == "sente") Some(Sente)
    else if (n == "white" || n == "gote") Some(Gote)
    else None

  def apply(c: Char): Option[Color] =
    if (c == 'b') Some(Sente)
    else if (c == 'w') Some(Gote)
    else None

  val sente: Color = Sente
  val gote: Color  = Gote

  val all = List(Sente, Gote)

  val names = all map (_.name)

  def exists(name: String) = all exists (_.name == name)

  def showResult(color: Option[Color]) =
    color match {
      case Some(shogi.Sente) => "1-0"
      case Some(shogi.Gote)  => "0-1"
      case None              => "1/2-1/2"
    }

  def fromResult(result: String): Option[Color] =
    result match {
      case "1-0" => Some(shogi.Sente)
      case "0-1" => Some(shogi.Gote)
      case _     => None
    }
}
