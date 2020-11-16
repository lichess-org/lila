package chess

import Pos.posAt

sealed trait Role {
  val forsyth: Char
  lazy val forsythUpper: Char = forsyth.toUpper
  lazy val pgn: Char          = forsythUpper
  lazy val name               = toString.toLowerCase
  val projection: Boolean
  val dirs: Directions
  val dirsOpposite: Directions
  def dir(from: Pos, to: Pos): Option[Direction]
}
sealed trait PromotableRole extends Role {
}

case object King extends Role {
  val forsyth                  = 'k'
  val dirs: Directions         = Rook.dirs ::: Bishop.dirs
  val dirsOpposite: Directions = dirs
  def dir(from: Pos, to: Pos)  = None
  val projection               = false
}
case object Rook extends PromotableRole {
  val forsyth                  = 'r'
  val dirs: Directions         = List(_.up, _.down, _.left, _.right)
  val dirsOpposite: Directions = dirs
  def dir(from: Pos, to: Pos) =
    if (to ?| from)
      Some(
        if (to ?^ from) (_.up) else (_.down)
      )
    else if (to ?- from)
      Some(
        if (to ?< from) (_.left) else (_.right)
      )
    else None
  val projection               = true
}
case object Bishop extends PromotableRole {
  val forsyth                  = 'b'
  val dirs: Directions         = List(_.upLeft, _.upRight, _.downLeft, _.downRight)
  val dirsOpposite: Directions = dirs
  def dir(from: Pos, to: Pos) =
    if (to onSameDiagonal from)
      Some(
        if (to ?^ from) {
          if (to ?< from) (_.upLeft) else (_.upRight)
        } else {
          if (to ?< from) (_.downLeft) else (_.downRight)
        }
      )
    else None
  val projection               = true
}
case object Knight extends PromotableRole {
  val forsyth = 'n'
  val dirs: Directions = List(
    p => posAt(p.x - 1, p.y + 2),
    p => posAt(p.x + 1, p.y + 2)
  )
  val dirsOpposite: Directions = List(
    p => posAt(p.x - 1, p.y - 2),
    p => posAt(p.x + 1, p.y - 2)
  )
  def dir(from: Pos, to: Pos)  = None
  val projection               = false
}
case object Pawn extends PromotableRole {
  val forsyth                  = 'p'
  val dirs: Directions         = List(_.up)
  val dirsOpposite: Directions = List(_.down)
  def dir(from: Pos, to: Pos)  = None
  val projection               = false
}
case object Gold extends Role {
  val forsyth                  = 'g'
  val dirs: Directions         = List(_.up, _.down, _.left, _.right, _.upLeft, _.upRight)
  val dirsOpposite: Directions = List(_.up, _.down, _.left, _.right, _.downLeft, _.downRight)
  def dir(from: Pos, to: Pos)  = None
  val projection               = false
}
case object Silver extends PromotableRole {
  val forsyth                  = 's'
  val dirs: Directions         = List(_.up, _.upLeft, _.upRight, _.downLeft, _.downRight)
  val dirsOpposite: Directions = List(_.down, _.upLeft, _.upRight, _.downLeft, _.downRight)
  def dir(from: Pos, to: Pos)  = None
  val projection               = false
}
case object Lance extends PromotableRole {
  val forsyth                  = 'l'
  val dirs: Directions         = List(_.up)
  val dirsOpposite: Directions = List(_.down)
  def dir(from: Pos, to: Pos)  =
    if (to ?| from)
      Some(
        if (to ?^ from) (_.up) else (_.down)
      ) else None

  val projection               = true
}
case object Tokin extends PromotableRole {
  val forsyth                                    = 't' // +p
  val dirs: Directions                           = Gold.dirs
  val dirsOpposite: Directions                   = Gold.dirsOpposite
  def dir(from: Pos, to: Pos): Option[Direction] = None
  val projection: Boolean                        = false
}
case object PromotedSilver extends PromotableRole {
  val forsyth: Char                              = 'a' // +s
  val dirs: Directions                           = Gold.dirs
  val dirsOpposite: Directions                   = Gold.dirsOpposite
  def dir(from: Pos, to: Pos): Option[Direction] = None
  val projection: Boolean                        = false
}
case object PromotedKnight extends PromotableRole {
  val forsyth: Char                              = 'm' // +n
  val dirs: Directions                           = Gold.dirs
  val dirsOpposite: Directions                   = Gold.dirsOpposite
  def dir(from: Pos, to: Pos): Option[Direction] = None
  val projection: Boolean                        = false
}
case object PromotedLance extends PromotableRole {
  val forsyth: Char                              = 'u' // +l
  val dirs: Directions                           = Gold.dirs
  val dirsOpposite: Directions                   = Gold.dirsOpposite
  def dir(from: Pos, to: Pos): Option[Direction] = None
  val projection: Boolean                        = false
}
case object Horse extends PromotableRole {
  val forsyth: Char                = 'h'         // +b
  val dirs: Directions             = Bishop.dirs //todo
  val dirsOpposite: Directions     = Bishop.dirsOpposite
  def dir(from: Pos, to: Pos)      = Bishop.dir(from, to)
  val projection                   = true
}
case object Dragon extends PromotableRole {
  val forsyth: Char                              = 'd'
  val dirs: Directions                           = Rook.dirs //todo
  val dirsOpposite: Directions                   = Rook.dirsOpposite
  def dir(from: Pos, to: Pos): Option[Direction] = Rook.dir(from, to)
  val projection: Boolean                        = true
}

