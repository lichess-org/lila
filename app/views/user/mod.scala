package views.html.user

import controllers.routes
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.evaluation.Display
import lila.playban.RageSit
import lila.security.{ Permission, UserLogins }
import lila.user.User

object mod {
  private def mzSection(key: String) = div(id := s"mz_$key", cls := "mz-section")

  def menu =
    mzSection("menu")(
      a(href := "#mz_actions")("Overview"),
      a(href := "#mz_irwin")("Irwin"),
      a(href := "#mz_assessments")("Evaluation"),
      a(href := "#mz_mod_log")("Mod log"),
      a(href := "#mz_reports")("Reports"),
      a(href := "#identification_screen")("Identification")
    )

  def actions(u: User, emails: User.Emails, erased: User.Erased)(implicit ctx: Context): Frag =
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
            title := "Collect data and ask irwin",
            cls := "xhr"
          )(
            submitButton(cls := "btn-rack__btn")("Evaluate")
          )
        },
        isGranted(_.Shadowban) option {
          a(
            cls := "btn-rack__btn",
            href := routes.Mod.communicationPublic(u.id),
            title := "View communications"
          )("Comms")
        },
        isGranted(_.NotifySlack) option {
          postForm(action := routes.Mod.notifySlack(u.id), title := "Notify #tavern", cls := "xhr")(
            submitButton(cls := "btn-rack__btn")("Slack")
          )
        }
      ),
      div(cls := "btn-rack")(
        isGranted(_.CloseAccount) option {
          postForm(
            action := routes.Mod.alt(u.username, !u.marks.alt),
            title := "Preemptively close unauthorized alt.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.alt))("Alt")
          )
        },
        isGranted(_.MarkEngine) option {
          postForm(
            action := routes.Mod.engine(u.username, !u.marks.engine),
            title := "This user is clearly cheating.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.engine))("Engine")
          )
        },
        isGranted(_.MarkBooster) option {
          postForm(
            action := routes.Mod.booster(u.username, !u.marks.boost),
            title := "Marks the user as a booster or sandbagger.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.boost))("Booster")
          )
        },
        isGranted(_.Shadowban) option {
          postForm(
            action := routes.Mod.troll(u.username, !u.marks.troll),
            title := "Enable/disable communication features for this user.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.troll))("Shadowban")
          )
        },
        (u.marks.troll && isGranted(_.Shadowban)) option {
          postForm(
            action := routes.Mod.deletePmsAndChats(u.username),
            title := "Delete all PMs and public chat messages",
            cls := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Clear PMs & chats")
          )
        },
        isGranted(_.SetKidMode) option {
          postForm(
            action := routes.Mod.kid(u.username),
            title := "Activate kid mode if not already the case",
            cls := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Kid")
          )
        },
        isGranted(_.RemoveRanking) option {
          postForm(
            action := routes.Mod.rankban(u.username, !u.marks.rankban),
            title := "Include/exclude this user from the rankings.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.rankban))("Rankban")
          )
        },
        isGranted(_.ReportBan) option {
          postForm(
            action := routes.Mod.reportban(u.username, !u.marks.reportban),
            title := "Enable/disable the report feature for this user.",
            cls := "xhr"
          )(
            submitButton(cls := List("btn-rack__btn" -> true, "active" -> u.marks.reportban))("Reportban")
          )
        }
      ),
      div(cls := "btn-rack")(
        if (u.enabled) {
          isGranted(_.CloseAccount) option {
            postForm(
              action := routes.Mod.closeAccount(u.username),
              title := "Disables this account.",
              cls := "xhr"
            )(
              submitButton(cls := "btn-rack__btn")("Close")
            )
          }
        } else if (erased.value) {
          "Erased"
        } else {
          isGranted(_.CloseAccount) option {
            postForm(
              action := routes.Mod.reopenAccount(u.username),
              title := "Re-activates this account.",
              cls := "xhr"
            )(
              submitButton(cls := "btn-rack__btn active")("Closed")
            )
          }
        }
      ),
      div(cls := "btn-rack")(
        (u.totpSecret.isDefined && isGranted(_.DisableTwoFactor)) option {
          postForm(
            action := routes.Mod.disableTwoFactor(u.username),
            title := "Disables two-factor authentication for this account.",
            cls := "xhr"
          )(
            submitButton(cls := "btn-rack__btn confirm")("Disable 2FA")
          )
        },
        (isGranted(_.Impersonate) || (isGranted(_.Admin) && u.id == "lichess")) option {
          postForm(action := routes.Mod.impersonate(u.username))(
            submitButton(cls := "btn-rack__btn")("Impersonate")
          )
        }
      ),
      isGranted(_.SetTitle) option {
        postForm(cls := "fide_title", action := routes.Mod.setTitle(u.username))(
          form3.select(
            lila.user.UserForm.title.fill(u.title.map(_.value))("title"),
            lila.user.Title.acronyms.map(t => t -> t),
            "".some
          )
        )
      },
      (isGranted(_.Doxing) && isGranted(_.SetEmail)) ?? frag(
        postForm(cls := "email", action := routes.Mod.setEmail(u.username))(
          st.input(
            tpe := "email",
            value := emails.current.??(_.value),
            name := "email",
            placeholder := "Email address"
          ),
          submitButton(cls := "button", dataIcon := "E")
        ),
        emails.previous.map { email =>
          s"Previously $email"
        }
      )
    )

  def prefs(u: User)(pref: lila.pref.Pref)(implicit ctx: Context) =
    frag(
      canViewRoles(u) option mzSection("roles")(
        (if (isGranted(_.ChangePermission)) a(href := routes.Mod.permissions(u.username)) else span)(
          strong(cls := "text inline", dataIcon := " ")("Permissions: "),
          if (u.roles.isEmpty) "Add some" else Permission(u.roles).map(_.name).mkString(", ")
        )
      ),
      mzSection("preferences")(
        strong(cls := "text inline", dataIcon := "%")("Notable preferences:"),
        ul(
          (pref.keyboardMove != lila.pref.Pref.KeyboardMove.NO) option li("keyboard moves"),
          pref.botCompatible option li(
            strong(
              a(
                cls := "text",
                dataIcon := "j",
                href := lila.common.String.base64
                  .decode("aHR0cDovL2NoZXNzLWNoZWF0LmNvbS9ob3dfdG9fY2hlYXRfYXRfbGljaGVzcy5odG1s")
              )("BOT-COMPATIBLE SETTINGS")
            )
          )
        )
      )
    )

  def showRageSit(rageSit: RageSit) =
    mzSection("sitdccounter")(
      strong(cls := "text inline")("Ragesit counter: "),
      span(cls := "text inline")(rageSit.counterView)
    )

  def plan(charges: List[lila.plan.Charge])(implicit ctx: Context): Option[Frag] =
    charges.headOption.map { firstCharge =>
      mzSection("plan")(
        strong(cls := "text", dataIcon := patronIconChar)(
          "Patron payments",
          isGranted(_.PayPal) option {
            firstCharge.payPal.flatMap(_.subId).map { subId =>
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
            li(c.cents.usd.toString, " with ", c.serviceName, " on ", showDateTimeUTC(c.date), " UTC")
          }
        ),
        br
      )
    }

  def student(managed: lila.clas.Student.ManagedInfo)(implicit ctx: Context): Frag =
    mzSection("student")(
      "Created by ",
      userLink(managed.createdBy),
      " for class ",
      a(href := routes.Clas.show(managed.clas.id.value))(managed.clas.name)
    )

  def modLog(history: List[lila.mod.Modlog], appeal: Option[lila.appeal.Appeal])(implicit lang: Lang) =
    mzSection("mod_log")(
      div(cls := "mod_log mod_log--history")(
        strong(cls := "text", dataIcon := "!")(
          "Moderation history",
          history.isEmpty option ": nothing to show"
        ),
        history.nonEmpty ?? frag(
          ul(
            history.map { e =>
              li(
                userIdLink(e.mod.some, withTitle = false),
                " ",
                b(e.showAction),
                " ",
                e.gameId.fold[Frag](~e.details) { gameId =>
                  a(href := s"${routes.Round.watcher(gameId, "white").url}?pov=${~e.user}")(~e.details)
                },
                " ",
                momentFromNowServer(e.date)
              )
            }
          ),
          br
        )
      ),
      appeal map { a =>
        frag(
          div(cls := "mod_log mod_log--appeal")(
            st.a(href := routes.Appeal.show(a.id))(
              strong(cls := "text", dataIcon := "!")(
                "Appeal status: ",
                a.status.toString
              )
            ),
            br,
            a.msgs.map(_.text).map(shorten(_, 140)).map(p(_)),
            a.msgs.size > 1 option st.a(href := routes.Appeal.show(a.id))(
              frag("and ", pluralize("more message", a.msgs.size - 1))
            )
          )
        )
      }
    )

  def reportLog(u: User)(reports: lila.report.Report.ByAndAbout)(implicit lang: Lang): Frag =
    mzSection("reports")(
      div(cls := "mz_reports mz_reports--out")(
        strong(cls := "text", dataIcon := "!")(
          s"Reports sent by ${u.username}",
          reports.by.isEmpty option ": nothing to show."
        ),
        reports.by.map { r =>
          r.atomBy(lila.report.ReporterId(u.id)).map { atom =>
            postForm(action := routes.Report.inquiry(r.id))(
              submitButton(reportScore(r.score), " ", strong(r.reason.name)),
              " ",
              userIdLink(r.user.some),
              " ",
              momentFromNowServer(atom.at),
              ": ",
              shorten(atom.text, 200)
            )
          }
        }
      ),
      div(cls := "mz_reports mz_reports--in")(
        strong(cls := "text", dataIcon := "!")(
          s"Reports concerning ${u.username}",
          reports.about.isEmpty option ": nothing to show."
        ),
        reports.about.map { r =>
          postForm(action := routes.Report.inquiry(r.id))(
            submitButton(reportScore(r.score), " ", strong(r.reason.name)),
            div(cls := "atoms")(
              r.bestAtoms(3).map { atom =>
                div(cls := "atom")(
                  "By ",
                  userIdLink(atom.by.value.some),
                  " ",
                  momentFromNowServer(atom.at),
                  ": ",
                  shorten(atom.text, 200)
                )
              },
              (r.atoms.size > 3) option s"(and ${r.atoms.size - 3} more)"
            )
          )
        }
      )
    )

  def assessments(u: User, pag: lila.evaluation.PlayerAggregateAssessment.WithGames)(implicit
      ctx: Context
  ): Frag =
    mzSection("assessments")(
      pag.pag.sfAvgBlurs.map { blursYes =>
        p(cls := "text", dataIcon := "j")(
          "ACPL in games with blurs is ",
          strong(blursYes._1),
          " [",
          blursYes._2,
          " , ",
          blursYes._3,
          "]",
          pag.pag.sfAvgNoBlurs ?? { blursNo =>
            frag(
              " against ",
              strong(blursNo._1),
              " [",
              blursNo._2,
              ", ",
              blursNo._3,
              "] in games without blurs."
            )
          }
        )
      },
      pag.pag.sfAvgLowVar.map { lowVar =>
        p(cls := "text", dataIcon := "j")(
          "ACPL in games with consistent move times is ",
          strong(lowVar._1),
          " [",
          lowVar._2,
          ", ",
          lowVar._3,
          "]",
          pag.pag.sfAvgHighVar ?? { highVar =>
            frag(
              " against ",
              strong(highVar._1),
              " [",
              highVar._2,
              ", ",
              highVar._3,
              "] in games with random move times."
            )
          }
        )
      },
      pag.pag.sfAvgHold.map { holdYes =>
        p(cls := "text", dataIcon := "j")(
          "ACPL in games with bot signature ",
          strong(holdYes._1),
          " [",
          holdYes._2,
          ", ",
          holdYes._3,
          "]",
          pag.pag.sfAvgNoHold ?? { holdNo =>
            frag(
              " against ",
              strong(holdNo._1),
              " [",
              holdNo._2,
              ", ",
              holdNo._3,
              "]  in games without bot signature."
            )
          }
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
                      playerUsername(p.opponent)
                    }
                  )
                ),
                td(
                  pag.pov(result).map { p =>
                    a(href := routes.Round.watcher(p.gameId, p.color.name))(
                      p.game.isTournament option iconTag("g"),
                      p.game.perfType.map { pt =>
                        iconTag(pt.iconChar)(cls := "text")
                      },
                      shortClockName(p.game.clock.map(_.config))
                    )
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.stockfishSig(result)}", dataIcon := "J"),
                  s" ${result.sfAvg} ± ${result.sfSd}"
                ),
                td(
                  span(cls := s"sig sig_${Display.moveTimeSig(result)}", dataIcon := "J"),
                  s" ${result.mtAvg / 10} ± ${result.mtSd / 10}",
                  (~result.mtStreak) ?? frag(br, "STREAK")
                ),
                td(
                  span(cls := s"sig sig_${Display.blurSig(result)}", dataIcon := "J"),
                  s" ${result.blurs}%",
                  result.blurStreak.filter(8.<=) map { s =>
                    frag(br, s"STREAK $s/12")
                  }
                ),
                td(
                  span(cls := s"sig sig_${Display.holdSig(result)}", dataIcon := "J"),
                  if (result.hold) "Yes" else "No"
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

  private val sortNumberTh    = th(attr("data-sort-method") := "number")
  private val dataSort        = attr("data-sort")
  private val dataTags        = attr("data-tags")
  private val playban         = iconTag("p")
  private val alt: Frag       = i("A")
  private val shadowban: Frag = iconTag("c")
  private val boosting: Frag  = iconTag("9")
  private val engine: Frag    = iconTag("n")
  private val closed: Frag    = iconTag("k")
  private val clean: Frag     = iconTag("r")
  private val reportban       = iconTag("!")
  private val notesText       = iconTag("m")
  private def markTd(nb: Int, content: => Frag) =
    if (nb > 0) td(cls := "i", dataSort := nb)(content)
    else td

  def otherUsers(u: User, data: UserLogins.TableData)(implicit ctx: Context): Tag = {
    import data._
    mzSection("others")(
      table(cls := "slist")(
        thead(
          tr(
            th(
              pluralize("linked user", userLogins.otherUsers.size),
              (max < 1000 && true || othersWithEmail.others.sizeIs >= max) option frag(
                nbsp,
                a(cls := "more-others")("Load more")
              )
            ),
            th("Email"),
            sortNumberTh("Same"),
            th("Games"),
            sortNumberTh(playban)(cls := "i", title := "Playban"),
            sortNumberTh(alt)(cls := "i", title := "Alt"),
            sortNumberTh(shadowban)(cls := "i", title := "Shadowban"),
            sortNumberTh(boosting)(cls := "i", title := "Boosting"),
            sortNumberTh(engine)(cls := "i", title := "Engine"),
            sortNumberTh(closed)(cls := "i", title := "Closed"),
            sortNumberTh(reportban)(cls := "i", title := "Reportban"),
            sortNumberTh(notesText)(cls := "i", title := "Notes"),
            sortNumberTh("Created"),
            sortNumberTh("Active"),
            isGranted(_.CloseAccount) option th
          )
        ),
        tbody(
          othersWithEmail.others.map { case other @ UserLogins.OtherUser(o, _, _) =>
            val dox = isGranted(_.Doxing) || (o.lameOrAlt && !o.hasTitle)
            val userNotes =
              notes.filter(n => n.to == o.id && (ctx.me.exists(n.isFrom) || isGranted(_.Doxing)))
            tr(
              dataTags := s"${other.ips.mkString(" ")} ${other.fps.mkString(" ")}",
              cls := (o == u) option "same"
            )(
              if (dox || o == u) td(dataSort := o.id)(userLink(o, withBestRating = true, params = "?mod"))
              else td,
              if (dox) td(othersWithEmail emailValueOf o)
              else td,
              td(
                // show prints and ips separately
                dataSort := other.score + (other.ips.nonEmpty ?? 1000000) + (other.fps.nonEmpty ?? 3000000)
              )(
                List(other.ips.size -> "IP", other.fps.size -> "Print")
                  .collect {
                    case (nb, name) if nb > 0 => s"$nb $name"
                  }
                  .mkString(", ")
              ),
              td(dataSort := o.count.game)(o.count.game.localize),
              markTd(~bans.get(o.id), playban(cls := "text")(~bans.get(o.id))),
              markTd(o.marks.alt ?? 1, alt),
              markTd(o.marks.troll ?? 1, shadowban),
              markTd(o.marks.boost ?? 1, boosting),
              markTd(o.marks.engine ?? 1, engine),
              markTd(o.disabled ?? 1, closed),
              markTd(o.marks.reportban ?? 1, reportban),
              userNotes.nonEmpty option {
                td(dataSort := userNotes.size)(
                  a(href := s"${routes.User.show(o.username)}?notes")(
                    notesText(
                      title := s"Notes from ${userNotes.map(_.from).map(usernameOrId).mkString(", ")}",
                      cls := "is-green"
                    ),
                    userNotes.size
                  )
                )
              } getOrElse td(dataSort := 0),
              td(dataSort := o.createdAt.getMillis)(momentFromNowServer(o.createdAt)),
              td(dataSort := o.seenAt.map(_.getMillis.toString))(o.seenAt.map(momentFromNowServer)),
              isGranted(_.CloseAccount) option td(
                !o.marks.alt option button(
                  cls := "button button-empty button-thin button-red mark-alt",
                  href := routes.Mod.alt(o.id, !o.marks.alt)
                )("ALT")
              )
            )
          }
        )
      )
    )
  }

  def identification(spy: UserLogins)(implicit ctx: Context): Frag = {
    val canIpBan = isGranted(_.IpBan)
    val canFpBan = isGranted(_.PrintBan)
    mzSection("identification")(
      div(cls := "spy_locs")(
        table(cls := "slist slist--sort")(
          thead(
            tr(
              th("Country"),
              th("Region"),
              th("City"),
              sortNumberTh("Date")
            )
          ),
          tbody(
            spy.distinctLocations.toList
              .sortBy(-_.seconds)
              .map { loc =>
                tr(
                  td(loc.value.country),
                  td(loc.value.region),
                  td(loc.value.city),
                  td(dataSort := loc.date.getMillis)(momentFromNowServer(loc.date))
                )
              }
              .toList
          )
        )
      ),
      div(cls := "spy_uas")(
        table(cls := "slist slist--sort")(
          thead(
            tr(
              th(pluralize("Device", spy.uas.size)),
              th("OS"),
              th("Client"),
              sortNumberTh("Date"),
              th("Flag")
            )
          ),
          tbody(
            spy.uas
              .sortBy(-_.seconds)
              .map { ua =>
                import ua.value.client._
                tr(
                  td(title := ua.value.ua)(if (device.family == "Other") "Computer" else device.family),
                  td(parts(os.family.some, os.major)),
                  td(parts(userAgent.family.some, userAgent.major)),
                  td(dataSort := ua.date.getMillis)(momentFromNowServer(ua.date)),
                  td(
                    if (ua.value.app) "APP"
                    else if (ua.value.mobile) "MOB"
                    else ""
                  )
                )
              }
          )
        )
      ),
      div(id := "identification_screen", cls := "spy_ips")(
        table(cls := "slist spy_filter slist--sort")(
          thead(
            tr(
              th(pluralize("IP", spy.prints.size)),
              sortNumberTh("Alts"),
              th,
              sortNumberTh("Date"),
              canIpBan option sortNumberTh
            )
          ),
          tbody(
            spy.ips.sortBy(ip => (-ip.alts.score, -ip.ip.seconds)).map { ip =>
              tr(cls := ip.blocked option "blocked")(
                td(a(href := routes.Mod.singleIp(ip.ip.value.value))(ip.ip.value)),
                td(dataSort := ip.alts.score)(altMarks(ip.alts)),
                td(ip.proxy option span(cls := "proxy")("PROXY")),
                td(dataSort := ip.ip.date.getMillis)(momentFromNowServer(ip.ip.date)),
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
              th(pluralize("Print", spy.prints.size)),
              sortNumberTh("Alts"),
              sortNumberTh("Date"),
              canFpBan option sortNumberTh
            )
          ),
          tbody(
            spy.prints.sortBy(fp => (-fp.alts.score, -fp.fp.seconds)).map { fp =>
              tr(cls := fp.banned option "blocked")(
                td(a(href := routes.Mod.print(fp.fp.value.value))(fp.fp.value)),
                td(dataSort := fp.alts.score)(altMarks(fp.alts)),
                td(dataSort := fp.fp.date.getMillis)(momentFromNowServer(fp.fp.date)),
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
  }

  private def parts(ps: Option[String]*) = ps.flatten.distinct mkString " "

  private def altMarks(alts: UserLogins.Alts) =
    List[(Int, Frag)](
      alts.boosters -> boosting,
      alts.engines  -> engine,
      alts.trolls   -> shadowban,
      alts.alts     -> alt,
      alts.closed   -> closed,
      alts.cleans   -> clean
    ) collect {
      case (nb, tag) if nb > 4 => frag(List.fill(3)(tag), "+", nb - 3)
      case (nb, tag) if nb > 0 => frag(List.fill(nb)(tag))
    }

  def userMarks(o: User, playbans: Option[Int]) =
    div(cls := "user_marks")(
      playbans.map { nb =>
        playban(nb)
      },
      o.marks.troll option shadowban,
      o.marks.boost option boosting,
      o.marks.engine option engine,
      o.disabled option closed,
      o.marks.reportban option reportban
    )
}
