package draughts

import StartingPosition.Category

object TableOfDraw {

  val categories: List[Category] = List(
    Category("I", List(
      StartingPosition("I.1", "B:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1", "1. ab4", "ab4".some),
      StartingPosition("I.2", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,13:H0:F2", "1. ab4 ba5", "ab4 ba5".some),
      StartingPosition("I.3", "W:W17,21,22,23,24,26,27,28,29,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,13:H0:F3", "1. ab4 ba5 2. ba3 ab6", "ab4 ba5 ba3 ab6".some)
    )),
    Category("II", List(
      StartingPosition("II.1", "B:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1", "", "a1-a5".some),
      StartingPosition("II.2", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,20:H0:F1", "", "a1-a5 a7-h4".some),
      StartingPosition("II.3", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,16:H0:F1", "", "a1-a5 b6-g5".some)
    )),
    Category("III", List(
      StartingPosition("III.1", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,14:H0:F2", "1. ab4 bc5", "ab4 bc5".some),
      StartingPosition("III.2", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,17:H0:F3", "1. ab4 bc5 2. ba5 cb4", "ab4 bc5 ba5 cb4".some),
      StartingPosition("III.3", "W:W13,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,15,17:H0:F4", "1. ab4 bc5 2. ba5 cb4 3. ed4 fe5", "ab4 bc5 ba5 cb4 ed4 fe5".some)
    ))
  )

}
