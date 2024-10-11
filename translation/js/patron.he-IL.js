"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="כן, הנה מסמך היצירה (בצרפתית)";i['amount']="סכום";i['bankTransfers']="אנחנו מקבלים גם העברות בנקאיות";i['becomePatron']="הפכו לתומך/ת של ליצ\\'ס";i['cancelSupport']="בטלו את תמיכתכם";i['celebratedPatrons']="התומכים המהוללים שהופכים את לִיצֶ\\'ס לאפשרי";i['changeCurrency']="שינוי מטבע";i['changeMonthlyAmount']=s("שנה את הסכום החודשי (%s)");i['changeMonthlySupport']="האם אני יכול/ה לשנות או לבטל את התמיכה החודשית שלי?";i['changeOrContact']=s("כן, בכל זמן נתון, מהעמוד הזה.\nאו שאפשר %s.");i['checkOutProfile']="הסתכלו על עמוד הפרופיל שלכם!";i['contactSupport']="ליצור קשר עם התמיכה שלנו";i['costBreakdown']="צפו במפרט ההוצאות הכלכליות";i['currentStatus']="סטטוס נוכחי";i['date']="תאריך";i['decideHowMuch']="החליטו מה ליצ\\'ס שווה בשבילכם:";i['donate']="תרמו";i['donateAsX']=s("תרומה כ־%s");i['downgradeNextMonth']="בעוד חודש לא תחויב/י שוב, וחשבון Lichess שלך יחזור להיות חשבון רגיל.";i['featuresComparison']="צפו בהבדלים בין החשבונות";i['freeAccount']="חשבון חינמי";i['freeChess']="שחמט חינם לכולם, לעד!";i['giftPatronWings']="מתן כנפי תורם/ת לשחקן/ית";i['giftPatronWingsShort']="מתן כנפי תורם/ת";i['ifNotRenewedThenAccountWillRevert']="אם לא יחודש מנוי התורם/ת, חשבונכם ישוב להיות חשבון רגיל.";i['lichessIsRegisteredWith']=s("Lichess רשום ל־%s.");i['lichessPatron']="תומך/ת ליצ\\'ס";i['lifetime']="לכל החיים";i['lifetimePatron']="תומך/ת לכל החיים";i['logInToDonate']="התחבר/י כדי לתרום";i['makeAdditionalDonation']="תרמו תרומה נוספת עכשיו";i['monthly']="חודשי";i['newPatrons']="תומכים חדשים";i['nextPayment']="התשלום הבא";i['noAdsNoSubs']="ללא פרסומות, ללא מנוי – רק קוד־פתוח ואהבה למשחק.";i['noLongerSupport']="ביטול תמיכתך בליצ\\'ס";i['noPatronFeatures']="לא, מכיוון שלִיצֶ\\'ס חינמי לחלוטין, לנצח, לכולם. זאת הבטחה. \nמה שכן, תומכים מקבלים סמליל חדש ומגניב לפרופיל ויכולות רברוב.";i['nowLifetime']="את/ה עכשיו תומך/ת לכל החיים!";i['nowOneMonth']="את/ה עכשיו תומך/ת לחודש אחד!";i['officialNonProfit']="האם לִיצֶ\\'ס הוא ארגון ללא מטרות רווח באופן רשמי?";i['onetime']="חד פעמי";i['onlyDonationFromAbove']="לידיעתכם: רק טופס התרומה שלעיל מקנה את כנפי התורם/ת.";i['otherAmount']="אחר";i['otherMethods']="האם יש אמצעים אחרים לתרום?";i['patronFeatures']="האם יש יתרונות ששמורים רק עבור תומכים?";i['patronForMonths']=p({"one":"תומך ליצ\\'ס לחודש אחד","two":"תומך ליצ\\'ס ל%s חודשים","many":"תומך ליצ\\'ס ל%s חודשים","other":"תומך ליצ\\'ס ל%s חודשים"});i['patronUntil']=s("הפכת לתומך/ת עד %s.");i['payLifetimeOnce']=s("שלמו %s פעם אחת. הפכו לתומך/ת לכל החיים!");i['paymentDetails']="פרטי התשלום";i['permanentPatron']="עכשיו יש לך חשבון תומך/ת קבוע.";i['pleaseEnterAmountInX']=s("בבקשה הכניסו סכום ב%s");i['recurringBilling']="חיוב חודשי קבוע שיחדש את כנפי התומך/ת שלך מדי חודש.";i['serversAndDeveloper']=s("בראש ובראשונה, לשרתים חזקים.\nלאחר מכן אנחנו משלמים למפתח במשרה מלאה: %s, המייסד של Lichess.");i['singleDonation']="תרומה חד פעמית תעניק לך כנפי תורם לחודש אחד.";i['stopPayments']="משכו את כרטיס האשראי שלכם ועצרו את התשלומים:";i['stopPaymentsPayPal']="בטלו את מינוי הPayPal ועצרו את התשלומים:";i['stripeManageSub']="ניהול המנוי והורדת קבלות וחשבוניות";i['thankYou']="תודה על תרומתך!";i['transactionCompleted']="העסקה שלך הושלמה, וקבלה על התרומה שלך נשלחה אליך בדוא\\\"ל.";i['tyvm']="תודה רבה מאוד על העזרה שלך. את/ה תותח/ית!";i['update']="עדכן";i['updatePaymentMethod']="עדכון אופן התשלום";i['viewOthers']="צפו בתומכי לִיצֶ\\'ס אחרים";i['weAreNonProfit']="אנחנו ארגון ללא מטרות רווח כי אנו מאמינים שלכולם צריכה להיות גישה לפלטפורמת שחמט חינמית ברמה עולמית.";i['weAreSmallTeam']="אנחנו צוות קטן, לכן התמיכה שלך תהיה משמעותית מאוד!";i['weRelyOnSupport']="אנחנו תלויים בתומכים כמוך כדי לממש את החזון הזה. אם אתם נהנים מלִיצֶ\\'ס בבקשה שקלו לתרום לנו ולהפוך לתומכ/ת!";i['whereMoneyGoes']="לאן הכסף הולך?";i['withCreditCard']="כרטיס אשראי";i['xBecamePatron']=s("%s הפך/ה לתומך של ליצ\\'ס");i['xIsPatronForNbMonths']=p({"one":"%1$s תומך/ת לִיצֶ\\'ס חודש %2$s","two":"%1$s תומך/ת לִיצֶ\\'ס %2$s חודשים","many":"%1$s תומך/ת לִיצֶ\\'ס %2$s חודשים","other":"%1$s תומך/ת לִיצֶ\\'ס %2$s חודשים"});i['xOrY']=s("%1$s או %2$s");i['youHaveLifetime']="יש לך חשבון תומך/ת לכל החיים. זה מדהים!";i['youSupportWith']=s("את/ה תומכ/ת ב־lichess.org עם תרומה של %s בחודש.");i['youWillBeChargedXOnY']=s("את/ה תחויב/י ב%1$s ב%2$s.")})()