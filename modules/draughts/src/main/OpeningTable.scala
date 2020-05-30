package draughts

case class OpeningTable(key: String, name: String, url: String, positions: List[StartingPosition]) {

  lazy val shuffled = new scala.util.Random(475592).shuffle(positions).toIndexedSeq

  def randomOpening: StartingPosition = shuffled(scala.util.Random.nextInt(shuffled.size))
}

object OpeningTable {

  import StartingPosition.Category

  val categoriesIDF = List(
    Category("I", List(
      StartingPosition("I.1", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1", "", "ab4".some),
      StartingPosition("I.2", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,13:H0:F2", "1. ab4 ba5", "ab4 ba5".some),
      StartingPosition("I.3", "W:W17,21,22,23,24,26,27,28,29,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,13:H0:F3", "1. ab4 ba5 2. ba3 ab6", "ab4 ba5 ba3 ab6".some),
      StartingPosition("I.4", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,11,12,13,14:H0:F4", "1. ab4 ba5 2. ba3 ab6 3. ab2 dc5", "ab4 ba5 ba3 ab6 ab2 dc5".some),
      StartingPosition("I.5", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,11,12,13,15:H0:F4", "1. ab4 ba5 2. ba3 ab6 3. ab2 de5", "ab4 ba5 ba3 ab6 ab2 de5".some),
      StartingPosition("I.6", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,7,8,10,11,12,13,14:H0:F4", "1. ab4 ba5 2. ba3 cb6 3. ab2 bc5", "ab4 ba5 ba3 cb6 ab2 bc5".some),
      StartingPosition("I.7", "W:W17,21,22,23,24,25,26,27,28,29,31,32:B1,2,3,4,5,7,8,9,11,12,13,15:H0:F4", "1. ab4 ba5 2. ba3 cb6 3. cb2 de5", "ab4 ba5 ba3 cb6 cb2 de5".some),
      StartingPosition("I.8", "B:W17,19,21,22,23,26,27,28,29,30,31,32:B1,2,3,4,5,7,8,9,10,11,12,13:H0:F3", "1. ab4 ba5 2. ba3 cb6 3. gf4", "ab4 ba5 ba3 cb6 gf4".some),
      StartingPosition("I.9", "W:W17,20,21,22,23,26,27,28,29,30,31,32:B1,2,3,4,5,7,8,9,10,12,13,15:H0:F4", "1. ab4 ba5 2. ba3 cb6 3. gh4 fe5", "ab4 ba5 ba3 cb6 gh4 fe5".some),
      StartingPosition("I.10", "W:W17,21,22,23,24,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,11,12,13,15:H0:F3", "1. ab4 ba5 2. ba3 de5", "ab4 ba5 ba3 de5".some),
      StartingPosition("I.11", "B:W17,20,21,22,23,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,13,15:H0:F3", "1. ab4 ba5 2. ba3 fe5 3. gh4", "ab4 ba5 ba3 fe5 gh4".some),
      StartingPosition("I.12", "W:W17,18,22,23,24,25,26,28,29,30,31,32:B2,3,4,5,6,7,8,9,10,11,12,13:H0:F4", "1. ab4 ba5 2. ed4 ab6 3. fe3 ba7", "ab4 ba5 ed4 ab6 fe3 ba7".some),
      StartingPosition("I.13", "W:W17,18,22,23,24,25,26,28,29,30,31,32:B1,2,3,4,6,7,8,9,11,12,13,14:H0:F4", "1. ab4 ba5 2. ed4 ab6 3. fe3 dc5", "ab4 ba5 ed4 ab6 fe3 dc5".some),
      StartingPosition("I.14", "W:W17,18,22,23,24,25,26,28,29,30,31,32:B1,2,3,4,6,7,8,9,10,12,13,15:H0:F4", "1. ab4 ba5 2. ed4 ab6 3. fe3 fe5", "ab4 ba5 ed4 ab6 fe3 fe5".some),
      StartingPosition("I.15", "W:W17,18,22,23,24,25,26,28,29,30,31,32:B1,2,3,4,6,7,8,9,10,11,13,16:H0:F4", "1. ab4 ba5 2. ed4 ab6 3. fe3 hg5", "ab4 ba5 ed4 ab6 fe3 hg5".some),
      StartingPosition("I.16", "W:W17,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,11,12,13,14:H0:F3", "1. ab4 ba5 2. ed4 dc5", "ab4 ba5 ed4 dc5".some),
      StartingPosition("I.17", "W:W18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,7,8,11,12,13,15:H0:F4", "1. ab4 ba5 2. ed4 dc5 3. bd6 ce5", "ab4 ba5 ed4 dc5 bd6 ce5".some),
      StartingPosition("I.18", "W:W17,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,10,12,13,15:H0:F4", "1. ab4 ba5 2. ed4 fe5 3. df6 ge5", "ab4 ba5 ed4 fe5 df6 ge5".some),
      StartingPosition("I.19", "W:W17,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,8,10,12,13,16:H0:F4", "1. ab4 ba5 2. ed4 fe5 3. df6 eg5", "ab4 ba5 ed4 fe5 df6 eg5".some),
      StartingPosition("I.20", "W:W17,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,13,16:H0:F3", "1. ab4 ba5 2. ed4 fg5", "ab4 ba5 ed4 fg5".some),
      StartingPosition("I.21", "W:W17,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,13,16:H0:F3", "1. ab4 ba5 2. ed4 hg5", "ab4 ba5 ed4 hg5".some),
      StartingPosition("I.22", "B:W17,20,22,23,24,25,26,27,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,13,15:H0:F3", "1. ab4 ba5 2. gh4 fe5 3. hg3", "ab4 ba5 gh4 fe5 hg3".some),
      StartingPosition("I.23", "W:W17,20,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,13,16:H0:F3", "1. ab4 ba5 2. gh4 fg5", "ab4 ba5 gh4 fg5".some)
    )),
    Category("II", List(
      StartingPosition("II.1", "B:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1", "", "a1-a5".some),
      StartingPosition("II.2", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,20:H0:F1", "", "a1-a5 a7-h4".some),
      StartingPosition("II.3", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,16:H0:F1", "", "a1-a5 b6-g5".some),
      StartingPosition("II.4", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,20:H0:F1", "", "a1-a5 b6-h4".some),
      StartingPosition("II.5", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,20:H0:F1", "", "a1-a5 f6-h4".some),
      StartingPosition("II.6", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,15:H0:F1", "", "a1-a5 f6-h4".some),
      StartingPosition("II.7", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,16:H0:F1", "", "a1-a5 g7-g5".some),
      StartingPosition("II.8", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,20:H0:F1", "", "a1-a5 g7-h4".some),
      StartingPosition("II.9", "W:W13,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,17:H0:F1", "", "a1-a5 h6-b4".some),
      StartingPosition("II.10", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,13:H0:F1", "", "a1-b4 a7-a5".some),
      StartingPosition("II.11", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,16:H0:F1", "", "a1-b4 a7-g5".some),
      StartingPosition("II.12", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,13:H0:F1", "", "a1-b4 b6-a5".some),
      StartingPosition("II.13", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,16:H0:F1", "", "a1-b4 d6-g5".some),
      StartingPosition("II.14", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,5,6,7,8,9,10,11,12,15:H0:F1", "", "a1-b4 h8-e5".some),
      StartingPosition("II.15", "W:W17,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,16:H0:F1", "", "a1-b4 h6-g5".some),
      StartingPosition("II.16", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,14:H0:F1", "", "a1-d4 a7-c5".some),
      StartingPosition("II.17", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,19:H0:F1", "", "a1-d4 a7-f4".some),
      StartingPosition("II.18", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,13:H0:F1", "", "a1-d4 b6-a5".some),
      StartingPosition("II.19", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,14:H0:F1", "", "a1-d4 b6-c5".some),
      StartingPosition("II.20", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,15:H0:F1", "", "a1-d4 f6-e5".some),
      StartingPosition("II.21", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,5,6,7,8,9,10,11,12,15:H0:F1", "", "a1-d4 h8-e5".some),
      StartingPosition("II.22", "W:W18,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,16:H0:F1", "", "a1-d4 h6-g5".some),
      StartingPosition("II.23", "W:W14,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,16:H0:F1", "", "a1-c5 g7-g5".some)
    )),
    Category("III", List(
      StartingPosition("III.1", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,14:H0:F2", "1. ab4 bc5", "ab4 bc5".some),
      StartingPosition("III.2", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,17:H0:F3", "1. ab4 bc5 2. ba5 cb4", "ab4 bc5 ba5 cb4".some),
      StartingPosition("III.3", "W:W13,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,15,17:H0:F4", "1. ab4 bc5 2. ba5 cb4 3. ed4 fe5", "ab4 bc5 ba5 cb4 ed4 fe5".some),
      StartingPosition("III.4", "W:W13,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,16,17:H0:F4", "1. ab4 bc5 2. ba5 cb4 3. ed4 fg5", "ab4 bc5 ba5 cb4 ed4 fg5".some),
      StartingPosition("III.5", "W:W13,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,16,17:H0:F4", "1. ab4 bc5 2. ba5 cb4 3. ed4 hg5", "ab4 bc5 ba5 cb4 ed4 hg5".some),
      StartingPosition("III.6", "W:W13,19,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,15,17:H0:F4", "1. ab4 bc5 2. ba5 cb4 3. gf4 fe5", "ab4 bc5 ba5 cb4 gf4 fe5".some),
      StartingPosition("III.7", "B:W13,20,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,17:H0:F3", "1. ab4 bc5 2. ba5 cb4 3. gh4", "ab4 bc5 ba5 cb4 gh4".some),
      StartingPosition("III.8", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,12,14,16:H0:F3", "1. ab4 bc5 2. ba5 fg5", "ab4 bc5 ba5 fg5".some),
      StartingPosition("III.9", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,14:H0:F2", "1. ab4 dc5", "ab4 dc5".some),
      StartingPosition("III.10", "B:W20,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,8,9,11,12,14:H0:F3", "1. ab4 dc5 2. bd6 ec5 3. gh4", "ab4 dc5 bd6 ec5 gh4".some),
      StartingPosition("III.11", "B:W19,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,7,8,9,11,12,15:H0:F3", "1. ab4 dc5 2. bd6 ce5 3. ef4", "ab4 dc5 bd6 ce5 ef4".some),
      StartingPosition("III.12", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,15:H0:F2", "1. ab4 de5", "ab4 de5".some),
      StartingPosition("III.13", "B:W13,19,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,12,15,16:H0:F3", "1. ab4 de5 2. ba5 fg5 3. ef4", "ab4 de5 ba5 fg5 ef4".some),
      StartingPosition("III.14", "B:W13,20,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,12,15,16:H0:F3", "1. ab4 de5 2. ba5 fg5 3. gh4", "ab4 de5 ba5 fg5 gh4".some),
      StartingPosition("III.15", "W:W17,21,22,23,24,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,12,15,16:H0:F3", "1. ab4 de5 2. ba3 fg5", "ab4 de5 ba3 fg5".some),
      StartingPosition("III.16", "B:W17,19,21,22,23,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,12,15,16:H0:F3", "1. ab4 de5 2. ba3 fg5 3. gf4", "ab4 de5 ba3 fg5 gf4".some),
      StartingPosition("III.17", "B:W13,18,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,11,15,16:H0:F3", "1. ab4 de5 2. ed4 hg5 3. ba5", "ab4 de5 ed4 hg5 ba5".some),
      StartingPosition("III.18", "B:W17,20,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,15:H0:F2", "1. ab4 de5 2. gh4", "ab4 de5 gh4".some),
      StartingPosition("III.19", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,15:H0:F2", "1. ab4 fe5", "ab4 fe5".some),
      StartingPosition("III.20", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,19:H0:F3", "1. ab4 fe5 2. ba5 ef4", "ab4 fe5 ba5 ef4".some),
      StartingPosition("III.21", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,15:H0:F3", "1. ab4 fe5 2. ba5 gf6", "ab4 fe5 ba5 gf6".some),
      StartingPosition("III.22", "B:W17,19,22,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,15:H0:F2", "1. ab4 fe5 2. ef4", "ab4 fe5 ef4".some),
      StartingPosition("III.23", "W:W17,20,22,23,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,15:H0:F3", "1. ab4 fe5 2. gh4 gf6", "ab4 fe5 gh4 gf6".some)
    )),
    Category("IV", List(
      StartingPosition("IV.1", "W:W15,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,16:H0:F1", "", "a1-e5 d6-g5".some),
      StartingPosition("IV.2", "W:W15,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,9,10,11,12,20:H0:F1", "", "a1-e5 g7-h4".some),
      StartingPosition("IV.3", "W:W19,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,14:H0:F1", "", "a1-f4 a7-c5".some),
      StartingPosition("IV.4", "W:W19,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,14:H0:F1", "", "a1-f4 b6-c5".some),
      StartingPosition("IV.5", "W:W19,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,7,8,9,10,11,12,13:H0:F1", "", "a1-f4 c7-a5".some),
      StartingPosition("IV.6", "W:W19,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,14:H0:F1", "", "a1-f4 d6-c5".some),
      StartingPosition("IV.7", "W:W19,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,8,9,10,11,12,15:H0:F1", "", "a1-f4 e7-e5".some),
      StartingPosition("IV.8", "W:W19,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,15:H0:F1", "", "a1-f4 f6-e5".some),
      StartingPosition("IV.9", "W:W16,21,22,23,24,25,26,27,28,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,15:H0:F1", "", "a1-g5 d6-e5".some),
      StartingPosition("IV.10", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,6,7,8,9,10,11,12,21:H0:F1", "", "a3-a5 a7-a3".some),
      StartingPosition("IV.11", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,21:H0:F1", "", "a3-a5 b6-a3".some),
      StartingPosition("IV.12", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,17:H0:F1", "", "a3-a5 d6-b4".some),
      StartingPosition("IV.13", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,12,21:H0:F1", "", "a3-a5 f6-a3".some),
      StartingPosition("IV.14", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,21:H0:F1", "", "a3-a5 h6-a3".some),
      StartingPosition("IV.15", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,17:H0:F1", "", "a3-a5 h6-b4".some),
      StartingPosition("IV.16", "W:W13,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,15:H0:F1", "", "a3-a5 h6-e5".some),
      StartingPosition("IV.17", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,21:H0:F1", "", "a3-b4 b6-a3".some),
      StartingPosition("IV.18", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,11,12,21:H0:F1", "", "a3-b4 d6-a3".some),
      StartingPosition("IV.19", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,8,9,10,11,12,13:H0:F1", "", "a3-b4 e7-a5".some),
      StartingPosition("IV.20", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,21:H0:F1", "", "a3-b4 h6-a3".some),
      StartingPosition("IV.21", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,15:H0:F1", "", "a3-b4 h6-e5".some),
      StartingPosition("IV.22", "W:W17,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,5,6,7,8,9,10,11,12,21:H0:F1", "", "a3-b4 h8-a3".some),
      StartingPosition("IV.23", "W:W14,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,10,11,12,21:H0:F1", "", "a3-c5 b6-a3".some)
    ))
  )

  val tableIDF = OpeningTable(
    key = "idf",
    name = "IDF competitions Table of draw",
    url = "https://idf64.org/tables-of-draw/",
    positions = categoriesIDF.flatMap(_.positions)
  )

  val allTables = List(tableIDF)

  def byKey = key2table.get _
  private val key2table: Map[String, OpeningTable] = allTables.map { p =>
    p.key -> p
  }(scala.collection.breakOut)

}
