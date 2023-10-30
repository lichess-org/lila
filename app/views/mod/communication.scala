package views.html.mod

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.richText
import lila.common.base.StringUtils.escapeHtmlRaw
import lila.hub.actorApi.shutup.PublicSource
import lila.mod.IpRender.RenderIp
import lila.mod.UserWithModlog
import lila.relation.Follow
import lila.user.{ Me, User }
import lila.shutup.Analyser

object communication:

  def apply(
      mod: Me,
      u: User,
      players: List[(lila.game.Pov, lila.chat.MixedChat)],
      convos: List[lila.msg.ModMsgConvo],
      publicLines: List[lila.shutup.PublicLine],
      notes: List[lila.user.Note],
      history: List[lila.mod.Modlog],
      logins: lila.security.UserLogins.TableData[UserWithModlog],
      appeals: List[lila.appeal.Appeal],
      priv: Boolean
  )(using ctx: PageContext, renderIp: RenderIp) =
    views.html.base.layout(
      title = u.username + " communications",
      moreCss = frag(
        cssTag("mod.communication"),
        isGranted(_.UserModView) option cssTag("mod.user")
      ),
      moreJs = frag:
        isGranted(_.UserModView) option jsModule("mod.user")
    ) {
      main(id := "communication", cls := "box box-pad")(
        boxTop(
          h1(
            div(cls := "title")(userLink(u), " communications"),
            div(cls := "actions")(
              a(
                cls  := "button button-empty mod-zone-toggle",
                href := routes.User.mod(u.username),
                titleOrText("Mod zone (Hotkey: m)"),
                dataIcon := licon.Agent
              ),
              isGranted(_.ViewPrivateComms) option {
                if priv then
                  a(cls := "priv button active", href := routes.Mod.communicationPublic(u.username))("PMs")
                else
                  a(
                    cls   := "priv button",
                    href  := routes.Mod.communicationPrivate(u.username),
                    title := "View private messages. This will be logged in #commlog"
                  )("PMs")
              }
            )
          )
        ),
        isGranted(_.UserModView) option frag(
          div(cls := "mod-zone mod-zone-full none"),
          views.html.user.mod.otherUsers(mod, u, logins, appeals)(
            cls := "mod-zone communication__logins"
          )
        ),
        history.nonEmpty option frag(
          h2("Moderation history"),
          div(cls := "history")(
            history.map: e =>
              div(
                userIdLink(e.mod.userId.some),
                " ",
                b(e.showAction),
                " ",
                u.username,
                " ",
                e.details,
                " ",
                momentFromNowServer(e.date)
              )
          )
        ),
        notes.nonEmpty option frag(
          h2("Notes from other users"),
          div(cls := "notes")(
            notes.map: note =>
              (isGranted(_.Admin) || !note.dox) option
                div(
                  userIdLink(note.from.some),
                  " ",
                  momentFromNowServer(note.date),
                  ": ",
                  richText(note.text)
                )
          )
        ),
        h2("Dubious public chats"),
        if publicLines.isEmpty then strong("None!")
        else
          ul(cls := "public_chats")(
            publicLines.reverse.map: line =>
              li(cls := "line author")(
                line.date.fold[Frag]("[OLD]")(momentFromNowServer),
                " ",
                line.from.map:
                  case PublicSource.Tournament(id) => tournamentLink(id)
                  case PublicSource.Simul(id)      => views.html.simul.bits.link(id)
                  case PublicSource.Team(id)       => views.html.team.bits.link(id)
                  case PublicSource.Watcher(id) => a(href := routes.Round.watcher(id, "white"))("Game #", id)
                  case PublicSource.Study(id)   => a(href := routes.Study.show(id))("Study #", id)
                  case PublicSource.Swiss(id)   => views.html.swiss.bits.link(SwissId(id))
                  case PublicSource.Forum(id)   => a(href := routes.ForumPost.redirect(id))("Forum #", id)
                  case PublicSource.Ublog(id)   => a(href := routes.Ublog.redirect(id))("User blog #", id)
                ,
                nbsp,
                span(cls := "message")(highlightBad(line.text))
              )
          )
        ,
        priv option frag(
          h2("Recent private chats"),
          div(cls := "player_chats")(
            players.map: (pov, chat) =>
              div(cls := "game")(
                a(
                  href := routes.Round.player(pov.fullId),
                  cls := List(
                    "title"        -> true,
                    "friend_title" -> pov.game.fromFriend
                  ),
                  title := pov.game.fromFriend.option("Friend game")
                )(
                  titleNameOrAnon(pov.opponent.userId),
                  " – ",
                  momentFromNowServer(pov.game.movedAt)
                ),
                div(cls := "chat")(
                  chat.lines.map: line =>
                    div(
                      cls := List(
                        "line"   -> true,
                        "author" -> (UserStr(line.author) is u)
                      )
                    )(
                      userIdLink(line.userIdMaybe, withOnline = false, withTitle = false),
                      nbsp,
                      span(cls := "message")(highlightBad(line.text))
                    )
                )
              )
          ),
          div(cls := "threads")(
            h2("Recent inbox messages"),
            convos.map: modConvo =>
              div(cls := "thread")(
                p(cls := "title")(
                  strong(userLink(modConvo.contact)),
                  showSbMark(modConvo.contact),
                  modConvo.relations.in.has(Follow) option span(cls := "friend_title")(
                    "is following this user",
                    br
                  )
                ),
                table(cls := "slist")(
                  tbody(
                    modConvo.truncated option div(cls := "truncated-convo")(
                      s"Truncated, showing last ${modConvo.msgs.length} messages"
                    ),
                    modConvo.msgs.reverse.map: msg =>
                      val author = msg.user == u.id
                      tr(cls := List("post" -> true, "author" -> author))(
                        td(momentFromNowServer(msg.date)),
                        td(strong(if author then u.username else modConvo.contact.username)),
                        td(cls := "message")(highlightBad(msg.text))
                      )
                  )
                )
              )
          )
        )
      )
    }

  // incompatible with richText
  def highlightBad(text: String): Frag =
    val words = Analyser(text).badWords
    if words.isEmpty then frag(text)
    else
      val regex             = { """(?iu)""" + Analyser.bounds.wrap(words.mkString("(", "|", ")")) }.r
      def tag(word: String) = s"<bad>$word</bad>"
      raw(regex.replaceAllIn(escapeHtmlRaw(text), m => tag(m.toString)))

  private def showSbMark(u: User) =
    u.marks.troll option span(cls := "user_marks")(iconTag(licon.BubbleSpeech))
