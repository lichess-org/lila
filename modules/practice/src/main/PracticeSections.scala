package lila.practice

import lila.core.study.data.StudyName

private object PracticeSections:

  val list = List(
    PracticeSection(
      name = "Checkmates",
      id = "checkmates",
      studies = List(
        study("BJy6fEDf", "Piece Checkmates I", "Basic checkmates"),
        study("fE4k21MW", "Checkmate Patterns I", "Recognize the patterns"),
        study("8yadFPpU", "Checkmate Patterns II", "Recognize the patterns"),
        study("PDkQDt6u", "Checkmate Patterns III", "Recognize the patterns"),
        study("96Lij7wH", "Checkmate Patterns IV", "Recognize the patterns"),
        study("Rg2cMBZ6", "Piece Checkmates II", "Challenging checkmates"),
        study("ByhlXnmM", "Knight & Bishop Mate", "Interactive lesson")
      )
    ),
    PracticeSection(
      name = "Fundamental Tactics",
      id = "fundamental-tactics",
      studies = List(
        study("9ogFv8Ac", "The Pin", "Pin it to win it"),
        study("tuoBxVE5", "The Skewer", "Yum - skewers!"),
        study("Qj281y1p", "The Fork", "Use the fork, Luke"),
        study("MnsJEWnI", "Discovered Attacks", "Including discovered checks"),
        study("RUQASaZm", "Double Check", "A very powerful tactic"),
        study("o734CNqp", "Overloaded Pieces", "They have too much work"),
        study("ITWY4GN2", "Zwischenzug", "In-between moves"),
        study("lyVYjhPG", "X-Ray", "Attacking through an enemy piece")
      )
    ),
    PracticeSection(
      name = "Advanced Tactics",
      id = "advanced-tactics",
      studies = List(
        study("9cKgYrHb", "Zugzwang", "Being forced to move"),
        study("g1fxVZu9", "Interference", "Interpose a piece to great effect"),
        study("s5pLU7Of", "Greek Gift", "Study the greek gift sacrifice"),
        study("kdKpaYLW", "Deflection", "Distracting a defender"),
        study("jOZejFWk", "Attraction", "Lure a piece to a bad square"),
        study("49fDW0wP", "Underpromotion", "Promote - but not to a queen!"),
        study("0YcGiH4Y", "Desperado", "A piece is lost, but it can still help"),
        study("CgjKPvxQ", "Counter Check", "Respond to a check with a check"),
        study("udx042D6", "Undermining", "Remove the defending piece"),
        study("Grmtwuft", "Clearance", "Get out of the way!")
      )
    ),
    PracticeSection(
      name = "Pawn Endgames",
      id = "pawn-endgames",
      studies = List(
        study("xebrDvFe", "Key Squares", "Reach a key square"),
        study("A4ujYOer", "Opposition", "Take the opposition"),
        study("pt20yRkT", "7th-Rank Rook Pawn", "Versus a Queen")
      )
    ),
    PracticeSection(
      name = "Rook Endgames",
      id = "rook-endgames",
      studies = List(
        study("MkDViieT", "7th-Rank Rook Pawn", "And Passive Rook vs Rook"),
        study("pqUSUw8Y", "Basic Rook Endgames", "Lucena and Philidor"),
        study("heQDnvq7", "Intermediate Rook Endings", "Broaden your knowledge"),
        study("wS23j5Tm", "Practical Rook Endings", "Rook endings with several pawns")
      )
    )
  )

  private def study(id: String, name: String, desc: String) =
    PracticeStudy(
      id = StudyId(id),
      name = StudyName(name),
      desc = desc,
      chapters = Nil // Chapters will be filled later
    )