object Role {

  val all: List[Role] = List(
    King,
    Rook,
    Bishop,
    Knight,
    Pawn,
    Gold,
    Silver,
    Lance,
    Tokin,
    Horse,
    PromotedSilver,
    PromotedKnight,
    PromotedLance,
    Dragon
  )
  val allPromotable: List[PromotableRole] = List(Rook, Bishop, Knight, Lance, Silver, Pawn,
  Dragon, Horse, PromotedKnight, PromotedLance, PromotedSilver, Tokin)
  val allByForsyth: Map[Char, Role] = all map { r =>
    (r.forsyth, r)
  } toMap
  val allByPgn: Map[Char, Role] = all map { r =>
    (r.pgn, r)
  } toMap
  val allByName: Map[String, Role] = all map { r =>
    (r.name, r)
  } toMap
  val allPromotableByName: Map[String, PromotableRole] =
    allPromotable map { r =>
      (r.toString, r)
    } toMap
  val allPromotableByForsyth: Map[Char, PromotableRole] =
    allPromotable map { r =>
      (r.forsyth, r)
    } toMap
  val allPromotableByPgn: Map[Char, PromotableRole] =
    allPromotable map { r =>
      (r.pgn, r)
    } toMap

  def forsyth(c: Char): Option[Role] = allByForsyth get c

  def promotable(c: Char): Option[PromotableRole] =
    allPromotableByForsyth get c

  def promotable(name: String): Option[PromotableRole] =
    allPromotableByName get name.capitalize

  def promotable(name: Option[String]): Option[PromotableRole] =
    name flatMap promotable

  def promotesTo(r: Role): Option[PromotableRole] =
    r match {
      case Pawn => Some(Tokin)
      case Lance => Some(PromotedLance)
      case Knight => Some(PromotedKnight)
      case Silver => Some(PromotedSilver)
      case Bishop => Some(Horse)
      case Rook => Some(Dragon)
      case _ => None
    }

  def demotesTo(r: Role): Role = {
    r match {
      case Tokin => Pawn
      case PromotedLance => Lance
      case PromotedSilver => Silver
      case PromotedKnight => Knight
      case Horse => Bishop
      case Dragon => Rook
      case _ => r
    }
  }

  def valueOf(r: Role): Option[Int] =
    r match {
      case Pawn                                                   => Some(1)
      case Lance                                                  => Some(3)
      case Knight                                                 => Some(4)
      case Silver                                                 => Some(5)
      case Gold | PromotedSilver | PromotedLance | PromotedKnight => Some(6)
      case Tokin                                                  => Some(7)
      case Bishop                                                 => Some(8)
      case Rook                                                   => Some(10)
      case Horse                                                  => Some(10)
      case Dragon                                                 => Some(12)
      case King                                                   => None
    }
}
