package lila.ai
package stockfish

import akka.actor.ActorSystem
import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import com.typesafe.config.{ Config => TypesafeConfig }

import actorApi._
import lila.hub.actorApi.ai.GetLoad

// We inherit, in this case, from UnboundedPriorityMailbox
// and seed it with the priority generator
final class MailBox(settings: ActorSystem.Settings, config: TypesafeConfig)
  extends UnboundedPriorityMailbox(PriorityGenerator {
    case GetLoad              => 0
    case PlayReq(_, _, level) => level
    case _: AnalReq           => 10
    case _                    => 20
  })
