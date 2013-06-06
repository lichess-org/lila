package lila.ai
package stockfish

import akka.actor.ActorSystem
import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import com.typesafe.config.{ Config ⇒ TypesafeConfig }

import actorApi._

// We inherit, in this case, from UnboundedPriorityMailbox
// and seed it with the priority generator
final class MailBox(settings: ActorSystem.Settings, config: TypesafeConfig)
  extends UnboundedPriorityMailbox(PriorityGenerator {
    case _: PlayReq ⇒ 0
    case _: AnalReq ⇒ 1
    case _          ⇒ 3
  })
