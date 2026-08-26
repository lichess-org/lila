package lila.appeal

case class AnswerBranch(id: String, answer: String, nextNode: AppealNode)

sealed trait AppealNode:
  def id: String

case class UserQuestionNode(id: String, question: String, branches: Vector[AnswerBranch]) extends AppealNode
case class ModQuestionNode(id: String, question: String, branches: Vector[AnswerBranch]) extends AppealNode
case class SystemActionNode(
    id: String,
    text: String,
    sleepMonths: Option[Int] = None,
    closeNow: Boolean = false,
    unmark: Boolean = false
) extends AppealNode

object AppealFlow:

  def nextNode(appeal: Appeal): Option[AppealNode] =
    if appeal.msgs.isEmpty then map(appeal.topic).some else None

  private val map: Map[AppealTopic, AppealNode] = Map(
    AppealTopic.cheat -> {
      val manyInfractions = ModQuestionNode(
        "many-infractions",
        "Does user have many infractions?",
        Vector(
          AnswerBranch(
            "yes",
            "Yes",
            SystemActionNode("wait-6-months", "You must wait 6 months.", sleepMonths = 6.some)
          ),
          AnswerBranch(
            "no",
            "No",
            SystemActionNode(
              "second-chance",
              "You get a second chance, share new username."
            )
          )
        )
      )
      UserQuestionNode(
        "accept-cheat-mark",
        "Do you accept this cheat mark?",
        Vector(
          AnswerBranch("yes", "Yes", manyInfractions),
          AnswerBranch(
            "no",
            "No",
            ModQuestionNode(
              "is-mark-valid",
              "Is mark valid?",
              Vector(
                AnswerBranch(
                  "yes",
                  "Yes",
                  UserQuestionNode(
                    "mark-is-valid",
                    "We have determined the mark is valid.",
                    Vector(
                      AnswerBranch("regret-mistake", "I regret my mistake.", manyInfractions),
                      AnswerBranch(
                        "disagree-with-outcome",
                        "I disagree with the outcome.",
                        SystemActionNode("decision-final", "Decision is final.", closeNow = true)
                      )
                    )
                  )
                ),
                AnswerBranch(
                  "no",
                  "No",
                  SystemActionNode(
                    "false-positive",
                    "This was a false positive.",
                    closeNow = true,
                    unmark = true
                  )
                )
              )
            )
          )
        )
      )
    }
  )
