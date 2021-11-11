package shogi

sealed trait Role {
  val kif: String
  val csa: String
  val forsyth: Char
  val forsythFull: String

  lazy val forsythUpper: Char       = forsyth.toUpper
  lazy val forsythFullUpper: String = forsythFull.toUpperCase
  lazy val pgn: Char                = forsythUpper
  lazy val name: String             = toString.toLowerCase

  val senteProjectionDirs: Directions
  val goteProjectionDirs: Directions

  val senteShortDirs: Directions
  val goteShortDirs: Directions

  // generating next possible position of piece based on previous position
  // for pieces that don't have long range attacks it's gonna be None
  def dir(from: Pos, to: Pos): Option[Direction]
}

case object King extends Role {
  val kif         = "玉"
  val csa         = "OU"
  val forsyth     = 'k'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = Rook.senteProjectionDirs ::: Bishop.senteProjectionDirs
  val goteShortDirs       = senteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Rook extends Role {
  val kif         = "飛"
  val csa         = "HI"
  val forsyth     = 'r'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = List(_.up, _.down, _.left, _.right)
  val goteProjectionDirs  = senteProjectionDirs
  val senteShortDirs      = Nil
  val goteShortDirs       = Nil

  def dir(from: Pos, to: Pos) =
    if (to ?| from)
      Option(
        if (to ?^ from) (_.up) else (_.down)
      )
    else if (to ?- from)
      Option(
        if (to ?< from) (_.left) else (_.right)
      )
    else None
}

case object Bishop extends Role {
  val kif         = "角"
  val csa         = "KA"
  val forsyth     = 'b'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = List(_.upLeft, _.upRight, _.downLeft, _.downRight)
  val goteProjectionDirs  = senteProjectionDirs
  val senteShortDirs      = Nil
  val goteShortDirs       = Nil

  def dir(from: Pos, to: Pos) =
    if (to onSameDiagonal from)
      Option(
        if (to ?^ from) {
          if (to ?< from) (_.upLeft) else (_.upRight)
        } else {
          if (to ?< from) (_.downLeft) else (_.downRight)
        }
      )
    else None
}

case object Knight extends Role {
  val kif         = "桂"
  val csa         = "KE"
  val forsyth     = 'n'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs: Directions = List(
    p => Pos.at(p.x - 1, p.y - 2),
    p => Pos.at(p.x + 1, p.y - 2)
  )
  val goteShortDirs: Directions = List(
    p => Pos.at(p.x - 1, p.y + 2),
    p => Pos.at(p.x + 1, p.y + 2)
  )

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Pawn extends Role {
  val kif         = "歩"
  val csa         = "FU"
  val forsyth     = 'p'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = List(_.up)
  val goteShortDirs       = List(_.down)

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Gold extends Role {
  val kif         = "金"
  val csa         = "KI"
  val forsyth     = 'g'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = List(_.up, _.down, _.left, _.right, _.upLeft, _.upRight)
  val goteShortDirs       = List(_.up, _.down, _.left, _.right, _.downLeft, _.downRight)

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Silver extends Role {
  val kif         = "銀"
  val csa         = "GI"
  val forsyth     = 's'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = List(_.up, _.upLeft, _.upRight, _.downLeft, _.downRight)
  val goteShortDirs       = List(_.down, _.upLeft, _.upRight, _.downLeft, _.downRight)

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Lance extends Role {
  val kif         = "香"
  val csa         = "KY"
  val forsyth     = 'l'
  val forsythFull = forsyth.toString

  val senteProjectionDirs = List(_.up)
  val goteProjectionDirs  = List(_.down)
  val senteShortDirs      = Nil
  val goteShortDirs       = Nil

  def dir(from: Pos, to: Pos) =
    if (to ?| from)
      Option(
        if (to ?^ from) (_.up) else (_.down)
      )
    else None
}

case object Tokin extends Role {
  val kif         = "と"
  val csa         = "TO"
  val forsyth     = 't'
  val forsythFull = "+p"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object PromotedSilver extends Role {
  val kif         = "成銀"
  val csa         = "NG"
  val forsyth     = 'a'
  val forsythFull = "+s"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object PromotedKnight extends Role {
  val kif         = "成桂"
  val csa         = "NK"
  val forsyth     = 'm'
  val forsythFull = "+n"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object PromotedLance extends Role {
  val kif         = "成香"
  val csa         = "NY"
  val forsyth     = 'u'
  val forsythFull = "+l"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Horse extends Role {
  val kif         = "馬"
  val csa         = "UM"
  val forsyth     = 'h'
  val forsythFull = "+b"

  val senteProjectionDirs = Bishop.senteProjectionDirs
  val goteProjectionDirs  = Bishop.goteProjectionDirs
  val senteShortDirs      = Rook.senteProjectionDirs
  val goteShortDirs       = Rook.goteProjectionDirs

  def dir(from: Pos, to: Pos) = Bishop.dir(from, to)
}

case object Dragon extends Role {
  val kif         = "龍"
  val csa         = "RY"
  val forsyth     = 'd'
  val forsythFull = "+r"

  val senteProjectionDirs = Rook.senteProjectionDirs
  val goteProjectionDirs  = Rook.goteProjectionDirs
  val senteShortDirs      = Bishop.senteProjectionDirs
  val goteShortDirs       = Bishop.goteProjectionDirs

  def dir(from: Pos, to: Pos): Option[Direction] = Rook.dir(from, to)
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

  val allByForsyth: Map[Char, Role] = all map { r =>
    (r.forsyth, r)
  } toMap
  val allByPgn: Map[Char, Role] = all map { r =>
    (r.pgn, r)
  } toMap
  val allByName: Map[String, Role] = all map { r =>
    (r.name, r)
  } toMap

  val singleKifs = Map(
    "王" -> King,
    "兵" -> Pawn,
    "竜" -> Dragon,
    "全" -> PromotedSilver,
    "圭" -> PromotedKnight,
    "今" -> PromotedKnight,
    "杏" -> PromotedLance,
    "仝" -> PromotedLance,
    "个" -> Tokin
  )

  val allByKif: Map[String, Role] = (all map { r =>
    (r.kif, r)
  } toMap) ++ singleKifs

  val allByCsa: Map[String, Role] = all map { r =>
    (r.csa, r)
  } toMap

  val allByFullForsyth: Map[String, Role] = (all map { r =>
    (r.forsythFull, r)
  }) toMap

  val allByFullForsythUpper: Map[String, Role] = (all map { r =>
    (r.forsythFullUpper, r)
  }) toMap

  val allByEverything: Map[String, Role] =
    allByFullForsyth ++ allByFullForsythUpper ++ allByKif ++ allByCsa

  def forsyth(c: Char): Option[Role] = allByForsyth get c

  def kif(c: String): Option[Role] =
    allByKif get c

  def valueOf(r: Role): Int =
    r match {
      case Pawn                                                   => 1
      case Lance                                                  => 3
      case Knight                                                 => 4
      case Silver                                                 => 5
      case Gold | PromotedSilver | PromotedLance | PromotedKnight => 6
      case Tokin                                                  => 7
      case Bishop                                                 => 8
      case Rook                                                   => 10
      case Horse                                                  => 10
      case Dragon                                                 => 12
      case King                                                   => 0
    }

  def impasseValueOf(r: Role): Int =
    r match {
      case Bishop | Rook | Horse | Dragon => 5
      case King                           => 0
      case _                              => 1
    }
}
