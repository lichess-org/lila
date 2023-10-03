package views.html.user

import controllers.appeal.{ routes as appealRoutes }
import controllers.clas.routes.{ Clas as clasRoutes }
import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.appeal.Appeal
import lila.common.EmailAddress
import lila.evaluation.Display
import lila.mod.IpRender.RenderIp
import lila.mod.{ ModPresets, UserWithModlog }
import lila.playban.RageSit
import lila.security.{ Granter, Permission, UserLogins, Dated, UserClient, UserAgentParser }
import lila.user.{ Me, User }

object mod:

  import views.html.mod.userTable

  private def mzSection(key: String) =
    div(cls := s"mz-section mz-section--$key", dataRel := key, id := s"mz_$key")

  def menu =
    mzSection("menu")(
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
      emails: User.Emails,
      erased: User.Erased,
      pmPresets: ModPresets
  )(using Context): Frag =
    mzSection("actions")(
      div(cls := "btn-rack")(
        isGranted(_.ModMessage) option {
          postForm(action := routes.Mod.spontaneousInquiry(u.username), title := "Start an inquiry")(
            submitButton(cls := "btn-rack__btn inquiry", title := "Hotkey: i")(i)
          )
        },
        isGranted(_.UserEvaluate) option {
          postForm(
            action := routes.Mod.refreshUserAssess(u.username),
            title  := "Collect data and ask irwin and Kaladin",
            cls    := "xhr"
          )(
            submitButton(cls := "btn-rack__btn")("Evaluate")
          )
        },
        isGranted(_.GamesModView) option {
          a(
            cls   := "btn-rack__btn",
            href  := routes.GameMod.index(u.username),
            title := "View games"
          )("Games")
        },
        isGranted(_.Shadowban) option {
          a(
            cls   := "btn-rack__btn",
            href  := routes.Mod.communicationPublic(u.id),
            title := "View communications"
          )("Comms")
        }
      ),
      div(cls := "btn-rack")(
        canCloseAlt option {
          postForm(
            action := routes.Mod.alt(u.username, !u.marks.alt),
            title  := "Preemptively close unauthorized alt.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.alt))("Alt")
          )
        },
        isGranted(_.MarkEngine) option {
          postForm(
            action := routes.Mod.engine(u.username, !u.marks.engine),
            title  := "This user is clearly cheating.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.engine))("Engine")
          )
        },
        isGranted(_.MarkBooster) option {
          postForm(
            action := routes.Mod.booster(u.username, !u.marks.boost),
            title  := "Marks the user as a booster or sandbagger.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.boost))("Booster")
          )
        },
        isGranted(_.Shadowban) option {
          postForm(
            action := routes.Mod.troll(u.username, !u.marks.troll),
            title  := "Enable/disable communication features for this user.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.troll))("Shadowban")
          )
        },
        (u.marks.troll && isGranted(_.Shadowban)) option {
          postForm(
            action := routes.Mod.deletePmsAndChats(u.username),
            title  := "Delete all PMs and public chat messages",
            cls    := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Clear PMs & chats")
          )
        },
        isGranted(_.SetKidMode) option {
          postForm(
            action := routes.Mod.kid(u.username),
            title  := "Activate kid mode if not already the case",
            cls    := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm", cls := u.kid.option("active"))("Kid")
          )
        },
        isGranted(_.RemoveRanking) option {
          postForm(
            action := routes.Mod.rankban(u.username, !u.marks.rankban),
            title  := "Include/exclude this user from the rankings.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.rankban))("Rankban")
          )
        },
        isGranted(_.ArenaBan) option {
          postForm(
            action := routes.Mod.arenaBan(u.username, !u.marks.arenaBan),
            title  := "Enable/disable this user from joining all arenas.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.arenaBan))("Arena ban")
          )
        },
        isGranted(_.PrizeBan) option {
          postForm(
            action := routes.Mod.prizeban(u.username, !u.marks.prizeban),
            title  := "Enable/disable this user from joining prized tournaments.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.prizeban))("Prizeban")
          )
        },
        isGranted(_.ReportBan) option {
          postForm(
            action := routes.Mod.reportban(u.username, !u.marks.reportban),
            title  := "Enable/disable the report feature for this user.",
            cls    := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.reportban))("Reportban")
          )
        }
      ),
      isGranted(_.CloseAccount) option div(cls := "btn-rack")(
        if u.enabled.yes then
          postForm(
            action := routes.Mod.closeAccount(u.username),
            title  := "Disables this account.",
            cls    := "xhr"
          )(
            submitButton(cls := "btn-rack__btn")("Close")
          )
        else if erased.value then "Erased"
        else
          frag(
            postForm(
              action := routes.Mod.reopenAccount(u.username),
              title  := "Re-activates this account.",
              cls    := "xhr"
            )(submitButton(cls := "btn-rack__btn active")("Closed")),
            isGranted(_.GdprErase) option gdprEraseForm(u)
          )
      ),
      div(cls := "btn-rack")(
        (u.totpSecret.isDefined && isGranted(_.DisableTwoFactor)) option {
          postForm(
            action := routes.Mod.disableTwoFactor(u.username),
            title  := "Disables two-factor authentication for this account.",
            cls    := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Disable 2FA")
          )
        },
        (isGranted(_.Impersonate) || (isGranted(_.Admin) && u.id == User.lichessId)) option {
          postForm(action := routes.Mod.impersonate(u.username))(
            submitButton(cls := "btn-rack__btn")("Impersonate")
          )
        }
      ),
      isGranted(_.ModMessage) option
        postForm(action := routes.Mod.warn(u.username, ""), cls := "pm-preset")(
          st.select(
            option(value := "")("Send PM"),
            pmPresets.value.map { preset =>
              option(st.value := preset.name, title := preset.text)(preset.name)
            }
          )
        ),
      isGranted(_.SetTitle) option {
        postForm(cls := "fide-title", action := routes.Mod.setTitle(u.username))(
          form3.select(
            lila.user.UserForm.title.fill(u.title)("title"),
            lila.user.Title.acronyms.map(t => t -> t.value),
            "No title".some
          )
        )
      },
      isGranted(_.SetEmail) so frag(
        postForm(cls := "email", action := routes.Mod.setEmail(u.username))(
          st.input(
            tpe         := "email",
            value       := emails.current.so(_.value),
            name        := "email",
            placeholder := "Email address"
          ),
          submitButton(cls := "button", dataIcon := licon.Checkmark)
        ),
        emails.previous.map { email =>
          s"Previously $email"
        }
      )
    )

  private def gdprEraseForm(u: User)(using Context) =
    postForm(
      action := routes.Mod.gdprErase(u.username),
      cls    := "gdpr-erasure"
    )(gdprEraseButton(u)(cls := "btn-rack__btn confirm"))

  def gdprEraseButton(u: User)(using Context) =
    val allowed = u.marks.clean || isGranted(_.Admin)
    submitButton(
      cls := !allowed option "disabled",
      title := {
        if allowed
        then "Definitely erase everything about this user"
        else "This user has some history, only admins can erase"
      },
      !allowed option disabled
    )("GDPR erasure")

  def prefs(u: User)(pref: lila.pref.Pref)(using Context) =
    frag(
      canViewRoles(u) option mzSection("roles")(
        (if isGranted(_.ChangePermission) then a(href := routes.Mod.permissions(u.username)) else span) (
          strong(cls := "text inline", dataIcon := " ")("Permissions: "),
          if u.roles.isEmpty then "Add some" else Permission(u.roles).map(_.name).mkString(", ")
        )
      ),
      mzSection("preferences")(
        strong(cls := "text inline", dataIcon := licon.Gear)("Notable preferences"),
        ul(
          pref.hasKeyboardMove option li("keyboard moves"),
          pref.botCompatible option li(
            strong(
              a(
                cls      := "text",
                dataIcon := licon.CautionCircle,
                href := lila.common.String.base64
                  .decode("aHR0cDovL2NoZXNzLWNoZWF0LmNvbS9ob3dfdG9fY2hlYXRfYXRfbGljaGVzcy5odG1s")
              )("BOT-COMPATIBLE SETTINGS")
            )
          )
        )
      )
    )

  def showRageSitAndPlaybans(rageSit: RageSit, playbans: Int): Frag =
    mzSection("sitdccounter")(
      strong(cls := "text inline")("Ragesits / playbans"),
      strong(cls := "fat")(rageSit.counterView, " / ", playbans)
    )

  def plan(u: User)(charges: List[lila.plan.Charge])(using Context): Option[Frag] =
    charges.nonEmpty option
      mzSection("plan")(
        strong(cls := "text inline", dataIcon := patronIconChar)(
          "Patron payments",
          isGranted(_.PayPal) option {
            charges.find(_.giftTo.isEmpty).flatMap(_.payPal).flatMap(_.subId).map { subId =>
              frag(
                " - ",
                a(
                  href := s"https://www.paypal.com/fr/cgi-bin/webscr?cmd=_profile-recurring-payments&encrypted_profile_id=$subId"
                )("[PayPal sub]")
              )
            }
          }
        ),
        ul(
          charges.map { c =>
            li(
              c.giftTo match
                case Some(giftedId) if u is giftedId => frag("Gift from", userIdLink(c.userId), " ")
                case Some(giftedId)                  => frag("Gift to", userIdLink(giftedId.some), " ")
                case _                               => emptyFrag
              ,
              c.money.display,
              " with ",
              c.serviceName,
              " on ",
              showInstantUTC(c.date),
              " UTC"
            )
          }
        ),
        br
      )

  def student(managed: lila.clas.Student.ManagedInfo)(using Context): Frag =
    mzSection("student")(
      "Created by ",
      userLink(managed.createdBy),
      " for class ",
      a(href := clasRoutes.show(managed.clas.id.value))(managed.clas.name)
    )

  def boardTokens(tokens: List[lila.oauth.AccessToken])(using Context): Frag =
    if tokens.isEmpty then emptyFrag
    else
      mzSection("boardTokens")(
        strong(cls := "inline")(pluralize("Board token", tokens.size)),
        ul:
          tokens.map: token =>
            li(
              List(token.description, token.clientOrigin).flatten.mkString(" "),
              ", last used ",
              token.usedAt map momentFromNowOnce
            )
      )

  def teacher(u: User)(nb: Int)(using Context): Frag =
    if nb == 0 then emptyFrag
    else
      mzSection("teacher")(strong(cls := "inline")(a(href := clasRoutes.teacher(u.username))(nb, " Classes")))

  def modLog(history: List[lila.mod.Modlog], appeal: Option[lila.appeal.Appeal])(using Lang) =
    mzSection("mod_log")(
      div(cls := "mod_log mod_log--history")(
        strong(cls := "text", dataIcon := licon.CautionTriangle)(
          "Moderation history",
          history.isEmpty option ": nothing to show"
        ),
        history.nonEmpty so frag(
          ul:
            history.map: e =>
              li(
                userIdLink(e.mod.userId.some, withTitle = false),
                " ",
                b(e.showAction),
                " ",
                e.gameId.fold[Frag](e.details.orZero: String) { gameId =>
                  a(href := s"${routes.Round.watcher(gameId, "white").url}?pov=${e.user.so(_.value)}")(
                    e.details.orZero: String
                  )
                },
                " ",
                momentFromNowServer(e.date)
              )
          ,
          br
        )
      ),
      appeal.map: a =>
        frag(
          div(cls := "mod_log mod_log--appeal")(
            st.a(href := appealRoutes.Appeal.show(a.id)):
              strong(cls := "text", dataIcon := licon.CautionTriangle)("Appeal status: ", a.status.toString)
            ,
            br,
            a.msgs.map(_.text).map(shorten(_, 140)).map(p(_)),
            a.msgs.size > 1 option st.a(href := appealRoutes.Appeal.show(a.id)):
              frag("and ", pluralize("more message", a.msgs.size - 1))
          )
        )
    )

  def reportLog(u: User)(reports: lila.report.Report.ByAndAbout)(using Lang): Frag =
    mzSection("reports")(
      div(cls := "mz_reports mz_reports--out")(
        strong(cls := "text", dataIcon := licon.CautionTriangle)(
          s"Reports sent by ${u.username}",
          reports.by.isEmpty option ": nothing to show."
        ),
        reports.by.map: r =>
          r.atomBy(lila.report.ReporterId(u.id)).map { atom =>
            postForm(action := reportRoutes.inquiry(r.id))(
              reportSubmitButton(r),
              " ",
              userIdLink(r.user.some),
              " ",
              momentFromNowServer(atom.at),
              ": ",
              shorten(atom.text, 200)
            )
          }
      ),
      div(cls := "mz_reports mz_reports--in")(
        strong(cls := "text", dataIcon := licon.CautionTriangle)(
          s"Reports concerning ${u.username}",
          reports.about.isEmpty option ": nothing to show."
        ),
        reports.about.map: r =>
          postForm(action := reportRoutes.inquiry(r.id))(
            reportSubmitButton(r),
            div(cls := "atoms")(
              r.bestAtoms(3).map { atom =>
                div(cls := "atom")(
                  "By ",
                  userIdLink(atom.by.userId.some),
                  " ",
                  momentFromNowServer(atom.at),
                  ": ",
                  shorten(atom.text, 200)
                )
              },
              (r.atoms.size > 3) option s"(and ${r.atoms.size - 3} more)"
            )
          )
      )
    )

  def assessments(u: User, pag: lila.evaluation.PlayerAggregateAssessment.WithGames)(using
      Context
  ): Frag =
    mzSection("assessments")(
      pag.pag.sfAvgBlurs.map { blursYes =>
        p(cls := "text", dataIcon := licon.CautionCircle)(
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
        p(cls := "text", dataIcon := licon.CautionCircle)(
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
        p(cls := "text", dataIcon := licon.CautionCircle)(
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
            .map { result =>
              tr(
                td(
                  a(href := routes.Round.watcher(result.gameId, result.color.name))(
                    pag.pov(result).fold[Frag](result.gameId) { p =>
                      playerUsername(p.opponent.light)
                    }
                  )
                ),
                td(
                  pag.pov(result).map { p =>
                    a(href := routes.Round.watcher(p.gameId, p.color.name))(
                      p.game.isTournament option iconTag(licon.Trophy),
                      iconTag(p.game.perfType.icon)(cls := "text"),
                      shortClockName(p.game.clock.map(_.config))
                    )
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.stockfishSig(result)}", dataIcon := licon.DiscBig),
                  s" ${result.analysis}"
                ),
                td(
                  span(cls := s"sig sig_${Display.moveTimeSig(result)}", dataIcon := licon.DiscBig),
                  s" ${result.basics.moveTimes / 10}",
                  result.basics.mtStreak so frag(br, "streak")
                ),
                td(
                  span(cls := s"sig sig_${Display.blurSig(result)}", dataIcon := licon.DiscBig),
                  s" ${result.basics.blurs}%",
                  result.basics.blurStreak.filter(8.<=) map { s =>
                    frag(br, s"streak $s/12")
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.holdSig(result)}", dataIcon := licon.DiscBig),
                  if result.basics.hold then "Yes" else "No"
                ),
                td(
                  pag.pov(result).map { p =>
                    a(href := routes.Round.watcher(p.gameId, p.color.name), cls := "glpt")(
                      momentFromNowServerText(p.game.movedAt)
                    )
                  }
                ),
                td(
                  div(cls := "aggregate")(
                    span(cls := s"sig sig_${result.assessment.id}")(result.assessment.emoticon)
                  )
                )
              )
            }
        )
      )
    )

  private val dataValue       = attr("data-value")
  private val dataTags        = attr("data-tags")
  private val playban         = iconTag(licon.Clock)
  private val alt: Frag       = i("A")
  private val shadowban: Frag = iconTag(licon.BubbleSpeech)
  private val boosting: Frag  = iconTag(licon.LineGraph)
  private val engine: Frag    = iconTag(licon.Cogs)
  private val closed: Frag    = iconTag(licon.NotAllowed)
  private val clean: Frag     = iconTag(licon.User)
  private val reportban       = iconTag(licon.CautionTriangle)
  private val notesText       = iconTag(licon.Pencil)
  private def markTd(nb: Int, content: => Frag, date: Option[Instant] = None)(using ctx: Context) =
    if nb > 0 then td(cls := "i", dataSort := nb, title := date.map(d => showInstantUTC(d)))(content)
    else td

  def otherUsers(mod: Me, u: User, data: UserLogins.TableData[UserWithModlog], appeals: List[Appeal])(using
      ctx: Context,
      renderIp: RenderIp
  ): Tag =
    import data.*
    val canLocate = isGranted(_.Admin)
    mzSection("others")(
      table(cls := "slist")(
        thead(
          tr(
            th(
              pluralize("linked user", userLogins.otherUsers.size),
              (max < 1000 || othersWithEmail.others.sizeIs >= max) option frag(
                nbsp,
                a(cls := "more-others")("Load more")
              )
            ),
            isGranted(_.Admin) option th("Email"),
            thSortNumber(dataSortDefault)("Same"),
            th("Games"),
            thSortNumber(playban)(cls                 := "i", title := "Playban"),
            thSortNumber(alt)(cls                     := "i", title := "Alt"),
            thSortNumber(shadowban)(cls               := "i", title := "Shadowban"),
            thSortNumber(boosting)(cls                := "i", title := "Boosting"),
            thSortNumber(engine)(cls                  := "i", title := "Engine"),
            thSortNumber(closed)(cls                  := "i", title := "Closed"),
            thSortNumber(reportban)(cls               := "i", title := "Reportban"),
            thSortNumber(notesText)(cls               := "i", title := "Notes"),
            thSortNumber(iconTag(licon.InkQuill))(cls := "i", title := "Appeals"),
            thSortNumber("Created"),
            thSortNumber("Active"),
            userTable.selectAltAll
          )
        ),
        tbody(
          othersWithEmail.others.map { case other @ UserLogins.OtherUser(log @ UserWithModlog(o, _), _, _) =>
            val userNotes = notes.filter: n =>
              n.to.is(o.id) && (ctx.me.exists(n.isFrom) || isGranted(_.Admin))
            val userAppeal = appeals.find(_.isAbout(o.id))
            tr(
              dataTags := List(
                other.ips.map(renderIp),
                other.fps,
                canLocate.so(userLogins.distinctLocationIdsOf(other.ips))
              ).flatten.mkString(" "),
              cls := o.is(u) option "same"
            )(
              if o.is(u) || Granter.canViewAltUsername(o)
              then td(dataSort := o.id)(userLink(o, withPerfRating = o.perfs.some, params = "?mod"))
              else td,
              isGranted(_.Admin) option td(emailValueOf(othersWithEmail)(o)),
              td(
                // show prints and ips separately
                dataSort := other.score + (other.ips.nonEmpty so 1000000) + (other.fps.nonEmpty so 3000000)
              )(
                List(other.ips.size -> "IP", other.fps.size -> "Print")
                  .collect:
                    case (nb, name) if nb > 0 => s"$nb $name"
                  .mkString(", ")
              ),
              td(dataSort := o.count.game)(o.count.game.localize),
              markTd(~bans.get(o.id), playban(cls := "text")(~bans.get(o.id): Int)),
              markTd(o.marks.alt so 1, alt, log.dateOf(_.alt)),
              markTd(o.marks.troll so 1, shadowban, log.dateOf(_.troll)),
              markTd(o.marks.boost so 1, boosting, log.dateOf(_.booster)),
              markTd(o.marks.engine so 1, engine, log.dateOf(_.engine)),
              markTd(o.enabled.no so 1, closed, log.dateOf(_.closeAccount)),
              markTd(o.marks.reportban so 1, reportban, log.dateOf(_.reportban)),
              userNotes.nonEmpty option {
                td(dataSort := userNotes.size)(
                  a(href := s"${routes.User.show(o.username)}?notes")(
                    notesText(
                      title := s"Notes from ${userNotes.map(_.from).map(titleNameOrId).mkString(", ")}",
                      cls   := "is-green"
                    ),
                    userNotes.size
                  )
                )
              } getOrElse td(dataSort := 0),
              userAppeal match
                case None => td(dataSort := 0)
                case Some(appeal) =>
                  td(dataSort := 1)(
                    a(
                      href := isGranted(_.Appeals).option(appealRoutes.Appeal.show(o.username).url),
                      cls := List(
                        "text"         -> true,
                        "appeal-muted" -> appeal.isMuted
                      ),
                      dataIcon := licon.InkQuill,
                      title := s"${pluralize("appeal message", appeal.msgs.size)}${appeal.isMuted so " [MUTED]"}"
                    )(appeal.msgs.size)
                  )
              ,
              td(dataSort := o.createdAt.toMillis)(momentFromNowServer(o.createdAt)),
              td(dataSort := o.seenAt.map(_.toMillis.toString))(o.seenAt.map(momentFromNowServer)),
              userTable.userCheckboxTd(o.marks.alt)
            )
          }
        )
      )
    )

  private def emailValueOf(emails: UserLogins.WithMeSortedWithEmails[UserWithModlog])(u: User) =
    emails.emails.get(u.id).map(_.value) map {
      case EmailAddress.clasIdRegex(id) => a(href := clasRoutes.show(id))(s"Class #$id")
      case email                        => frag(email)
    }

  def identification(logins: UserLogins)(using ctx: Context, renderIp: RenderIp): Frag =
    val canIpBan  = isGranted(_.IpBan)
    val canFpBan  = isGranted(_.PrintBan)
    val canLocate = isGranted(_.Admin)
    mzSection("identification")(
      canLocate option div(cls := "spy_locs")(
        table(cls := "slist slist--sort spy_filter")(
          thead(
            tr(
              th("Country"),
              th("Proxy"),
              th("Region"),
              th("City"),
              thSortNumber("Date")
            )
          ),
          tbody(
            logins.distinctLocations.toList
              .sortBy(-_.seconds)
              .map: l =>
                tr(dataValue := l.value.location.id)(
                  td(l.value.location.country),
                  td(l.value.proxy.name map { proxy => span(cls := "proxy", title := "Proxy")(proxy) }),
                  td(l.value.location.region),
                  td(l.value.location.city),
                  td(dataSort := l.date.toMillis)(momentFromNowServer(l.date))
                )
              .toList
          )
        )
      ),
      canLocate option div(cls := "spy_uas")(
        table(cls := "slist slist--sort")(
          thead(
            tr(
              th(pluralize("Device", logins.uas.size)),
              th("OS"),
              th("Client"),
              thSortNumber("Date"),
              th("Type")
            )
          ),
          tbody(
            logins.uas
              .sortBy(-_.seconds)
              .map { case Dated(ua, date) =>
                val parsed = UserAgentParser.parse(ua)
                tr(
                  td(title := ua.value)(
                    if parsed.device.family == "Other" then "Computer" else parsed.device.family
                  ),
                  td(parts(parsed.os.family.some, parsed.os.major)),
                  td(parts(parsed.userAgent.family.some, parsed.userAgent.major)),
                  td(dataSort := date.toMillis)(momentFromNowServer(date)),
                  td(UserClient(ua).toString)
                )
              }
          )
        )
      ),
      div(id := "identification_screen", cls := "spy_ips")(
        table(cls := "slist spy_filter slist--sort")(
          thead(
            tr(
              th(pluralize("IP", logins.ips.size)),
              thSortNumber("Alts"),
              th,
              th("Client"),
              thSortNumber("Date"),
              canIpBan option thSortNumber
            )
          ),
          tbody(
            logins.ips.sortBy(ip => (-ip.alts.score, -ip.ip.seconds)).map { ip =>
              val renderedIp = renderIp(ip.ip.value)
              tr(cls := ip.blocked option "blocked", dataValue := renderedIp, dataTags := ip.location.id)(
                td(a(href := routes.Mod.singleIp(renderedIp))(renderedIp)),
                td(dataSort := ip.alts.score)(altMarks(ip.alts)),
                td(ip.proxy.name map { proxy => span(cls := "proxy", title := "Proxy")(proxy) }),
                td(ip.clients.toList.map(_.toString).sorted mkString ", "),
                td(dataSort := ip.ip.date.toMillis)(momentFromNowServer(ip.ip.date)),
                canIpBan option td(dataSort := (9999 - ip.alts.cleans))(
                  button(
                    cls := List(
                      "button button-empty" -> true,
                      "button-discouraging" -> (ip.alts.cleans > 0)
                    ),
                    href := routes.Mod.singleIpBan(!ip.blocked, ip.ip.value.value)
                  )("BAN")
                )
              )
            }
          )
        )
      ),
      div(cls := "spy_fps")(
        table(cls := "slist spy_filter slist--sort")(
          thead(
            tr(
              th(pluralize("Print", logins.prints.size)),
              thSortNumber("Alts"),
              th("Client"),
              thSortNumber("Date"),
              canFpBan option thSortNumber
            )
          ),
          tbody(
            logins.prints.sortBy(fp => (-fp.alts.score, -fp.fp.seconds)).map { fp =>
              tr(cls := fp.banned option "blocked", dataValue := fp.fp.value)(
                td(a(href := routes.Mod.print(fp.fp.value.value))(fp.fp.value)),
                td(dataSort := fp.alts.score)(altMarks(fp.alts)),
                td(fp.client.toString),
                td(dataSort := fp.fp.date.toMillis)(momentFromNowServer(fp.fp.date)),
                canFpBan option td(dataSort := (9999 - fp.alts.cleans))(
                  button(
                    cls := List(
                      "button button-empty" -> true,
                      "button-discouraging" -> (fp.alts.cleans > 0)
                    ),
                    href := routes.Mod.printBan(!fp.banned, fp.fp.value.value)
                  )("BAN")
                )
              )
            }
          )
        )
      )
    )

  def apply(
      users: List[User.WithEmails],
      showUsernames: Boolean = false,
      eraseButton: Boolean = false,
      checkboxes: Boolean = false
  )(using Context, Me) =
    users.nonEmpty option table(cls := "slist slist-pad mod-user-table")(
      thead(
        tr(
          th("User"),
          th("Games"),
          th("Marks"),
          th("Closed"),
          th("Created"),
          th("Active"),
          eraseButton option th,
          checkboxes option userTable.selectAltAll
        )
      ),
      tbody(
        users.map { case lila.user.User.WithEmails(u, emails) =>
          tr(
            if showUsernames || Granter.canViewAltUsername(u.user)
            then
              td(
                userLink(u.user, withPerfRating = u.perfs.some, params = "?mod"),
                isGranted(_.Admin) option
                  userTable.email(emails.strList.mkString(", "))
              )
            else td,
            td(u.count.game.localize),
            td(
              u.marks.alt option userTable.mark("ALT"),
              u.marks.engine option userTable.mark("ENGINE"),
              u.marks.boost option userTable.mark("BOOSTER"),
              u.marks.troll option userTable.mark("SHADOWBAN")
            ),
            td(u.enabled.no option userTable.mark("CLOSED")),
            td(momentFromNow(u.createdAt)),
            td(u.seenAt.map(momentFromNow(_))),
            eraseButton option td(
              postForm(action := routes.Mod.gdprErase(u.username)):
                views.html.user.mod.gdprEraseButton(u)(cls := "button button-red button-empty confirm")
            ),
            if checkboxes then userTable.userCheckboxTd(u.marks.alt)
            else
              canCloseAlt option td(
                !u.marks.alt option button(
                  cls  := "button button-empty button-thin button-red mark-alt",
                  href := routes.Mod.alt(u.id, !u.marks.alt)
                )("ALT")
              )
          )
        }
      )
    )

  private def parts(ps: Option[String]*) = ps.flatten.distinct mkString " "

  private def altMarks(alts: UserLogins.Alts) =
    List[(Int, Frag)](
      alts.boosters -> boosting,
      alts.engines  -> engine,
      alts.trolls   -> shadowban,
      alts.alts     -> alt,
      alts.closed   -> closed,
      alts.cleans   -> clean
    ).collect:
      case (nb, tag) if nb > 4 => frag(List.fill(3)(tag), "+", nb - 3)
      case (nb, tag) if nb > 0 => frag(List.fill(nb)(tag))

  private def reportSubmitButton(r: lila.report.Report)(using Lang) =
    submitButton(
      title := {
        if r.open then "open"
        else s"closed: ${r.done.fold("no data")(done => s"by ${done.by} at ${showInstantUTC(done.at)}")}"
      }
    )(reportScore(r.score), " ", strong(r.reason.name))

  def userMarks(o: User, playbans: Option[Int]) =
    div(cls := "user_marks")(
      playbans.map: nb =>
        playban(nb),
      o.marks.troll option shadowban,
      o.marks.boost option boosting,
      o.marks.engine option engine,
      o.enabled.no option closed,
      o.marks.reportban option reportban
    )
