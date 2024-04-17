package lila.core
package irc

import lila.core.id.{ RelayRoundId, UblogPostId }
import lila.core.userId.MyId

enum ModDomain:
  case Admin, Cheat, Boost, Comm, Other

enum Event:
  case Error(msg: String)
  case Warning(msg: String)
  case Info(msg: String)
  case Victory(msg: String)

trait IrcApi:
  def commReportBurst(user: LightUser): Funit
  def broadcastStart(id: RelayRoundId, fullName: String): Funit
  def broadcastError(id: RelayRoundId, name: String, error: String): Funit
  def monitorMod(icon: String, text: String, tpe: ModDomain)(using MyId): Funit
  def ublogPost(user: LightUser, id: UblogPostId, slug: String, title: String, intro: String): Funit
