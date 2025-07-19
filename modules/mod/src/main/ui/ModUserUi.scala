package lila.mod
package ui

import lila.core.perm.Permission
import lila.core.playban.RageSit
import lila.evaluation.Display
import lila.ui.*
import lila.user.WithPerfsAndEmails

import ScalatagsTemplate.{ *, given }
import lila.report.Report

def mzSection(key: String) =
  div(cls := s"mz-section mz-section--$key", dataRel := key, id := s"mz_$key")

final class ModUserUi(helpers: Helpers, modUi: ModUi):
  import helpers.{ *, given }

  val dataValue       = attr("data-value")
  val dataTags        = attr("data-tags")
  val playban         = iconTag(Icon.Clock)
  val alt: Frag       = i("A")
  val shadowban: Frag = iconTag(Icon.BubbleSpeech)
  val boosting: Frag  = iconTag(Icon.LineGraph)
  val engine: Frag    = iconTag(Icon.Cogs)
  val closed: Frag    = iconTag(Icon.NotAllowed)
  val clean: Frag     = iconTag(Icon.User)
  val reportban       = iconTag(Icon.CautionTriangle)
  val notesText       = iconTag(Icon.Pencil)
  val rankban         = i("R")

  def menu = mzSection("menu")(
    a(href := "#mz_actions")("Overview"),
    a(href := "#mz_kaladin")("Kaladin"),
    a(href := "#mz_irwin")("Irwin"),
    a(href := "#mz_assessments")("Evaluation"),
    a(href := "#mz_mod_log")("Mod log"),
    a(href := "#mz_reports")("Reports"),
    a(href := "#identification_screen")("Identification")
  )

  def actions(
      u: User,
      emails: lila.core.user.Emails,
      deleted: Boolean,
      pmPresets: ModPresets
  )(using Context, Me): Frag =
    mzSection("actions")(
      div(cls := "btn-rack")(
        Granter.opt(_.ModMessage).option {
          postForm(action := routes.Mod.spontaneousInquiry(u.username), title := "Start an inquiry")(
            submitButton(cls := "btn-rack__btn inquiry", title := "Hotkey: i")(i)
          )
        },
        Granter.opt(_.UserEvaluate).option {
          postForm(
            action := routes.Mod.refreshUserAssess(u.username),
            title  := "Collect data and ask irwin and Kaladin",
            cls    := "xhr"
          ):
            submitButton(cls := "btn-rack__btn")("Evaluate")
        },
        Granter.opt(_.GamesModView).option {
          a(
            cls   := "btn-rack__btn",
            href  := routes.GameMod.index(u.username),
            title := "View games"
          )("Games")
        },
        Granter.opt(_.Shadowban).option {
          a(
            cls   := "btn-rack__btn",
            href  := routes.Mod.communicationPublic(u.id),
            title := "View communications"
          )("Comms")
        }
      ),
      div(cls := "btn-rack")(
        ModUserTableUi.canCloseAlt.option {
          postForm(
            action := routes.Mod.alt(u.username, !u.marks.alt),
            title  := "Preemptively close unauthorized alt.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.alt))("Alt")
        },
        Granter.opt(_.MarkEngine).option {
          postForm(
            action := routes.Mod.engine(u.username, !u.marks.engine),
            title  := "This user is clearly cheating.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.engine))("Engine")
        },
        Granter.opt(_.MarkBooster).option {
          postForm(
            action := routes.Mod.booster(u.username, !u.marks.boost),
            title  := "Marks the user as a booster or sandbagger.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.boost))("Booster")
        },
        Granter
          .opt(_.Shadowban)
          .option:
            frag(
              postForm(
                action := routes.Mod.troll(u.username, !u.marks.troll),
                title  := "Enable/disable communication features for this user.",
                cls    := "xhr"
              )(
                submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.troll))("Shadowban")
              ),
              u.marks.troll.option:
                frag(
                  postForm(
                    action := routes.Mod.deletePmsAndChats(u.username),
                    title  := "Delete all PMs and public chat messages",
                    cls    := "xhr"
                  ):
                    submitButton(cls := "btn-rack__btn yes-no-confirm")("Clear PMs & chats")
                  ,
                  postForm(
                    action := routes.Mod.isolate(u.username, !u.marks.isolate),
                    title  := "Isolate user by preventing all PMs, follows and challenges",
                    cls    := "xhr"
                  )(
                    submitButton(
                      cls := List("btn-rack__btn yes-no-confirm" -> true, "active" -> u.marks.isolate)
                    )("Isolate")
                  )
                )
            )
        ,
        Granter.opt(_.SetKidMode).option {
          postForm(
            action := routes.Mod.kid(u.username, !u.kid.value),
            title  := "Activate kid mode if not already the case",
            cls    := "xhr"
          ):
            submitButton(cls := "btn-rack__btn yes-no-confirm", cls := u.kid.yes.option("active"))("Kid")
        },
        Granter.opt(_.RemoveRanking).option {
          postForm(
            action := routes.Mod.rankban(u.username, !u.marks.rankban),
            title  := "Include/exclude this user from the rankings.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.rankban))("Rankban")
        },
        Granter.opt(_.ArenaBan).option {
          postForm(
            action := routes.Mod.arenaBan(u.username, !u.marks.arenaBan),
            title  := "Enable/disable this user from joining all arenas.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.arenaBan))("Arena ban")
        },
        Granter.opt(_.PrizeBan).option {
          postForm(
            action := routes.Mod.prizeban(u.username, !u.marks.prizeban),
            title  := "Enable/disable this user from joining prized tournaments.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.prizeban))("Prizeban")
        },
        Granter.opt(_.ReportBan).option {
          postForm(
            action := routes.Mod.reportban(u.username, !u.marks.reportban),
            title  := "Enable/disable the report feature for this user.",
            cls    := "xhr"
          ):
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.reportban))("Reportban")
        }
      ),
      Granter
        .opt(_.CloseAccount)
        .option(
          div(cls := "btn-rack")(
            if u.enabled.yes then
              postForm(
                action := routes.Mod.closeAccount(u.username),
                title  := "Disables this account.",
                cls    := "xhr"
              )(
                submitButton(cls := "btn-rack__btn")("Close")
              )
            else if deleted then "Deleted"
            else
              frag(
                postForm(
                  action := routes.Mod.reopenAccount(u.username),
                  title  := "Re-activates this account.",
                  cls    := "xhr"
                )(submitButton(cls := "btn-rack__btn active")("Closed")),
                Granter.opt(_.GdprErase).option(gdprEraseForm(u))
              )
          )
        ),
      div(cls := "btn-rack")(
        (u.totpSecret.isDefined && Granter.opt(_.DisableTwoFactor)).option {
          postForm(
            action := routes.Mod.disableTwoFactor(u.username),
            title  := "Disables two-factor authentication for this account.",
            cls    := "xhr"
          ):
            submitButton(cls := "btn-rack__btn yes-no-confirm")("Disable 2FA")
        }
      ),
      Granter
        .opt(_.ModMessage)
        .option {
          postForm(action := routes.Mod.warn(u.username, ""), cls := "pm-preset")(
            st.select(
              st.option(value := "")("Send PM"),
              pmPresets.value.map: preset =>
                st.option(st.value := preset.name, title := preset.text)(preset.name)
            )
          )
        },
      Granter.opt(_.SetTitle).option {
        postForm(cls := "fide-title", action := routes.Mod.setTitle(u.username))(
          form3.select(
            lila.user.UserForm.title.fill(u.title)("title"),
            chess.PlayerTitle.acronyms.map(t => t -> t.value),
            "No title".some
          )
        )
      },
      Granter
        .opt(_.SetEmail)
        .so:
          frag(
            postForm(cls := "email", action := routes.Mod.setEmail(u.username))(
              st.input(
                tpe          := "email",
                value        := emails.current.so(_.value),
                name         := "email",
                placeholder  := "Email address",
                autocomplete := "off"
              ),
              submitButton(cls := "button", dataIcon := Icon.Checkmark)
            ),
            emails.previous.map: email =>
              s"Previously $email",
            postForm(
              action := routes.Mod.blankPassword(u.username),
              title  := "Blank the password",
              cls    := "btn-rack"
            ):
              submitButton(cls := "btn-rack__btn yes-no-confirm")("Blank password")
          )
      ,
      Granter.opt(_.FreePatron).option {
        postForm(
          action := routes.Mod.freePatron(u.username),
          title  := "Give free Patron wings for a month",
          cls    := "btn-rack"
        ):
          submitButton(cls := "btn-rack__btn yes-no-confirm")("Free Patron")
      }
    )

  private def gdprEraseForm(u: User)(using Context) =
    postForm(
      action := routes.Mod.gdprErase(u.username),
      cls    := "gdpr-erasure"
    )(modUi.gdprEraseButton(u)(cls := "btn-rack__btn yes-no-confirm"))

  private def canViewRolesOf(user: User)(using Option[Me]): Boolean =
    Granter.opt(_.ChangePermission) || (Granter.opt(_.Admin) && user.roles.nonEmpty)

  def prefs(u: User, hasKeyboardMove: Boolean, hasVoice: Boolean, botCompatible: Boolean)(using Context) =
    val prefList = List(
      hasKeyboardMove.option(li("keyboard moves")),
      hasVoice.option(li("voice moves")),
      botCompatible.option:
        li:
          strong:
            a(
              cls      := "text",
              dataIcon := Icon.CautionCircle,
              href     := lila.common.String.base64
                .decode("aHR0cDovL2NoZXNzLWNoZWF0LmNvbS9ob3dfdG9fY2hlYXRfYXRfbGljaGVzcy5odG1s")
            )("BOT-COMPATIBLE SETTINGS")
    ).flatten
    frag(
      canViewRolesOf(u).option(
        mzSection("roles")(
          (if Granter.opt(_.ChangePermission) then a(href := routes.Mod.permissions(u.username)) else span) (
            strong(cls := "text inline", dataIcon := " ")("Permissions: "),
            if u.roles.isEmpty then "Add some" else Permission(u).map(_.name).mkString(", ")
          )
        )
      ),
      prefList.nonEmpty.option:
        mzSection("preferences")(
          strong(cls := "text inline", dataIcon := Icon.Gear)("Notable preferences"),
          ul(prefList)
        )
    )

  def showRageSitAndPlaybans(rageSit: RageSit, playbans: Int): Frag =
    mzSection("sitdccounter")(
      strong(cls := "text inline")("Ragesits / playbans"),
      strong(cls := "fat")(rageSit.counterView, " / ", playbans)
    )

  def teacher(u: User)(nb: Int): Frag =
    if nb == 0 then emptyFrag
    else
      mzSection("teacher")(
        strong(cls := "inline")(a(href := routes.Clas.teacher(u.username))(nb, " Classes"))
      )

  def modLog(history: List[lila.mod.Modlog])(using Translate) =
    div(cls := "mod_log mod_log--history")(
      strong(cls := "text", dataIcon := Icon.CautionTriangle)(
        "Moderation history",
        history.isEmpty.option(": nothing to show")
      ),
      history.nonEmpty.so:
        frag(
          ul:
            history.map: e =>
              li(
                userIdLink(e.mod.userId.some, withTitle = false),
                " ",
                b(e.showAction),
                " ",
                e.gameId.fold[Frag](e.details.orZero: String) { gameId =>
                  a(href := s"${routes.Round.watcher(gameId, Color.white).url}?pov=${e.user.so(_.value)}")(
                    e.details.orZero: String
                  )
                },
                " ",
                momentFromNowServer(e.date)
              )
          ,
          br
        )
    )

  def reportLog(u: User, reports: List[Report])(using Translate): Frag =
    val title = strong(cls := "text", dataIcon := Icon.CautionTriangle)(
      pluralizeLocalize("report", reports.size),
      " sent by ",
      u.username
    )
    mzSection("reports")(
      form3.fieldset(title, false.some)(cls := "mz_reports mz_reports--out")(
        reports.map: r =>
          r.atomBy(u.id.into(lila.report.ReporterId))
            .map: atom =>
              postForm(action := routes.Report.inquiry(r.id.value))(
                reportSubmitButton(r),
                " ",
                userIdLink(r.user.some, withOnline = false),
                " ",
                momentFromNowServer(atom.at),
                ": ",
                shorten(atom.text, 200)
              )
      )
    )

  def assessments(u: User, pag: lila.evaluation.PlayerAggregateAssessment.WithGames)(using
      Context
  ): Frag =
    mzSection("assessments")(
      pag.pag.sfAvgBlurs.map { blursYes =>
        p(cls := "text", dataIcon := Icon.CautionCircle)(
          "ACPL in games with blurs is ",
          strong(blursYes._1),
          " [",
          blursYes._2,
          " , ",
          blursYes._3,
          "]",
          pag.pag.sfAvgNoBlurs.so: blursNo =>
            frag(
              " against ",
              strong(blursNo._1),
              " [",
              blursNo._2,
              ", ",
              blursNo._3,
              "] in games without blurs."
            )
        )
      },
      pag.pag.sfAvgLowVar.map { lowVar =>
        p(cls := "text", dataIcon := Icon.CautionCircle)(
          "ACPL in games with consistent move times is ",
          strong(lowVar._1),
          " [",
          lowVar._2,
          ", ",
          lowVar._3,
          "]",
          pag.pag.sfAvgHighVar.so: highVar =>
            frag(
              " against ",
              strong(highVar._1),
              " [",
              highVar._2,
              ", ",
              highVar._3,
              "] in games with random move times."
            )
        )
      },
      pag.pag.sfAvgHold.map { holdYes =>
        p(cls := "text", dataIcon := Icon.CautionCircle)(
          "ACPL in games with bot signature ",
          strong(holdYes._1),
          " [",
          holdYes._2,
          ", ",
          holdYes._3,
          "]",
          pag.pag.sfAvgNoHold.so: holdNo =>
            frag(
              " against ",
              strong(holdNo._1),
              " [",
              holdNo._2,
              ", ",
              holdNo._3,
              "]  in games without bot signature."
            )
        )
      },
      table(cls := "slist")(
        thead(
          tr(
            th(a(href := routes.GameMod.index(u.username))("Games view")),
            th("Game"),
            th("Centi-Pawn", br, "(Avg ± SD)"),
            th("Move Times", br, "(Avg ± SD)"),
            th(span(title := "The frequency of which the user leaves the game page.")("Blurs")),
            th(span(title := "Bot detection using grid click analysis.")("Bot")),
            th("Date"),
            th(span(title := "Aggregate match")(raw("&Sigma;")))
          )
        ),
        tbody(
          pag.pag.playerAssessments
            .sortBy(-_.assessment.id)
            .take(15)
            .map: result =>
              tr(
                td(
                  a(href := routes.Round.watcher(result.gameId, result.color)):
                    pag
                      .pov(result)
                      .fold[Frag](result.gameId): p =>
                        playerUsername(p.opponent.light, p.opponent.userId.flatMap(lightUserSync))
                ),
                td(
                  pag
                    .pov(result)
                    .map: p =>
                      a(href := routes.Round.watcher(p.gameId, p.color))(
                        p.game.isTournament.option(iconTag(Icon.Trophy)),
                        iconTag(p.game.perfKey.perfIcon)(cls := "text"),
                        shortClockName(p.game.clock.map(_.config))
                      )
                ),
                td(
                  span(cls := s"sig sig_${Display.stockfishSig(result)}", dataIcon := Icon.DiscBig),
                  s" ${result.analysis}"
                ),
                td(
                  span(cls := s"sig sig_${Display.moveTimeSig(result)}", dataIcon := Icon.DiscBig),
                  s" ${result.basics.moveTimes / 10}",
                  result.basics.mtStreak.so(frag(br, "streak"))
                ),
                td(
                  span(cls := s"sig sig_${Display.blurSig(result)}", dataIcon := Icon.DiscBig),
                  s" ${result.basics.blurs}%",
                  result.basics.blurStreak.filter(8.<=).map { s =>
                    frag(br, s"streak $s/12")
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.holdSig(result)}", dataIcon := Icon.DiscBig),
                  if result.basics.hold then "Yes" else "No"
                ),
                td(
                  pag
                    .pov(result)
                    .map: p =>
                      a(href := routes.Round.watcher(p.gameId, p.color), cls := "glpt")(
                        momentFromNowServerText(p.game.movedAt)
                      )
                ),
                td(
                  div(cls := "aggregate"):
                    span(cls := s"sig sig_${result.assessment.id}")(result.assessment.emoticon)
                )
              )
        )
      )
    )
  def markTd(nb: Int, content: => Frag, date: Option[Instant] = None)(using ctx: Context) =
    if nb > 0 then td(cls := "i", dataSort := nb, title := date.map(showInstant))(content)
    else td

  def apply(
      users: List[WithPerfsAndEmails],
      showUsernames: Boolean = false,
      eraseButton: Boolean = false,
      checkboxes: Boolean = false
  )(using Context) =
    users.nonEmpty.option(
      table(cls := "slist slist-pad mod-user-table")(
        thead(
          tr(
            th("User"),
            th("Games"),
            th("Marks"),
            th("Closed"),
            th("Created"),
            th("Active"),
            eraseButton.option(th),
            checkboxes.option(ModUserTableUi.selectAltAll)
          )
        ),
        tbody(
          users.map { case WithPerfsAndEmails(u, emails) =>
            tr(
              if showUsernames || canViewAltUsername(u.user)
              then
                td(
                  userLink(u.user, withPerfRating = u.perfs.some, params = "?mod"),
                  Granter.opt(_.Admin).option(ModUserTableUi.email(emails.strList.mkString(", ")))
                )
              else td,
              td(u.count.game.localize),
              td(
                u.marks.alt.option(ModUserTableUi.mark("ALT")),
                u.marks.engine.option(ModUserTableUi.mark("ENGINE")),
                u.marks.boost.option(ModUserTableUi.mark("BOOSTER")),
                u.marks.troll.option(ModUserTableUi.mark("SHADOWBAN"))
              ),
              td(u.enabled.no.option(ModUserTableUi.mark("CLOSED"))),
              td(momentFromNow(u.createdAt)),
              td(u.seenAt.map(momentFromNow(_))),
              eraseButton.option(
                td(
                  postForm(action := routes.Mod.gdprErase(u.username)):
                    modUi.gdprEraseButton(u)(cls := "button button-red button-empty yes-no-confirm")
                )
              ),
              if checkboxes then ModUserTableUi.userCheckboxTd(u.marks.alt)
              else
                ModUserTableUi.canCloseAlt.option:
                  td(
                    (!u.marks.alt).option(
                      button(
                        cls  := "button button-empty button-thin button-red mark-alt",
                        href := routes.Mod.alt(u.id, !u.marks.alt)
                      )("ALT")
                    )
                  )
            )
          }
        )
      )
    )

  def parts(ps: Option[String]*) = ps.flatten.distinct.mkString(" ")

  def reportSubmitButton(r: lila.report.Report)(using Translate) =
    submitButton(
      title := {
        if r.open then "open"
        else s"closed: ${r.done.fold("no data")(done => s"by ${done.by} at ${showInstant(done.at)}")}"
      }
    )(lila.report.ui.ReportUi.reportScore(r.score), " ", strong(r.room.name))

  def userMarks(o: User, playbans: Option[Int]) =
    div(cls := "user_marks")(
      playbans.map(playban(_)),
      o.marks.troll.option(shadowban),
      o.marks.boost.option(boosting),
      o.marks.engine.option(engine),
      o.enabled.no.option(closed),
      o.marks.reportban.option(reportban),
      o.marks.rankban.option(rankban)
    )
