package lila.puzzle

sealed abstract class Tag(
    val name: Tag.Name,
    val desc: Tag.Desc) {

    val id = toString
}

object Tag {

  type Name = String
  type Desc = String

    case object Fork extends Tag(
        name = "Fork/Double Attack",
        desc = "Attack two pieces at once")

    case object HangingMaterial extends Tag(
        name = "Hanging Material",
        desc = "An undefended piece that can be taken without potential recapture")
    
    case object RemovingDefender extends Tag(
        name = "Removing the Defender",
        desc = "Remove the defender of a valuable piece or a mating square")

    case object DiscoveredAttack extends Tag(
        name = "Discovered Attack",
        desc = "Create an attack with a piece by moving another piece out of the line of sight")

    case object Deflection extends Tag(
        name = "Deflection",
        desc = "Force your opponent to move a piece enabling you to create further attacks")
    
    case object Overload extends Tag(
        name = "Overloading",
        desc = "Exploit a piece that has been given too many defensive roles")
    
    case object Interference extends Tag(
        name = "Interference",
        desc = "Thwart your opponent's attack or defense usually by sacrificing a piece")
    
    case object Pin extends Tag(
        name = "Exploiting a Pin",
        desc = "Exploit a piece that cannot move due to being pinned (usually against the king)")

    case object Skewer extends Tag(
        name = "Skewer",
        desc = "Similar to a pin, but usually with the king infront of a hanging piece")
    
    case object SmotheredMate extends Tag(
        name = "Smothered Mate",
        desc = "Checkmate the king with only a knight. All other squares are occupied by the opponent's pieces")

    case object ShelterDestruction extends Tag(
        name = "Destruction of the King's Shelter",
        desc = "Destroy the pawn structure around the king enabling further attacks against the king")

    case object TrappedPiece extends Tag(
        name = "Trapped Piece/Mobility",
        desc = "A piece that can move, however any legal move will cause that piece to be captured")

    case object BackRank extends Tag(
        name = "Weak Back Rank",
        desc = "The first rank of your opponent can be infiltrated (usually by a queen or a rook) unless defended")

    case object Promotion extends Tag(
        name = "Promotion",
        desc = "Reach the back rank with a pawn to promote it to a queen")

    case object UnderPromotion extends Tag(
        name = "Under Promotion",
        desc = "A position where it is ideal to promote to a piece other than the queen (usually a knight)")

    case object Zwischenzug extends Tag(
        name = "Zwischenzug",
        desc = "Instead of playing the expected move (commonly a recapture) first play another move, posing an immediate threat")

    case object Desperado extends Tag(
        name = "Desperado",
        desc = "Use a doomed piece to capture as much material as possible")

    case object Zugzwang extends Tag(
        name = "Zugzwang",
        desc = "A position where it would be ideal to be able to pass the turn")

    val all: List[Tag] = List(Zugzwang, Desperado, Zwischenzug, UnderPromotion, Promotion, BackRank, TrappedPiece,
        ShelterDestruction, SmotheredMate, SmotheredMate, Skewer, Pin, Interference, Overload, Deflection, DiscoveredAttack,
        RemovingDefender, HangingMaterial, Fork)

    val alphabetised: List[Tag] = all.sortWith(_.id < _.id)

    def byId(id: String): Option[Tag] = all.find(_.id == id)

}
