package shogi

import cats.data.NonEmptyList

sealed trait Role {
  val forsyth: String
  val csa: String
  val kif: NonEmptyList[String]

  lazy val forsythUpper: String = forsyth.toUpperCase
  lazy val kifSingle: String    = kif.head

  lazy val name: String = toString.toLowerCase

  val senteProjectionDirs: Directions
  val goteProjectionDirs: Directions

  val senteShortDirs: Directions
  val goteShortDirs: Directions

  // generating next possible position of piece based on previous positions
  // for pieces that don't have long range attacks it's None
  def dir(from: Pos, to: Pos): Option[Direction]
}

case object King extends Role {
  val forsyth = "k"
  val csa     = "OU"
  val kif     = NonEmptyList.of("玉", "王")

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = Rook.senteProjectionDirs ::: Bishop.senteProjectionDirs
  val goteShortDirs       = senteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Rook extends Role {
  val forsyth = "r"
  val csa     = "HI"
  val kif     = NonEmptyList.of("飛")

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
  val forsyth = "b"
  val csa     = "KA"
  val kif     = NonEmptyList.of("角")

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
  val forsyth = "n"
  val csa     = "KE"
  val kif     = NonEmptyList.of("桂")

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
  val forsyth = "p"
  val csa     = "FU"
  val kif     = NonEmptyList.of("歩", "兵")

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = List(_.up)
  val goteShortDirs       = List(_.down)

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Gold extends Role {
  val forsyth = "g"
  val csa     = "KI"
  val kif     = NonEmptyList.of("金")

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = List(_.up, _.down, _.left, _.right, _.upLeft, _.upRight)
  val goteShortDirs       = List(_.up, _.down, _.left, _.right, _.downLeft, _.downRight)

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Silver extends Role {
  val forsyth = "s"
  val csa     = "GI"
  val kif     = NonEmptyList.of("銀")

  val senteProjectionDirs = Nil
  val goteProjectionDirs  = Nil
  val senteShortDirs      = List(_.up, _.upLeft, _.upRight, _.downLeft, _.downRight)
  val goteShortDirs       = List(_.down, _.upLeft, _.upRight, _.downLeft, _.downRight)

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Lance extends Role {
  val forsyth = "l"
  val csa     = "KY"
  val kif     = NonEmptyList.of("香")

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
  val forsyth = "+p"
  val csa     = "TO"
  val kif     = NonEmptyList.of("と", "个")

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object PromotedSilver extends Role {
  val forsyth = "+s"
  val csa     = "NG"
  val kif     = NonEmptyList.of("成銀", "全")

  override lazy val kifSingle = "全"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object PromotedKnight extends Role {
  val forsyth = "+n"
  val csa     = "NK"
  val kif     = NonEmptyList.of("成桂", "今", "圭")

  override lazy val kifSingle = "圭"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object PromotedLance extends Role {
  val forsyth = "+l"
  val csa     = "NY"
  val kif     = NonEmptyList.of("成香", "仝", "杏")

  override lazy val kifSingle = "杏"

  val senteProjectionDirs = Gold.senteProjectionDirs
  val goteProjectionDirs  = Gold.goteProjectionDirs
  val senteShortDirs      = Gold.senteShortDirs
  val goteShortDirs       = Gold.goteShortDirs

  def dir(from: Pos, to: Pos): Option[Direction] = None
}

case object Horse extends Role {
  val forsyth = "+b"
  val csa     = "UM"
  val kif     = NonEmptyList.of("馬")

  val senteProjectionDirs = Bishop.senteProjectionDirs
  val goteProjectionDirs  = Bishop.goteProjectionDirs
  val senteShortDirs      = Rook.senteProjectionDirs
  val goteShortDirs       = Rook.goteProjectionDirs

  def dir(from: Pos, to: Pos) = Bishop.dir(from, to)
}

case object Dragon extends Role {
  val forsyth = "+r"
  val csa     = "RY"
  val kif     = NonEmptyList.of("龍", "竜")

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

  val allByName: Map[String, Role] = all map { r =>
    (r.name, r)
  } toMap

  val allByForsyth: Map[String, Role] = all map { r =>
    (r.forsyth, r)
  } toMap

  val allByForsythUpper: Map[String, Role] = all map { r =>
    (r.forsythUpper, r)
  } toMap

  val allByKif: Map[String, Role] = all flatMap { r =>
    r.kif.toList map { k =>
      (k, r)
    }
  } toMap

  val allByCsa: Map[String, Role] = all map { r =>
    (r.csa, r)
  } toMap

  val allByEverything: Map[String, Role] =
    allByForsyth ++ allByForsythUpper ++ allByKif ++ allByCsa

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
