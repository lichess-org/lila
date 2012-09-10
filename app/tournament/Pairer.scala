package lila
package tournament

object Pairer {

  def apply(tour: Started): List[Pairing] = 
    tour.pairings.fold(List[Pairing]()) {
      case (existing, pairings) if existing.status >= chess.Status.Mate =>
          
      case _ => pairings
    }

  private def idlePlayers(pairings: List[Pairings])
}
