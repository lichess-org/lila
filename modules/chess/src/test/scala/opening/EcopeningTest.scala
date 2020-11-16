// package chess
// package opening

// import org.specs2.matcher.Matchers
// import org.specs2.mutable.Specification

// class EcopeningTest extends Specification with Matchers {
//   "\"codeFamily\" index" should {
//     "correctly map a code to its respective family" in {
//       Openings.codeFamily must havePair("E50" -> "Nimzo-Indian Defence")
//     }
//     "for a given code, select the family with the highest cardinality" in {
//       Openings.codeFamily must havePair("D02" -> "Queen's Pawn Game")
//     }
//   }

//   "\"familyFirstMove\" index" should {
//     "for a given family, select the first available opening" in {
//       Openings.familyFirstMove must havePair("Benko Gambit" -> "d4")
//     }
//     "return the first move for a selected opening" in {
//       Openings.familyFirstMove must havePair("Czech Defence" -> "e4")
//     }
//   }

//   "\"familyMoveList\" index" should {
//     "for a given family, select the opening with the shortest move list" in {
//       Openings.familyMoveList must havePair("Benko Gambit" -> "d4 Nf6 c4 c5 d5 b5".split(' ').toList)
//     }
//     "return all moves for the selected opening" in {
//       Openings.familyMoveList must havePair("Czech Defence" -> "e4 d6 d4 Nf6 Nc3 c6".split(' ').toList)
//     }
//   }

//   "\"generals\" index" should {
//     "return a list of tuples sorted by the opening code (first member) in lexicographic order" in {
//       Openings.generals.map(_._1) must beSorted
//     }
//     "for a given code, select the first general opening" in {
//       Openings.generals must havePair("A00" -> "Anderssen Opening, General")
//     }
//     "for a given code, select the opening with the shortest move list where a general opening is not available" in {
//       Openings.generals must havePair("C94" -> "Spanish Game, Morphy Defence, Breyer Defence")
//     }
//   }
// }
