package lila.security

import lila.common.String._
import lila.hub.actorApi.message.LichessThread
import lila.user.User

import akka.actor.ActorSelection

final class Greeter(
    sender: String,
    messenger: ActorSelection) {

  def apply(user: User) {
    messenger ! LichessThread(
      from = sender,
      to = user.id,
      subject = s"""Hi ${user.username}, welcome to lichess.org!""",
      message = s"""
Thank you, ${user.username}, for joining lichess.org!

Here, all features are completely free and available without limitation for now and forever.
We are the world's community answer to commercial websites: yes, chess can be free!

Note that we don't ask for your email during registration.
However you can still link it to your account as a way to recover your password: http://lichess.org/account/email.

Now play some games, enjoy the computer analysis, try out tournaments and maybe some variants!
We wish you fantastic games and loads of fun :)

Cheers,
Lichess team
""")
  }
}
