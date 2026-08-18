package lila.appeal

sealed trait AppealNode

case class UserChoiceNode(question: String, branches: Vector[(String, AppealNode)]) extends AppealNode
case class ModChoiceNode(question: String, branches: Vector[(String, AppealNode)]) extends AppealNode
case class SystemActionNode(
    text: String,
    sleepMonths: Option[Int] = None,
    closeNow: Boolean = false,
    unmark: Boolean = false
) extends AppealNode

object AppealFlowApi:

  def nextNode(appeal: Appeal): Option[AppealNode] =
    if appeal.msgs.isEmpty then map(appeal.topic).some else None

  private val map: Map[AppealTopic, AppealNode] = Map(
    AppealTopic.cheat -> {
      val manyInfractions = ModChoiceNode(
        "Does user have many infractions?",
        Vector(
          ("Yes", SystemActionNode("You must wait 6 months.", sleepMonths = 6.some)),
          ("No", SystemActionNode("You get a second chance, share new username."))
        )
      )
      UserChoiceNode(
        "Do you accept this cheat mark?",
        Vector(
          ("Yes", manyInfractions),
          (
            "No",
            ModChoiceNode(
              "Is mark valid?",
              Vector(
                (
                  "Yes",
                  UserChoiceNode(
                    "We have determined the mark is valid.",
                    Vector(
                      ("I regret my mistake.", manyInfractions),
                      ("I disagree with the outcome.", SystemActionNode("Decision is final.", closeNow = true))
                    )
                  )
                ),
                ("No", SystemActionNode("This was a false positive.", closeNow = true, unmark = true))
              )
            )
          )
        )
      )
    }
  )

