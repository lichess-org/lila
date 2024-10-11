"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.contact)window.i18n.contact={};let i=window.i18n.contact;i['accountLost']="Jos kuitenkin olet käyttänyt kerrankin tietokonetta apunasi, tunnuksesi on valitettavasti mennyttä.";i['accountSupport']="Tarvitsen tukea tunnukseni käyttöön";i['authorizationToUse']="Lichessin käyttöluvat";i['banAppeal']="Valita porttikiellosta tai IP-estosta";i['botRatingAbuse']="Joissakin tilanteissa bottia vastaan pelatusta pisteytetystä pelistä ei välttämättä saa pisteitä, jos pelaajan katsotaan käyttävän bottia hyväkseen pisteiden saamiseksi.";i['buyingLichess']="Lichessin ostaminen";i['calledEnPassant']="Siirtoa kutsutaan ohestalyönniksi, ja se on osa shakin sääntöjä.";i['cantChangeMore']="Pienien ja isojen kirjainten vaihtamista lukuun ottamatta mitään ei voi muuttaa. Teknisistä syistä se on suorastaan mahdotonta.";i['cantClearHistory']="Pelihistoriaasi, tehtävähistoriaasi tai vahvuuslukuja ei voi tyhjentää.";i['castlingImported']="Jos olet tuonut pelin tai aloitit sen tietystä asemasta, varmistu että asetit linnoittautumisoikeudet oikein.";i['castlingPrevented']="Linnoitus on estetty vain, jos kuningas kulkisi vastustajan uhkaaman ruudun poikki.";i['castlingRules']="Varmistu siitä, että ymmärrät linnoitussäännöt";i['changeUsernameCase']="Tällä sivulla voit vaihtaa käyttäjänimesi pieniä kirjaimia isoiksi tai päinvastoin";i['closeYourAccount']="Voit sulkea tunnuksesi tältä sivulta";i['collaboration']="Yhteistyö, lakiasiat, kaupallisuus";i['contact']="Ota yhteyttä";i['contactLichess']="Ota yhteyttä Lichessiin";i['creditAppreciated']="Arvostamme Lichessin mainintaa, mutta emme vaadi sitä.";i['doNotAskByEmail']="Älä pyydä meitä sähköpostitse sulkemaan tunnustasi, sitä me emme tee.";i['doNotAskByEmailToReopen']="Älä pyydä meitä sähköpostitse avaamaan tunnustasi uudestaan, sitä me emme tee.";i['doNotDeny']="Älä kiellä huijaustasi. Jos haluat saada luvan uuden käyttäjätunnuksen luomiseen, myönnä tekosi ja osoita, että ymmärrät tehneesi virheen.";i['doNotMessageModerators']="Älä lähetä yksityisviestejä moderaattoreille.";i['doNotReportInForum']="Älä ilmoita pelaajia foorumilla.";i['doNotSendReportEmails']="Älä lähetä meille ilmoituksia sähköpostilla.";i['doPasswordReset']="Poista toinen tunnistautumisvaihe nollaamalla salasanasi";i['engineAppeal']="Tietokone- tai huijarimerkinnästä";i['errorPage']="Virhesivu";i['explainYourRequest']="Selitä pyyntösi selvästi ja kattavasti. Kerro Lichess-käyttäjänimesi ja kaikki muut tiedot, joista voi olla apua sinun auttamisessa.";i['falsePositives']="Vääriä positiivisia voi tulla joskus ja olemme pahoillamme siitä.";i['fideMate']="FIDE:n shakkisääntöjen §6.9 mukaan jos shakkimatti on mahdollinen millä tahansa laillisella siirtosarjalla, peli ei ole tasapeli";i['forgotPassword']="Unohdin salasanani";i['forgotUsername']="Unohdin käyttäjänimeni";i['howToReportBug']="Kuvaile bugin luonnetta. Mitä odotit tapahtuvaksi sen sijaan, ja miten bugin voi toistaa.";i['iCantLogIn']="En pääse kirjautumaan sisään";i['ifLegit']="Jos valituksesi on pätevä, kumoamme porttikiellon pikimmiten.";i['illegalCastling']="Laiton tai mahdoton linnoitus";i['illegalPawnCapture']="Laiton sotilaan lyönti";i['insufficientMaterial']="Matitukseen riittämätön materiaali";i['knightMate']="On mahdollista matittaa pelkällä ratsulla ja lähetillä, jos vastustajalla on pelkkä kuningas laudalla.";i['learnHowToMakeBroadcasts']="Opi pitämään omia lähetyksiä Lichessissä";i['lost2FA']="Olen kadottanut kaksivaiheisen tunnistautumisen koodini";i['monetizing']="Lichessin kaupallistaminen";i['noConfirmationEmail']="En saanut vahvistusta sähköpostitse";i['noneOfTheAbove']="Ei mitään näistä";i['noRatingPoints']="Vahvuuslukupisteitä ei jaettu";i['onlyReports']="Vain lomakkeen kautta tapahtuvalla ilmoituksella on vaikutusta.";i['orCloseAccount']="Voit kuitenkin sen sijaan sulkea nykyisen tunnuksesi ja avata uuden.";i['otherRestriction']="Muu rajoitus";i['ratedGame']="Varmistu että pelasit pisteytetyn pelin. Rennot pelit eivät vaikuta pelaajien vahvuuslukuihin.";i['reopenOnThisPage']="Voit avata tunnuksesi uudestaan tältä sivulta. Sen voi tehdä vain kerran.";i['reportBugInDiscord']="Lichessin Discord-palvelimella";i['reportBugInForum']="Foorumin Lichess Feedback -osiossa";i['reportErrorPage']="Jos törmäsit virhesivuun, voit raportoida sen:";i['reportMobileIssue']="Lichessin mobiilisovellusongelmaksi GitHubissa";i['reportWebsiteIssue']="Lichessin verkkosivusto-ongelmaksi GitHubissa";i['sendAppealTo']=s("Voit valittaa osoitteeseen %s.");i['sendEmailAt']=s("Lähetä sähköpostia osoitteeseen %s.");i['toReportAPlayerUseForm']="Ilmoittaaksesi pelaajasta, käytä raporttilomaketta";i['tryCastling']="Harjoittele linnoittautumista tällä pienellä interaktiivisella pelillä";i['tryEnPassant']="Opi lisää ohestalyönnistä kokeilemalla tätä pientä interaktiivista peliä.";i['videosAndBooks']="Lichess saa näkyä videoillasi, ja saat painattaa Lichessistä tehtyjä kuvakaappauksia kirjoihisi.";i['visitThisPage']="Ongelman ratkaisemiseksi käy tällä sivulla";i['visitTitleConfirmation']="Arvonimesi näyttämisestä Lichess-profiilissasi sekä arvonimiareenoihin osallistumisesta saat lisätietoa arvonimen vahvistussivulta";i['wantChangeUsername']="Haluan vaihtaa käyttäjänimeni";i['wantClearHistory']="Haluan tyhjentää historiani tai vahvuuslukuni";i['wantCloseAccount']="Haluan sulkea tunnukseni";i['wantReopen']="Haluan avata tunnukseni uudestaan";i['wantReport']="Haluan ilmoittaa pelaajan";i['wantReportBug']="Haluan ilmoittaa viasta";i['wantTitle']="Haluan minun arvonimeni näkyvän Lichessissä";i['welcomeToUse']="Saat vapaasti käyttää Lichessiä omiin tarkoituksiisi, myös kaupallisiin tarkoituksiin.";i['whatCanWeHelpYouWith']="Kuinka voimme olla avuksi?";i['youCanAlsoReachReportPage']=s("Pääset samalle sivulle myös klikkaamalla %s ilmoitusnappia pelaajan profiilisivulla.");i['youCanLoginWithEmail']="Voit kirjautua sisään sähköpostiosoitteella, jolla rekisteröidyit käyttäjäksi"})()