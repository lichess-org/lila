package lila.practice

case class PracticeStructure(
  sections: List[PracticeSection])

object PracticeStructure {
  val empty = PracticeStructure(Nil)
}

case class PracticeSection(
  id: String,
  name: String,
  studies: List[PracticeStudy])

case class PracticeStudy(
  id: String, // study ID
  name: String,
  desc: String)
