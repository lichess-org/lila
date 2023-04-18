package lila.coordinate

case class Score(
    _id: UserId,
    white: List[Int] = Nil,
    black: List[Int] = Nil,
    whiteNameSquare: List[Int] = Nil,
    blackNameSquare: List[Int] = Nil
)
