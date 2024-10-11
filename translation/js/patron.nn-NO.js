"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Ja, her er grunnleggingsdokumentet. (på fransk)";i['amount']="Beløp";i['bankTransfers']="Vi tek òg mot bankoverføringar";i['becomePatron']="Bli ein bidragsytar til Lichess";i['cancelSupport']="avslutt støtta di";i['celebratedPatrons']="Dei akta bidragsytarane som gjer Lichess mogleg";i['changeCurrency']="Endra valuta";i['changeMonthlyAmount']=s("Endre månadleg beløp (%s)");i['changeMonthlySupport']="Kan eg endre/annullere det månadlege bidraget mitt?";i['changeOrContact']=s("Ja, når som helst, frå denne sida.\nEller du kan %s.");i['checkOutProfile']="Sjekk profilen din!";i['contactSupport']="ta kontakt med Lichess-hjelpa";i['costBreakdown']="Sjå ei detaljert oversikt over kostnadar";i['currentStatus']="Gjeldande status";i['date']="Dato";i['decideHowMuch']="Avgjer kor stor verde Lichess har for deg:";i['donate']="Donér";i['donateAsX']=s("Gje som %s");i['downgradeNextMonth']="Om ein månad blir du IKKJE belasta på ny, og Lichess-kontoen din blir gradert ned til gratis-nivået.";i['featuresComparison']="Sjå ein detaljert samanlikning av funksjonar";i['freeAccount']="Gratis konto";i['freeChess']="Gratis sjakk for alle, for alltid!";i['giftPatronWings']="Gje ein spelar gjevar-venger";i['giftPatronWingsShort']="Gje mesen-vengjer som gåve";i['ifNotRenewedThenAccountWillRevert']="On kontoen ikkje vert fornya får den status som vanleg konto igjen.";i['lichessIsRegisteredWith']=s("Lichess er registrert hos %s.");i['lichessPatron']="Bidragsytar til Lichess";i['lifetime']="Livstid";i['lifetimePatron']="Lichess-mesén på livstid";i['logInToDonate']="Logg inn for å donera";i['makeAdditionalDonation']="Legg inn ei ekstra pengegåve no";i['monthly']="Månadleg";i['newPatrons']="Nye bidragsytarar";i['nextPayment']="Neste betaling";i['noAdsNoSubs']="Ingen annonsar, ingen abonnement; men open-kjeldekode og glødande engasjement.";i['noLongerSupport']="Avslutt å støtte Lichess";i['noPatronFeatures']="Nei, for Lichess er heilt gratis, for alltid og for alle. Det er ein lovnad.\nMen bidragsytarar kan briske seg med et tøft profil-ikon.";i['nowLifetime']="Du er no livslang Lichess-mesen!";i['nowOneMonth']="Du er no Lichess-mesen for ein månad!";i['officialNonProfit']="Er Lichess offisielt ein nonprofit organisasjon?";i['onetime']="Ein gong";i['onlyDonationFromAbove']="Legg merke til at berre donasjonsskjemaet ovanfor gjev status som bidragsytar.";i['otherAmount']="Anna";i['otherMethods']="Andre måtar å bidra på?";i['patronFeatures']="Er nokre funksjonar reservert bidragsytarar?";i['patronForMonths']=p({"one":"Lichess-mesén i ein månad","other":"Lichess-mesén i %s månader"});i['patronUntil']=s("Du har ein bidragsytar-konto fram til %s.");i['payLifetimeOnce']=s("Betal %s ein gong og ver Lichess bidragsytar for alltid!");i['paymentDetails']="Betalingsdetaljar";i['permanentPatron']="Du har no ein permanent mesenkonto.";i['pleaseEnterAmountInX']=s("Ver venleg å oppgje eit beløp i %s");i['recurringBilling']="Tilbakevendande fakturering, forny bidragsytar-vengane dine kvar månad.";i['serversAndDeveloper']=s("Framfor alt til kraftige tenarmaskiner.\nDeretter løner vi ein fulltidsutviklar: %s, grunnleggaren av Lichess.");i['singleDonation']="Eit enkelt bidrag vil gje deg bidragsytar-vengar i ein månad.";i['stopPayments']="Slett kredittkortdetaljar og stopp faste innbetalingar:";i['stopPaymentsPayPal']="Avslutt PayPal abonnement og stopp betalingar:";i['stripeManageSub']="Administrer abonnementet ditt og last ned fakturaer og kvitteringar";i['thankYou']="Takk for bidraget ditt!";i['transactionCompleted']="Transaksjonen er gjennomført og ei kvittering er sendt til epostadressa di.";i['tyvm']="Mange takk for støtta di. Du er sjef!";i['update']="Oppdater";i['updatePaymentMethod']="Oppdater betalingsmåte";i['viewOthers']="Sjå andre Lichess-mesenar";i['weAreNonProfit']="Vi er ein nonprofitorganisasjon fordi vi meiner at alle bør ha tilgang til ein gratis, verdsomspennande sjakkplatform.";i['weAreSmallTeam']="Vi er ei lita gruppe, så støtta di er av stor vekt!";i['weRelyOnSupport']="For å gjere det mogleg er vi avhengige av stønad frå folk som deg. Dersom du set pris på å bruka Lichess, så vurdér gjerne å støtte oss med eit bidrag og bli ein bidragsytar!";i['whereMoneyGoes']="Kva går pengane til?";i['withCreditCard']="Kredittkort";i['xBecamePatron']=s("%s vart bidragsytar til Lichess");i['xIsPatronForNbMonths']=p({"one":"%1$s har støtta Lichess økonomisk i %2$s månad","other":"%1$s har støtta Lichess økonomisk i %2$s månader"});i['xOrY']=s("%1$s eller %2$s");i['youHaveLifetime']="Du har ein bidragsytar-konto på livstid. Det er rett og slett storarta!";i['youSupportWith']=s("Du støttar lichess.org med %s i månaden.");i['youWillBeChargedXOnY']=s("Du vil bli trekt %1$s, %2$s.")})()