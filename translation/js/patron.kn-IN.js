"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="ಹೌದು, ಸೃಷ್ಟಿ ಕ್ರಿಯೆ ಇಲ್ಲಿದೆ (ಫ್ರೆಂಚ್‌ನಲ್ಲಿ)";i['amount']="ಮೊತ್ತ";i['bankTransfers']="ನಾವು ಬ್ಯಾಂಕ್ ವರ್ಗಾವಣೆಗಳನ್ನು ಸಹ ಸ್ವೀಕರಿಸುತ್ತೇವೆ";i['becomePatron']="ಲಿಚೆಸ್ ಪೋಷಕರಾಗಿ";i['cancelSupport']="ನಿಮ್ಮ ಬೆಂಬಲವನ್ನು ರದ್ದುಗೊಳಿಸಿ";i['celebratedPatrons']="ಸಾಧ್ಯವಾಗಿಸುವ ಪ್ರಸಿದ್ಧ ಪೋಷಕರು";i['changeCurrency']="ಕರೆನ್ಸಿ ಬದಲಾಯಿಸಿ";i['changeMonthlyAmount']=s("ಮಾಸಿಕ ಮೊತ್ತವನ್ನು ಬದಲಾಯಿಸಿ (%s)");i['changeMonthlySupport']="ನನ್ನ ಮಾಸಿಕ ಬೆಂಬಲವನ್ನು ನಾನು ಬದಲಾಯಿಸಬಹುದೇ/ರದ್ದು ಮಾಡಬಹುದೇ?";i['changeOrContact']=s("ಹೌದು, ಯಾವುದೇ ಸಮಯದಲ್ಲಿ, ಈ ಪುಟದಿಂದ.\nಅಥವಾ ನೀವು %s ಮಾಡಬಹುದು.");i['checkOutProfile']="ನಿಮ್ಮ ಪ್ರೊಫೈಲ್ ಪುಟವನ್ನು ಪರಿಶೀಲಿಸಿ!";i['contactSupport']="lichess ಬೆಂಬಲವನ್ನು ಸಂಪರ್ಕಿಸಿ";i['costBreakdown']="ವಿವರವಾದ ವೆಚ್ಚದ ವಿಭಜನೆಯನ್ನು ನೋಡಿ";i['currentStatus']="ಪ್ರಸ್ತುತ ಸ್ಥಿತಿ";i['date']="ದಿನಾಂಕ";i['decideHowMuch']="ಲಿಚೆಸ್ ನಿಮಗೆ ಯಾವುದು ಯೋಗ್ಯವಾಗಿದೆ ಎಂಬುದನ್ನು ನಿರ್ಧರಿಸಿ:";i['donate']="ದಾನ ಮಾಡಿ";i['donateAsX']=s("%s ನಂತೆ ದೇಣಿಗೆ ನೀಡಿ");i['downgradeNextMonth']="ಒಂದು ತಿಂಗಳಲ್ಲಿ, ನಿಮಗೆ ಮತ್ತೆ ಶುಲ್ಕ ವಿಧಿಸಲಾಗುವುದಿಲ್ಲ ಮತ್ತು ನಿಮ್ಮ Lichess ಖಾತೆಯು ಸಾಮಾನ್ಯ ಖಾತೆಗೆ ಹಿಂತಿರುಗುತ್ತದೆ.";i['featuresComparison']="ವಿವರವಾದ ವೈಶಿಷ್ಟ್ಯ ಹೋಲಿಕೆಯನ್ನು ನೋಡಿ";i['freeAccount']="ಉಚಿತ ಖಾತೆ";i['freeChess']="ಎಲ್ಲರಿಗೂ ಉಚಿತ ಚೆಸ್, ಶಾಶ್ವತವಾಗಿ!";i['giftPatronWings']="ಆಟಗಾರನಿಗೆ ಪೋಷಕ ರೆಕ್ಕೆಗಳನ್ನು ಉಡುಗೊರೆಯಾಗಿ ನೀಡಿ";i['giftPatronWingsShort']="ಯಾರಿಗಾದರೂ ಪೋಷಕ ರೆಕ್ಕೆಗಳನ್ನು ಉಡುಗೊರೆಯಾಗಿ ನೀಡಿ";i['ifNotRenewedThenAccountWillRevert']="ನವೀಕರಿಸದಿದ್ದರೆ, ನಿಮ್ಮ ಖಾತೆಯು ಸಾಮಾನ್ಯ ಖಾತೆಗೆ ಹಿಂತಿರುಗುತ್ತದೆ.";i['lichessIsRegisteredWith']=s("Lichess ಅನ್ನು %s ಜೊತೆಗೆ ನೋಂದಾಯಿಸಲಾಗಿದೆ.");i['lichessPatron']="ಲಿಚೆಸ್ ಪೋಷಕ";i['lifetime']="ಜೀವಮಾನ";i['lifetimePatron']="ಜೀವಮಾನದ ಲಿಚೆಸ್ ಪೋಷಕ";i['logInToDonate']="ದಾನ ಮಾಡಲು ಲಾಗ್ ಇನ್ ಮಾಡಿ";i['makeAdditionalDonation']="ಹೆಚ್ಚುವರಿ ದೇಣಿಗೆ ನೀಡಿ";i['monthly']="ಮಾಸಿಕ";i['newPatrons']="ಹೊಸ ಪೋಷಕರು";i['nextPayment']="ಮುಂದಿನ ಪಾವತಿ";i['noAdsNoSubs']="ಜಾಹೀರಾತುಗಳಿಲ್ಲ, ಚಂದಾದಾರಿಕೆಗಳಿಲ್ಲ; ಆದರೆ ಮುಕ್ತ ಮೂಲ ಮತ್ತು ಉತ್ಸಾಹ.";i['noLongerSupport']="ಇನ್ನು ಮುಂದೆ Lichess ಅನ್ನು ಬೆಂಬಲಿಸುವುದಿಲ್ಲ";i['noPatronFeatures']="ಇಲ್ಲ, ಏಕೆಂದರೆ ಲಿಚೆಸ್ ಸಂಪೂರ್ಣವಾಗಿ ಉಚಿತವಾಗಿದೆ, ಶಾಶ್ವತವಾಗಿ ಮತ್ತು ಎಲ್ಲರಿಗೂ. ಅದೊಂದು ಭರವಸೆ.\nಆದಾಗ್ಯೂ, ತಂಪಾದ ಹೊಸ ಪ್ರೊಫೈಲ್ ಐಕಾನ್‌ನೊಂದಿಗೆ ಪೋಷಕರು ಬಡಾಯಿ ಕೊಚ್ಚಿಕೊಳ್ಳುವ ಹಕ್ಕುಗಳನ್ನು ಪಡೆಯುತ್ತಾರೆ.";i['nowLifetime']="ನೀವು ಈಗ ಜೀವಮಾನದ ಲಿಚೆಸ್ ಪೋಷಕರಾಗಿದ್ದೀರಿ!";i['nowOneMonth']="ನೀವು ಈಗ ಒಂದು ತಿಂಗಳ ಕಾಲ ಲಿಚೆಸ್ ಪೋಷಕರಾಗಿದ್ದೀರಿ!";i['officialNonProfit']="ಲಿಚೆಸ್ ಅಧಿಕೃತ ಲಾಭರಹಿತವೇ?";i['onetime']="ಒಂದು ಬಾರಿ";i['onlyDonationFromAbove']="ಮೇಲಿನ ದೇಣಿಗೆ ನಮೂನೆಯು ಮಾತ್ರ ಪೋಷಕ ಸ್ಥಿತಿಯನ್ನು ನೀಡುತ್ತದೆ ಎಂಬುದನ್ನು ದಯವಿಟ್ಟು ಗಮನಿಸಿ.";i['otherAmount']="ಇತರೆ";i['otherMethods']="ದಾನದ ಇತರ ವಿಧಾನಗಳು?";i['patronFeatures']="ಕೆಲವು ವೈಶಿಷ್ಟ್ಯಗಳನ್ನು ಪೋಷಕರಿಗೆ ಕಾಯ್ದಿರಿಸಲಾಗಿದೆಯೇ?";i['patronForMonths']=p({"one":"ಒಂದು ತಿಂಗಳ ಕಾಲ ಲಿಚೆಸ್ ಪೋಷಕ","other":"%s ತಿಂಗಳವರೆಗೆ ಲಿಚೆಸ್ ಪೋಷಕ"});i['patronUntil']=s("ನೀವು %s ವರೆಗೆ ಪೋಷಕ ಖಾತೆಯನ್ನು ಹೊಂದಿರುವಿರಿ.");i['payLifetimeOnce']=s("ಒಮ್ಮೆ %s ಪಾವತಿಸಿ. ಶಾಶ್ವತವಾಗಿ ಲಿಚೆಸ್ ಪೋಷಕರಾಗಿರಿ!");i['permanentPatron']="ನೀವು ಈಗ ಶಾಶ್ವತ ಪೋಷಕ ಖಾತೆಯನ್ನು ಹೊಂದಿದ್ದೀರಿ.";i['pleaseEnterAmountInX']=s("ದಯವಿಟ್ಟು %s ನಲ್ಲಿ ಮೊತ್ತವನ್ನು ನಮೂದಿಸಿ");i['recurringBilling']="ಮರುಕಳಿಸುವ ಬಿಲ್ಲಿಂಗ್, ಪ್ರತಿ ತಿಂಗಳು ನಿಮ್ಮ ಪೋಷಕ ರೆಕ್ಕೆಗಳನ್ನು ನವೀಕರಿಸುವುದು.";i['serversAndDeveloper']=s("ಮೊದಲನೆಯದಾಗಿ, ಶಕ್ತಿಯುತ ಸರ್ವರ್‌ಗಳು.\nನಂತರ ನಾವು ಪೂರ್ಣ ಸಮಯದ ಡೆವಲಪರ್‌ಗೆ ಪಾವತಿಸುತ್ತೇವೆ: %s, ಲಿಚೆಸ್‌ನ ಸಂಸ್ಥಾಪಕ.");i['singleDonation']="ಒಂದೇ ದೇಣಿಗೆ ನಿಮಗೆ ಒಂದು ತಿಂಗಳ ಕಾಲ ಪೋಷಕ ರೆಕ್ಕೆಗಳನ್ನು ನೀಡುತ್ತದೆ.";i['stopPayments']="ನಿಮ್ಮ ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್ ಅನ್ನು ಹಿಂತೆಗೆದುಕೊಳ್ಳಿ ಮತ್ತು ಪಾವತಿಗಳನ್ನು ನಿಲ್ಲಿಸಿ:";i['stopPaymentsPayPal']="PayPal ಚಂದಾದಾರಿಕೆಯನ್ನು ರದ್ದುಗೊಳಿಸಿ ಮತ್ತು ಪಾವತಿಗಳನ್ನು ನಿಲ್ಲಿಸಿ:";i['thankYou']="ನಿಮ್ಮ ಕೊಡುಗೆಗಾಗಿ ಧನ್ಯವಾದಗಳು!";i['transactionCompleted']="ನಿಮ್ಮ ವಹಿವಾಟು ಪೂರ್ಣಗೊಂಡಿದೆ ಮತ್ತು ನಿಮ್ಮ ದೇಣಿಗೆಯ ರಸೀದಿಯನ್ನು ನಿಮಗೆ ಇಮೇಲ್ ಮಾಡಲಾಗಿದೆ.";i['tyvm']="ನಿಮ್ಮ ಸಹಾಯಕ್ಕಾಗಿ ತುಂಬಾ ಧನ್ಯವಾದಗಳು. ನೀವು ರಾಕ್!";i['update']="ನವೀಕರಿಸಿ";i['viewOthers']="ಇತರ ಲಿಚೆಸ್ ಪೋಷಕರನ್ನು ವೀಕ್ಷಿಸಿ";i['weAreNonProfit']="ನಾವು ಲಾಭರಹಿತ ಸಂಘವಾಗಿದೆ ಏಕೆಂದರೆ ಪ್ರತಿಯೊಬ್ಬರೂ ಉಚಿತ, ವಿಶ್ವ ದರ್ಜೆಯ ಚೆಸ್ ಪ್ಲಾಟ್‌ಫಾರ್ಮ್‌ಗೆ ಪ್ರವೇಶವನ್ನು ಹೊಂದಿರಬೇಕು ಎಂದು ನಾವು ನಂಬುತ್ತೇವೆ.";i['weAreSmallTeam']="ನಮ್ಮದು ಚಿಕ್ಕ ತಂಡ, ಆದ್ದರಿಂದ ನಿಮ್ಮ ಬೆಂಬಲವು ದೊಡ್ಡ ವ್ಯತ್ಯಾಸವನ್ನು ಮಾಡುತ್ತದೆ!";i['weRelyOnSupport']="ಅದನ್ನು ಸಾಧ್ಯವಾಗಿಸಲು ನಾವು ನಿಮ್ಮಂತಹ ಜನರ ಬೆಂಬಲವನ್ನು ಅವಲಂಬಿಸಿದ್ದೇವೆ. ನೀವು Lichess ಅನ್ನು ಬಳಸುವುದನ್ನು ಆನಂದಿಸುತ್ತಿದ್ದರೆ, ದಯವಿಟ್ಟು ದೇಣಿಗೆ ನೀಡುವ ಮೂಲಕ ಮತ್ತು ಪೋಷಕರಾಗುವ ಮೂಲಕ ನಮ್ಮನ್ನು ಬೆಂಬಲಿಸುವುದನ್ನು ಪರಿಗಣಿಸಿ!";i['whereMoneyGoes']="ಹಣ ಎಲ್ಲಿಗೆ ಹೋಗುತ್ತದೆ?";i['withCreditCard']="ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್";i['xBecamePatron']=s("%s ಅವರು ಲಿಚೆಸ್ ಪೋಷಕರಾದರು");i['xIsPatronForNbMonths']=p({"one":"%1$s ಅವರು %2$s ತಿಂಗಳಿಗೆ ಲಿಚೆಸ್ ಪೋಷಕರಾಗಿದ್ದಾರೆ","other":"%1$s ಅವರು %2$s ತಿಂಗಳವರೆಗೆ ಲಿಚೆಸ್ ಪೋಷಕರಾಗಿದ್ದಾರೆ"});i['xOrY']=s("%1$s or %2$s");i['youHaveLifetime']="ನೀವು ಜೀವಮಾನದ ಪೋಷಕ ಖಾತೆಯನ್ನು ಹೊಂದಿರುವಿರಿ. ಅದು ಬಹಳ ಅದ್ಭುತವಾಗಿದೆ!";i['youSupportWith']=s("ನೀವು ತಿಂಗಳಿಗೆ %s ನೊಂದಿಗೆ lichess.org ಅನ್ನು ಬೆಂಬಲಿಸುತ್ತೀರಿ.");i['youWillBeChargedXOnY']=s("%2$s ನಲ್ಲಿ ನಿಮಗೆ %1$s ಶುಲ್ಕ ವಿಧಿಸಲಾಗುತ್ತದೆ.")})()