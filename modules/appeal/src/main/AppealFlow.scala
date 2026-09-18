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
    effects: List[AppealEffect] = Nil
) extends AppealNode

case class AnswerBranch(id: AnswerId, answer: String, nextNodeId: NodeId)

case class AppealFlow(rootId: NodeId, nodes: Map[NodeId, AppealNode]):
  def root: AppealNode = nodes(rootId)

object AppealFlow:

  def rootNodeAnswerer(topic: AppealTopic) =
    appealFlows.get(topic).map(_.root).collect { case cn: ChoiceNode => cn.answerer }

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
          """Your account is banned for engine/computer assistance.

Did you ever get help from a chess engine during a game — yes or no?""",
          NonEmptyList.of(
            AnswerBranch(
              AnswerId("yes"),
              "Yes, I used external assistance in my games.",
              NodeId("many-infractions")
            ),
            AnswerBranch(
              AnswerId("no"),
              "No, I deny having used external assistance in my games.",
              NodeId("is-mark-valid")
            )
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
        ActionNode(
          NodeId("wait-6-months"),
          """At least two accounts controlled by you or related to this account have broken our fair play rules (https://lichess.org/terms-of-service).

Please appeal again in 6 months, as we will not give another chance before then. Do not create any new accounts until you appeal again.

We do not remove restrictions from accounts marked correctly for external assistance. Hence, following a review after the waiting period, we may allow you a separate account to resume playing without restrictions.""",
          List(AppealEffect.Sleep(6))
        ),
        ActionNode(
          NodeId("second-chance"),
          """As per our policy, we cannot remove the ban from this account, but we will let you make another (final) account where you can play rated games. You will need to register the new account with a different email address.

We trust that you understand why this account was banned, and that in the future you will comply with our fair play rules (lichess.org/terms-of-service).

Please tell us the name of your new account after you have created it by replying to this message. Otherwise, your new account is likely to be closed by the moderation team.

Example: "My new account is @YOUR_ACCOUNT_NAME"
"""
        ),
        ActionNode(
          NodeId("decision-final"),
          """Your appeal has been denied, and the mark on the account will remain.

After carefully reviewing your case, we regret to inform you that the moderation team will not change the decision and will keep the original engine/external assistance flag on your account.

Our fair play policy is supported by robust detection methods and data. All appeals are handled by a team of experienced moderators who take the time to identify, consider, and discuss all relevant evidence before reaching a decision. Each appeal is decided on its own merits and no decision is made lightly.

To protect our methods and processes, we cannot engage in any further discussion about the decision, which is final. Our rights regarding moderation decisions are set out in our Terms of Service: lichess.org/terms-of-service.""",
          List(AppealEffect.Close)
        ),
        ActionNode(
          NodeId("false-positive"),
          """After investigating your case, we have determined that our cheat detection algorithms flagged your account mistakenly.

We are continuously improving our cheat detection so that we can efficiently prevent cheating while minimizing false positives. We apologize for the inconvenience, and have now removed the mark on your account.""",
          List(AppealEffect.Unmark, AppealEffect.Close)
        )
      )
    )
  )
