package lila.appeal

opaque type NodeId = String
object NodeId extends OpaqueString[NodeId]
opaque type AnswerId = String
object AnswerId extends OpaqueString[AnswerId]

enum Answerer:
  case User, Mod

enum AppealEffect:
  case Sleep(months: Int)
  case Close
  case Unmark

sealed trait AppealNode:
  def id: NodeId

case class ChoiceNode(
    id: NodeId,
    answerer: Answerer,
    question: String,
    branches: NonEmptyList[AnswerBranch]
) extends AppealNode:
  def hasAnswer(answerId: AnswerId) = branches.exists(_.id == answerId)
  def getAnswerBranch(answerId: AnswerId) = branches.find(_.id == answerId)
case class ActionNode(
    id: NodeId,
    text: String,
    effects: Option[List[AppealEffect]] = None
) extends AppealNode

case class AnswerBranch(id: AnswerId, answer: String, nextNodeId: NodeId)

case class AppealFlow(rootId: NodeId, nodes: Map[NodeId, AppealNode]):
  def root: AppealNode = nodes(rootId)

object AppealFlow:

  private def make(appealNodes: NonEmptyList[AppealNode]) =
    AppealFlow(appealNodes.head.id, appealNodes.toList.mapBy(_.id))

  def nextNode(appeal: Appeal): Option[AppealNode] =
    appeal.msgs.isEmpty.so:
      appealFlows.get(appeal.topic).map(_.root)

  private val appealFlows: Map[AppealTopic, AppealFlow] = Map(
    AppealTopic.cheat -> AppealFlow.make(
      NonEmptyList.of(
        ChoiceNode(
          NodeId("accept-cheat-mark"),
          Answerer.User,
          "Do you accept this cheat mark?",
          NonEmptyList.of(
            AnswerBranch(AnswerId("yes"), "Yes", NodeId("many-infractions")),
            AnswerBranch(AnswerId("no"), "No", NodeId("is-mark-valid"))
          )
        ),
        ChoiceNode(
          NodeId("many-infractions"),
          Answerer.Mod,
          "Does user have many infractions?",
          NonEmptyList.of(
            AnswerBranch(AnswerId("yes"), "Yes", NodeId("wait-6-months")),
            AnswerBranch(AnswerId("no"), "No", NodeId("second-chance"))
          )
        ),
        ChoiceNode(
          NodeId("is-mark-valid"),
          Answerer.Mod,
          "Is mark valid?",
          NonEmptyList.of(
            AnswerBranch(AnswerId("yes"), "Yes", NodeId("mark-is-valid")),
            AnswerBranch(AnswerId("no"), "No", NodeId("false-positive"))
          )
        ),
        ChoiceNode(
          NodeId("mark-is-valid"),
          Answerer.User,
          "We have determined the mark is valid.",
          NonEmptyList.of(
            AnswerBranch(AnswerId("regret-mistake"), "I regret my mistake.", NodeId("many-infractions")),
            AnswerBranch(
              AnswerId("disagree-with-outcome"),
              "I disagree with the outcome.",
              NodeId("decision-final")
            )
          )
        ),
        ActionNode(NodeId("wait-6-months"), "You must wait 6 months.", List(AppealEffect.Sleep(6)).some),
        ActionNode(NodeId("second-chance"), "You get a second chance, share new username."),
        ActionNode(NodeId("decision-final"), "Decision is final.", List(AppealEffect.Close).some),
        ActionNode(
          NodeId("false-positive"),
          "This was a false positive.",
          List(AppealEffect.Unmark, AppealEffect.Close).some
        )
      )
    )
  )
