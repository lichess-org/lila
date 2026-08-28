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

  def findNodeById(topic: AppealTopic, id: NodeId): Option[AppealNode] = appealFlows
    .get(topic)
    .flatMap(_.nodes.get(id))

  def nextNode(appeal: Appeal): Option[AppealNode] =
    if appeal.msgs.isEmpty then appealFlows.get(appeal.topic).map(_.root)
    else
      appeal.msgs.lastOption.so:
        case event: ChoiceEvent =>
          appealFlows
            .get(appeal.topic)
            .flatMap: flow =>
              flow.nodes
                .get(event.nodeId)
                .so:
                  case cn: ChoiceNode =>
                    cn.branches
                      .find(_.id == event.answerId)
                      .flatMap: branch =>
                        flow.nodes.get(branch.nextNodeId)
                  case _ => none
        case _ => none

  private def make(appealNodes: NonEmptyList[AppealNode]) =
    AppealFlow(appealNodes.head.id, appealNodes.toList.mapBy(_.id))

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
        ActionNode(NodeId("second-chance"), "You get a second chance. Please share your new username."),
        ActionNode(
          NodeId("decision-final"),
          "We regret to inform you that the decision is final and will not be changed.",
          List(AppealEffect.Close).some
        ),
        ActionNode(
          NodeId("false-positive"),
          "This was a false positive. Your account has been unmarked.",
          List(AppealEffect.Unmark, AppealEffect.Close).some
        )
      )
    )
  )
