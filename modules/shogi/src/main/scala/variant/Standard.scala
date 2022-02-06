package shogi
package variant

import shogi.Pos._
import shogi.format.forsyth.Sfen

case object Standard
    extends Variant(
      id = 1,
      key = "standard",
      name = "Standard",
      shortName = "Std",
      title = "Standard rules of shogi"
    ) {

  val initialSfen = Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")

  val numberOfRanks: Int = 9
  val numberOfFiles: Int = 9

  val allPositions = Pos.all

  val pieces =
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

  val hands = Map.empty

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

  val handRoles: List[Role] = List(
    Rook,
    Bishop,
    Gold,
    Silver,
    Knight,
    Lance,
    Pawn
  )

  def promote(role: Role): Option[Role] =
    role match {
      case Pawn   => Some(Tokin)
      case Lance  => Some(PromotedLance)
      case Knight => Some(PromotedKnight)
      case Silver => Some(PromotedSilver)
      case Bishop => Some(Horse)
      case Rook   => Some(Dragon)
      case _      => None
    }

  def unpromote(role: Role): Option[Role] =
    role match {
      case Tokin          => Some(Pawn)
      case PromotedLance  => Some(Lance)
      case PromotedSilver => Some(Silver)
      case PromotedKnight => Some(Knight)
      case Horse          => Some(Bishop)
      case Dragon         => Some(Rook)
      case _              => None
    }

  def backrank(color: Color) =
    if (color.sente) Rank.A else Rank.I

  def promotionRanks(color: Color) =
    if (color.sente) List(Rank.A, Rank.B, Rank.C) else List(Rank.G, Rank.H, Rank.I)

}
