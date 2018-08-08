package lidraughts.puzzle

import draughts.Color
import draughts.format.Forsyth
import play.api.libs.json._

private case class Generated(
    category: String,
    last_pos: String,
    last_move: String,
    move_list: JsArray,
    game_id: String
) {

  def colorFromFen = (Forsyth << last_pos).fold(Color.white)(!_.color)

  def toPuzzle: PuzzleId => Puzzle = Puzzle.make(
    gameId = game_id,
    history = List(last_move),
    fen = last_pos,
    color = colorFromFen,
    lines = Generated readVariations move_list.as[List[List[String]]],
    mate = category == "Mate"
  )
}

private object Generated {

  def readVariations(variations: List[List[String]]): Lines = variations.foldLeft(List[List[String]]()) {
    (unique, variation) =>
      variation.headOption match {
        case Some(move) if !unique.exists(_.head == move) => variation :: unique
        case _ => unique
      }
  } map {
    case Nil => Win("")
    case move :: Nil => Win(move)
    case move :: more => Node(
      move,
      readVariations(variations filter {
        variation => variation.headOption.fold(false)(_ == move)
      } map { _ drop 1 })
    )
  }

  def readLines(moves: List[String]): Lines = moves match {
    case Nil => Nil
    case move :: Nil => List(Win(move))
    case move :: more => List(Node(move, readLines(more)))
  }

  implicit val generatedJSONRead = Json.reads[Generated]
}
