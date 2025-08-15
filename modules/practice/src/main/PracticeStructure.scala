package lila.practice

import lila.core.study.data.StudyName
import lila.study.Chapter

case class PracticeStructure(sections: List[PracticeSection]):

  def study(id: StudyId): Option[PracticeStudy] =
    sections.flatMap(_.study(id)).headOption

  lazy val studiesByIds: Map[StudyId, PracticeStudy] =
    sections.view
      .flatMap(_.studies)
      .mapBy(_.id)

  lazy val sectionsByStudyIds: Map[StudyId, PracticeSection] =
    sections.view.flatMap { sec =>
      sec.studies.map { stu =>
        stu.id -> sec
      }
    }.toMap

  lazy val chapterIds: List[StudyChapterId] = sections.flatMap(_.studies).flatMap(_.chapterIds)

  lazy val nbChapters = sections.flatMap(_.studies).map(_.chapterIds.size).sum

  def findSection(id: StudyId): Option[PracticeSection] = sectionsByStudyIds.get(id)

case class PracticeSection(
    id: String,
    name: String,
    studies: List[PracticeStudy]
):
  lazy val studiesByIds: Map[StudyId, PracticeStudy] = studies.mapBy(_.id)

  def study(id: StudyId): Option[PracticeStudy] = studiesByIds.get(id)

case class PracticeStudy(
    id: StudyId,
    name: StudyName,
    desc: String,
    chapters: List[Chapter.IdName]
) extends lila.core.practice.Study:
  val slug = scalalib.StringOps.slug(name.value)
  def chapterIds = chapters.map(_.id)

object PracticeStructure:

  private[practice] val totalChapters = 233

  private def makeStudy(id: String, name: String, desc: String) =
    PracticeStudy(
      id = StudyId(id),
      name = StudyName(name),
      desc = desc,
      chapters = Nil // Chapters will be filled later
    )

  private[practice] val sections = List(
    PracticeSection(
      name = "Checkmates",
      id = "checkmates",
      studies = List(
        makeStudy("BJy6fEDf", "Piece Checkmates I", "Basic checkmates"),
        makeStudy("fE4k21MW", "Checkmate Patterns I", "Recognize the patterns"),
        makeStudy("8yadFPpU", "Checkmate Patterns II", "Recognize the patterns"),
        makeStudy("PDkQDt6u", "Checkmate Patterns III", "Recognize the patterns"),
        makeStudy("96Lij7wH", "Checkmate Patterns IV", "Recognize the patterns"),
        makeStudy("Rg2cMBZ6", "Piece Checkmates II", "Challenging checkmates"),
        makeStudy("ByhlXnmM", "Knight & Bishop Mate", "Interactive lesson")
      )
    ),
    PracticeSection(
      name = "Fundamental Tactics",
      id = "fundamental-tactics",
      studies = List(
        makeStudy("9ogFv8Ac", "The Pin", "Pin it to win it"),
        makeStudy("tuoBxVE5", "The Skewer", "Yum - skewers!"),
        makeStudy("Qj281y1p", "The Fork", "Use the fork, Luke"),
        makeStudy("MnsJEWnI", "Discovered Attacks", "Including discovered checks"),
        makeStudy("RUQASaZm", "Double Check", "A very powerful tactic"),
        makeStudy("o734CNqp", "Overloaded Pieces", "They have too much work"),
        makeStudy("ITWY4GN2", "Zwischenzug", "In-between moves"),
        makeStudy("lyVYjhPG", "X-Ray", "Attacking through an enemy piece")
      )
    ),
    PracticeSection(
      name = "Advanced Tactics",
      id = "advanced-tactics",
      studies = List(
        makeStudy("9cKgYrHb", "Zugzwang", "Being forced to move"),
        makeStudy("g1fxVZu9", "Interference", "Interpose a piece to great effect"),
        makeStudy("s5pLU7Of", "Greek Gift", "Study the greek gift sacrifice"),
        makeStudy("kdKpaYLW", "Deflection", "Distracting a defender"),
        makeStudy("jOZejFWk", "Attraction", "Lure a piece to a bad square"),
        makeStudy("49fDW0wP", "Underpromotion", "Promote - but not to a queen!"),
        makeStudy("0YcGiH4Y", "Desperado", "A piece is lost, but it can still help"),
        makeStudy("CgjKPvxQ", "Counter Check", "Respond to a check with a check"),
        makeStudy("udx042D6", "Undermining", "Remove the defending piece"),
        makeStudy("Grmtwuft", "Clearance", "Get out of the way!")
      )
    ),
    PracticeSection(
      name = "Pawn Endgames",
      id = "pawn-endgames",
      studies = List(
        makeStudy("xebrDvFe", "Key Squares", "Reach a key square"),
        makeStudy("A4ujYOer", "Opposition", "Take the opposition"),
        makeStudy("pt20yRkT", "7th-Rank Rook Pawn", "Versus a Queen")
      )
    ),
    PracticeSection(
      name = "Rook Endgames",
      id = "rook-endgames",
      studies = List(
        makeStudy("MkDViieT", "7th-Rank Rook Pawn", "And Passive Rook vs Rook"),
        makeStudy("pqUSUw8Y", "Basic Rook Endgames", "Lucena and Philidor"),
        makeStudy("heQDnvq7", "Intermediate Rook Endings", "Broaden your knowledge"),
        makeStudy("wS23j5Tm", "Practical Rook Endings", "Rook endings with several pawns")
      )
    )
  )

  private[practice] def studyIds: List[StudyId] = sections.flatMap(_.studies.map(_.id))

  def withChapters(chapters: Map[StudyId, Vector[Chapter.IdName]]) = PracticeStructure:
    sections.map: sec =>
      sec.copy(
        studies = sec.studies.map: stu =>
          stu.copy(chapters = chapters.get(stu.id).so(_.toList))
      )
