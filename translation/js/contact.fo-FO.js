"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.contact)window.i18n.contact={};let i=window.i18n.contact;i['accountLost']="Fekst tú hinvegin í roynd og veru hjálp frá einari talvteldu - eisini um tað bert var ta einu ferðina - er konta tín tíverri farin fyri skeyti.";i['accountSupport']="Mær tørvar kontustuðul";i['authorizationToUse']="Loyvi at nýta Lichess";i['banAppeal']="Bið um eitt bann ella eina IP-avmarking";i['buyingLichess']="At keypa Lichess";i['calledEnPassant']="Tað eitur at taka í framlopi (\\\"en passant\\\"), og er ein av reglunum í talvi.";i['cantChangeMore']="Av tøkniligum ávum ber ikki til at broyta annað enn støddina á bókstavum; um teir eru við stórum ella smáum.";i['cantClearHistory']="Tað er ikki møguligt at strika tínar talv- og uppgávuhendingagongdir ella styrkitøl tíni.";i['castlingImported']="Tryggja tær, at tú valdi røttu innstillingina fyri at leypa í borg, um tú las talvið inn ella byrjaði tað frá eini ávísari støðu.";i['castlingPrevented']="Forðað verður bert fyri at leypa í borg, um kongurin noyðist at ganga tvørtur um ein punt, ið mótleikarin hevur tamarhald á.";i['castlingRules']="Ver vís/ur í, at tú skilir reglurnar fyri at leypa í borg";i['changeUsernameCase']="Vitja hesa síðuna at broyta støddina á bókstavum í brúkaranavni tínum";i['closeYourAccount']="Tú kanst steingja kontu tína á hesari síðuni";i['collaboration']="Samstarv, løgfrøði, vinnuligt virksemi";i['contact']="Samband";i['contactLichess']="Set teg í samband við Lichess";i['creditAppreciated']="At nevna Lichess sum heimild verður virðismett, men er ikki kravt.";i['doNotAskByEmail']="Ikki biðja okkum í einum telduposti at steingja eina kontu. Vit fara ikki at gera tað.";i['doNotAskByEmailToReopen']="Ikki biðja okkum í einum telduposti at lata eina kontu upp aftur. Vit fara ikki at gera tað.";i['doNotDeny']="Nokta ikki, at tú svikaði. Vilt tú hava loyvi at stovna eina nýggja kontu, mást tú viðganga tað, tú gjørdi, og vísa, at tú skilir, at tað var eitt mistak.";i['doNotMessageModerators']="Send ikki beinleiðis boð til fyriskiparar.";i['doNotReportInForum']="Ikki melda telvarar á torginum.";i['doNotSendReportEmails']="Send okkum ikki teldupostar við fráboðanum.";i['doPasswordReset']="Nullstilla loyniorðið til tess at burturbeina seinna stigið";i['engineAppeal']="Fráboðan um svik ella talvteldu";i['errorPage']="Skeivleikasíða";i['explainYourRequest']="Greið frá fyrispurningi tínum út í æsir. Upplýs títt Lichess-brúkaranavn og onnur viðurskifti, ið kunnu hjálpa okkum at hjálpa tær.";i['falsePositives']="Vit eru hørm um, at fólk viðhvørt kunnu verða bannað av órøttum.";i['fideMate']="Sambært altjóða FIDE-reglunum fyri telving §6.9 endar eitt talv ikki við remis, um møguligt er at seta skák og mát við eini lógligari raðfylgju av leikum";i['forgotPassword']="Gloymt loyniorðið";i['forgotUsername']="Eg havi gloymt mítt brúkaranavn";i['howToReportBug']="Greið vinaliga frá, hvussu trupulleikin ber seg at, hvat tú hevði væntað ístaðin, og tey stig, ið skulu til fyri at endurskapa trupulleikan.";i['iCantLogIn']="Eg fái ikki ritað inn";i['ifLegit']="Um hald er í áheitan tíni, seta vit bannið úr gildi skjótast gjørligt.";i['illegalCastling']="Ólógligur ella ómøguligur máti at leypa í borg";i['illegalPawnCapture']="Ólógligur háttur at taka finnuna";i['insufficientMaterial']="Ov fá talvfólk eftir at seta skák og mát";i['knightMate']="Tað er møguligt at seta skák og mát við bert einum riddara ella einum bispi, um mótleikarin hevur meir enn ein kong á borðinum.";i['lost2FA']="Eg misti atgongdina til mínar tveystigs-staðfestingarkotur";i['monetizing']="At tjena pening upp á Lichess";i['noConfirmationEmail']="Eg fekk ikki teldupost við váttan";i['noneOfTheAbove']="Einki av tí, ið stendur omanfyri";i['noRatingPoints']="Eingi ratingstig vórðu givin";i['onlyReports']="Tað er bert munadygt at melda telvarar gjøgnum fráboðanaroyðublaðið.";i['orCloseAccount']="Hinvegin ber til hjá tær at steingja tína núverandi kontu og stovna eina nýggja.";i['otherRestriction']="Aðrar avmarkingar";i['ratedGame']="Ver vísur í, at tú telvaði eitt talv, ið varð styrkismett. Óformell talv ávirka ikki styrkitalið hjá telvaranum.";i['reopenOnThisPage']="Tú kanst lata kontu tína upp aftur á hesari síðuni. Tað ber bert til eina ferð.";i['reportBugInDiscord']="Í Lichess-tvídráttaambætaranum";i['reportBugInForum']="Í Lichess-afturboðanarpartinum av torginum";i['reportErrorPage']="Um tú barst við eina síðu við skeivleikum, kanst tú boða frá tí:";i['reportMobileIssue']="Sum ein Lichess-fartelefonsapp-trupulleiki á GitHub";i['reportWebsiteIssue']="Sum ein Lichess-heimasíðutrupulleiki á GitHub";i['sendAppealTo']=s("Tú kanst senda eina áheitan til %s.");i['sendEmailAt']=s("Send okkum ein teldupost á %s.");i['toReportAPlayerUseForm']="Til tess at melda ein telvara, nýt fráboðanaroyðublaðið";i['tryCastling']="Ven at leypa í borg við at royna hetta lítla samvirkna spælið";i['tryEnPassant']="Royn hetta lítla samvirkna spælið at læra meir um at taka í framlopi (\\\"en passant.\\\")";i['videosAndBooks']="Tú kanst vísa tað í tínum sjónfílum, og tú kanst prenta skíggjamyndir av Lichess í tínum bókum.";i['visitThisPage']="Vitja hesa síðuna at greiða trupulleikan";i['visitTitleConfirmation']="Fyri at heitið hjá tær skal verða víst á Lichess-vangamynd tíni, og fyri at tú skalt kunna luttaka í Titled Arenas, skalt tú vitja váttanarsíðuna";i['wantChangeUsername']="Eg vil broyta mítt brúkaranavn";i['wantClearHistory']="Eg vil strika mína hendingagongd ella mítt styrkital";i['wantCloseAccount']="Eg vil steingja kontu mína";i['wantReopen']="Eg vil lata kontu mína upp aftur";i['wantReport']="Eg vil melda ein telvara";i['wantReportBug']="Eg vil boða frá eini telduvillu";i['wantTitle']="Eg vil fegin, at heitið hjá mær verður víst á Lichess";i['welcomeToUse']="Tú ert vælkomin at nýta Lichess til títt virksemi, enntá handilsliga.";i['whatCanWeHelpYouWith']="Hvørjum kunnu vit hjálpa tær við?";i['youCanAlsoReachReportPage']=s("Tú røkkur eisini teirri síðuni við at klikkja %s fráboðanarknøttin á eini vangamyndssíðu.");i['youCanLoginWithEmail']="Tú kanst rita inn við teldupostbústaðinum, tú meldaði teg til við"})()