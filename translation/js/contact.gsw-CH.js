"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.contact)window.i18n.contact={};let i=window.i18n.contact;i['accountLost']="Falls du aber tatsächlich Computer Underschtützig gnutzt häsch - nur eis einzigs Mal - dänn häsch dis Konto leider verloore.";i['accountSupport']="Ich bruche Konto Underschtützig";i['authorizationToUse']="Genehmigung Lichess z\\'benutze";i['banAppeal']="Ischpruch gäge en Usschluss oder IP-Beschränkig";i['botRatingAbuse']="Under beschtimmte Umschtänd - wänn gäge es \\\"Bot Konto\\\" gschpillt wird - chas si, dass kei Pünkt vergeh werded, wänn feschtgschtellt wird, dass de \\\"Bot\\\" für d\\'Wertig missbrucht wird.";i['buyingLichess']="Lichess chaufe";i['calledEnPassant']="Das nännt sich \\\"en passant\\\" und e Schachregle.";i['cantChangeMore']="Mir chönd nur d\\'Gross- und d\\'Chlischribig ändere. Us technische Gründ isch nöd meh möglich.";i['cantClearHistory']="Es isch nöd möglich din Schpielverlauf, din Ufgabeverlauf oder dini Wertig z\\'lösche.";i['castlingImported']="Wänn du es Schpiel importiert häsch oder us ere beschtimmte Schtellig schtartisch, schtell sicher, dass du d\\'Rächt für d\\'Rochade richtig igschtellt häsch.";i['castlingPrevented']="E Rochade isch nur unmöglich, wänn de König über es agriffes Fäld zieh will.";i['castlingRules']="Schtell sicher, dass du mit de Rochade Regle vertraut bisch";i['changeUsernameCase']="Bsuech die Site, um d\\'Gross- und d\\'Chlischribig vu dim Benutzername z\\'ändere";i['closeYourAccount']="Du chasch dis Konto uf dere Site schlüsse";i['collaboration']="Zämmearbet, Rächtlichs, Kommerziells";i['contact']="Kontakt";i['contactLichess']="Lichess kontaktiere";i['creditAppreciated']="Erwähnig isch g\\'wünscht, aber nöd notwändig.";i['doNotAskByEmail']="Verlang nöd - per E-Mail - es Konto z\\'schlüsse, mir mached das nöd.";i['doNotAskByEmailToReopen']="Verlang nöd - per E-Mail - es Konto wieder z\\'öffne, mir mached das nöd.";i['doNotDeny']="Schtrit de Betrug nöd ab. Wänn du es neus Konto wottsch, gisch eifach zue, was du gmacht häsch und zeigsch, dass du verschtande häsch, dass es en Fähler gsi isch.";i['doNotMessageModerators']="Bitte schick de Moderatore kei diräkti Nachrichte.";i['doNotReportInForum']="Mach kei Mäldige vu Schpiller im Forum.";i['doNotSendReportEmails']="Schick eus kei Mäldige per E-Mail.";i['doPasswordReset']="Setz dis Passwort zrugg, zum de zweit Faktor entferne";i['engineAppeal']="Markierig vu Computer Underschtützig oder Betrug";i['errorPage']="Fählersite";i['explainYourRequest']="Bitte erchlär dini Afrag klar und dütlich. Nänn eus din Nutzername und alli Infos wo eus chönnd hälfe, dir z\\'hälfe.";i['falsePositives']="Fählalarm chömmed öppe Mal vor - das tuet eus leid.";i['fideMate']="Gemäss FIDE Schachregle - §6.9 - isch es Schpiel nöd Remis, wänn es Matt mit irgend ere legale Zugfolg möglich isch";i['forgotPassword']="Ich han mis Passwort vergässe";i['forgotUsername']="Ich han min Benutzername vergässe";i['howToReportBug']="Bitte beschrib, wie de Fähler usgseht, was du stattdesse erwartet häsch und wie mer de Fähler chann reproduziere.";i['iCantLogIn']="Ich chann nöd ilogge";i['ifLegit']="Wänn din Ischpruch legitim isch, ziehd mir de Usschluss so schnäll wie möglich zrugg.";i['illegalCastling']="Unerlaubti oder unmöglichi Rochade";i['illegalPawnCapture']="Unerlaubts Schlah vu Puure";i['insufficientMaterial']="Nöd gnueg Material zum Matt setze";i['knightMate']="Es isch möglich - nur mit Schpringer oder Läufer - es Schachmatt z\\'erreiche, wänn de Gägner meh als nur de König uf em Brätt hät.";i['learnHowToMakeBroadcasts']="Lern wie du - uf Lichess - dini eigeni Überträgig machsch";i['lost2FA']="Ich han de Zuegriff zu mim Zwei-Faktor-Code verlore";i['monetizing']="Lichess finanzielli Bewertig";i['noConfirmationEmail']="Ich han mis Beschtätigungs Mail nöd übercho";i['noneOfTheAbove']="Öppis anders";i['noRatingPoints']="Es sind kei Wertigspünkt vergeh worde";i['onlyReports']="Effektiv sind Mäldige vu Schpiller nur via Mäldigsformular.";i['orCloseAccount']="Du chasch jedoch dis aktuelle Konto schlüsse und defür es neus eröffne.";i['otherRestriction']="Anderi Beschränkige";i['ratedGame']="Nöd g\\'werteti Schpiel ändered dini Wertig (Rating) nöd - schpill G\\'werteti!";i['reopenOnThisPage']="Du chasch dis Konto uf dere Site wieder uf mache. Aber das darfsch nur 1 Mal.";i['reportBugInDiscord']="Im Lichess Discordserver";i['reportBugInForum']="Under \\\"Lichess Feedback\\\", im Lichess Forum";i['reportErrorPage']="Wänn du e Fählersite entdeckt häsch, chasch sie mälde:";i['reportMobileIssue']="Als es \\\"Lichess Mobile App Problem\\\" uf GitHub";i['reportWebsiteIssue']="Als es \\\"Lichess Website Problem\\\" uf GitHub";i['sendAppealTo']=s("Du chasch en Ischpruch a %s sände.");i['sendEmailAt']=s("Schick eus e E-Mail a %s.");i['toReportAPlayerUseForm']="Um en Schpiller z\\'mälde, benutzisch s\\'Mäldeformular";i['tryCastling']="Probier das chline, interaktive Schpiel und lern meh über die \\\"Rochade\\\"";i['tryEnPassant']="Probier das chline, interaktive Schpiel und lern meh über \\\"en passant\\\".";i['videosAndBooks']="Du chasch Lichess i dine Videos zeige und au Screenshots usdrucke und verwände.";i['visitThisPage']="Bsuech die Site, um s\\'Problem z\\'löse";i['visitTitleConfirmation']="Das din Titel i dim Profil azeigt wird und dass du dörfsch bi Arena Turnier für Schpiller mit Titel aträte, muesch zerscht uf d\\'Site für d\\'Titelbeschtätigung";i['wantChangeUsername']="Ich möcht min Benutzername ändere";i['wantClearHistory']="Ich möcht min Verlauf oder mini Wertig lösche";i['wantCloseAccount']="Ich wott mis Konto schlüsse";i['wantReopen']="Ich möcht mis Konto wieder uf tue";i['wantReport']="Ich wott en Schpiller mälde";i['wantReportBug']="Ich möcht en Fähler mälde";i['wantTitle']="Ich wett, dass min Titel uf Lichess azeigt wird";i['welcomeToUse']="Du chasch Lichess für Aktivitäte verwände, au für Kommerzielli.";i['whatCanWeHelpYouWith']="Mit was chömmer hälfe?";i['youCanAlsoReachReportPage']=s("Du erreichsch die Site au mit dem %s Mäldechnopf uf de Profilsite.");i['youCanLoginWithEmail']="Du chasch mit de E-Mail-Adrässe ilogge, wo du für d\\'Regischtrierig benutzt häsch"})()