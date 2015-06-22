package lila.opening

case class Identified(
  name: Identified.Name,
  moves: Identified.Moves)

case object Identified {

  def many(fullNames: List[String], max: Int): List[Identified] =
    fullNames.map {
      _.split(',').take(2).mkString(",")
    }.distinct take max map { name =>
      Identified(
        name = name,
        moves = ~(movesPerName get name))
    }

  type Name = String
  type Moves = String

  private lazy val movesPerName: Map[Name, Moves] =
    chess.Openings.db.foldLeft(Map[Name, Moves]()) {
      case (outerAcc, (_, fullName, moves)) => List(1, 2).foldLeft(outerAcc) {
        case (acc, length) =>
          val name = fullName.split(',').take(length).mkString(",")
          acc get name match {
            case None                             => acc + (name -> moves)
            case Some(ms) if moves.size < ms.size => acc + (name -> moves)
            case _                                => acc
          }
      }
    }
}
