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
    chess.OpeningDB.db.foldLeft(Map[Name, Moves]()) {
      case (outerAcc, opening) => List(1, 2).foldLeft(outerAcc) {
        case (acc, length) =>
          val name = opening.fullName.split(',').take(length).mkString(",")
          acc get name match {
            case None                               => acc + (name -> opening.moves)
            case Some(ms) if opening.size < ms.size => acc + (name -> opening.moves)
            case _                                  => acc
          }
      }
    }
}
