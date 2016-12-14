package lila.puzzle

sealed abstract class Tag(
    val id: Tag.id,
    val name: Tag.name,
    val desc: Tag.desc)

object Tag {

    case object Fork extends Tag(
        id = "fork",
        name = "Fork/Double Attack",
        desc = "Attack two pieces at once")

    case object HangingMaterial extends Tag(
        id = "hanging",
        name = "Hanging Material",
        desc = "An undefended piece that can be taken without potential recapture")
    
    case object RemovingDefender extends Tag(
        id = "removedefender",
        name = "Removing the Defender",
        desc = "Remove the defender of a valuable piece or a mating square")

    case object DiscoveredAttack extends Tag(
        id = "discovered",
        name = "Discovered Attack",
        desc = "Create an attack with a piece by moving another piece out of the line of sight")

    case object Deflection extends Tag(
        id = "deflection",
        name = "Deflection",
        desc = "Force your opponent to move a piece enabling you to create further attacks")
    
    case object Overload extends Tag(
        id = "overload",
        name = "Overloading",
        desc = "Exploit a piece that has been given too many defensive roles")
    
    case object Interference extends Tag(
        id = "interference",
        name = "Interference",
        desc = "Thwart your opponent's attack or defense usually by sacrificing a piece")
    
    case object Pin extends Tag(
        id = "pin",
        name = "Exploiting a Pin",
        desc = "Exploit a piece that cannot move due to being pinned (usually against the king)")

    case object Skewer extends Tag(
        id = "skewer",
        name = "Skewer",
        desc = "Similar to a pin, but usually with the king infront of a hanging piece")
    
    case object SmotheredMate extends Tag(
        id = "smothered",
        name = "Smothered Mate",
        desc = "Checkmate the king with only a knight. All other squares are occupied by the opponent's pieces")

    case object ShelterDestruction extends Tag(
        id = "destruction",
        name = "Destruction of the King's Shelter",
        desc = "Destroy the pawn structure around the king enabling further attacks against the king")

    case object TrappedPiece extends Tag(
        id = "trapped",
        name = "Trapped Piece/Mobility",
        desc = "A piece that can move, however any legal move will cause that piece to be captured")

    case object BackRank extends Tag(
        id = "backrank",
        name = "Weak Back Rank",
        desc = "The first rank of your opponent can be infiltrated (usually by a queen or a rook) unless defended")

    case object Promotion extends Tag(
        id = "promotion",
        name = "Promotion",
        desc = "Reach the back rank with a pawn to promote it to a queen")

    case object UnderPromotion extends Tag(
        id = "underpromotion",
        name = "Under Promotion",
        desc = "A position where it is ideal to promote to a piece other than the queen (usually a knight)")

    case object Zwischenzug extends Tag(
        id = "zwischenzug",
        name = "Zwischenzug",
        desc = "Instead of playing the expected move (commonly a recapture) first play another move, posing an immediate threat")

    case object Desperado extends Tag(
        id = "desperado",
        name = "Desperado",
        desc = "Use a doomed piece to capture as much material as possible")

    case object Zugszwang extends Tag(
        id = "zugzwang",
        name = "Zugzwang",
        desc = "A position where it would be ideal to be able to pass the turn")

    val all: List[Tag] = List(Zugzwang, Desperado, Zwischenzug, UnderPromotion, Promotion, BackRank, TrappedPiece,
        ShelterDestruction, SmotheredMate, SmotheredMate, Skewer, Pin, Interference, Overload, Deflection, DiscoveredAttack,
        RemovingDefender, HangingMaterial, Fork)

    val alphabetised: List[Tag] = all.sortWith(_.id < _.id)

}