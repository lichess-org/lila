"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.contact)window.i18n.contact={};let i=window.i18n.contact;i['accountLost']="עם זאת, אם אכן הסתייעת במנועים, אפילו פעם אחת בלבד, אז לצערנו חשבונך אבד.";i['accountSupport']="אני צריך/ה תמיכת חשבון";i['authorizationToUse']="הרשאה לשימוש בליצ\\'ס";i['banAppeal']="ערעור על איסור או על הגבלה בIP";i['botRatingAbuse']="בנסיבות מסויימות כאשר משחק הוא מול בוט, משחק מדורג עשוי לא להעניק נקודות אם ייקבע שהשחקן/ית מנצל/ת את הבוט לצבירת ניקוד.";i['buyingLichess']="קניית ליצ\\'ס";i['calledEnPassant']="זה נקרא \\\"הכאה דרך הילוכו״ וזה מסע חוקי בשחמט.";i['cantChangeMore']="איננו יכולים לשנות יותר מאותיות קטנות או גדולות. מסיבות טכניות זה ממש בלתי אפשרי.";i['cantClearHistory']="לא ניתן למחוק את היסטוריית המשחק, היסטוריית הפאזלים, או הדירוגים שלך.";i['castlingImported']="אם ייבאתם את המשחק או התחלתם אותו מעמדה מותאמת אישית, בדקו אם הגדרתם את זכויות ההצרחה בצורה נכונה.";i['castlingPrevented']="הצרחה לא אפשרית באופן זמני אם המלך יצטרך לעבור דרך משבצת מותקפת.";i['castlingRules']="היו בטוחים שהבנתם את חוקי ההצרחה";i['changeUsernameCase']="בקר/י בדף זה כדי להחליף בין אותיות קטנות וגדולות בשם המשתמש שלך";i['closeYourAccount']="את/ה יכול/ה לסגור את חשבונך בעמוד זה";i['collaboration']="שיתוף פעולה: משפטי, מסחרי";i['contact']="צרו קשר";i['contactLichess']="צרו קשר עם ליצ\\'ס";i['creditAppreciated']="מתן קרדיט מוערך אך אינו נדרש.";i['doNotAskByEmail']="אל תבקשו מאיתנו לסגור את החשבון ע\\\"י אימייל, אנחנו לא נעשה את זה.";i['doNotAskByEmailToReopen']="אל תבקשו מאיתנו לפתוח מחדש את חשבונכם ע\\\"י אימייל, אנחנו לא נעשה את זה.";i['doNotDeny']="אל תכחישו שרימיתם. אם אתם רוצים שיאפשרו לכם ליצור חשבון חדש, פשוט תודו במה שעשיתם והראו שהבנתם שזו טעות.";i['doNotMessageModerators']="נא לא לשלוח הודעות ישירות למנהלים.";i['doNotReportInForum']="אל תדווחו על שחקנים בפורום.";i['doNotSendReportEmails']="אל תשלחו לנו מיילים כדי לדווח.";i['doPasswordReset']="השלימו איפוס סיסמה כדי להסיר את השלב השני שלכם";i['engineAppeal']="סימן שימוש במנוע או רמאות";i['errorPage']="שגיאה בדף";i['explainYourRequest']="אנא הסבירו את בקשתכם בבירור וביסודיות. ציינו את שם המשתמש שלכם בליצ\\'ס וכל מידע שיכול לעזור לנו לעזור לכם.";i['falsePositives']="אבחנות שגויות קורות לפעמים, ואנחנו מצטערים על כך.";i['fideMate']="על פי סעיף 6.9 לחוקי השחמט של פיד״ה, אם מט אפשרי ברצף מהלכים חוקי כלשהו, אז המשחק לא יסתיים בתיקו";i['forgotPassword']="שכחתי את הסיסמה";i['forgotUsername']="שכחתי את שם המשתמש";i['howToReportBug']="אנא תארו כיצד נראית התקלה, מה ציפיתם שיקרה במקום זאת ואת הצעדים לשחזור התקלה.";i['iCantLogIn']="אני לא מצליח/ה להתחבר";i['ifLegit']="אם הערעור שלך מוצדק, אנו נבטל את האיסור בהקדם האפשרי.";i['illegalCastling']="הצרחה לא חוקית או לא אפשרית";i['illegalPawnCapture']="הכאת (אכילת) חייל לא חוקית";i['insufficientMaterial']="היעדר חומר מספיק";i['knightMate']="אפשר לבצע מט רק עם פרש או רץ, אם ליריב יש יותר ממלך על הלוח.";i['learnHowToMakeBroadcasts']="למדו כיצד ליצור הקרנות חיות של טורנירים ב־Lichess";i['lost2FA']="איבדתי גישה לקודי האימות הדו־שלביים שלי";i['monetizing']="מונטיזציה של לִיצֶ\\'ס";i['noConfirmationEmail']="לא קיבלתי את אימייל האישור";i['noneOfTheAbove']="משהו אחר";i['noRatingPoints']="נקודות דירוג לא הוענקו";i['onlyReports']="רק דיווח על שחקנים באמצעות טופס הדיווח יזכה להתייחסות.";i['orCloseAccount']="עם זאת, את/ה יכול/ה לסגור את חשבונך הנוכחי וליצור חשבון חדש.";i['otherRestriction']="הגבלה אחרת";i['ratedGame']="בדקו אם שיחקתם משחק מדורג. משחק לא מדורג לא משפיע על הדירוג (מד הכושר).";i['reopenOnThisPage']="את/ה יכול/ה לפתוח מחדש את חשבונך בעמוד זה. זה עובד רק פעם אחת.";i['reportBugInDiscord']="בשרת הDiscord של ליצ\\'ס";i['reportBugInForum']="במדור המשוב על לִיצֶ\\'ס בפורום";i['reportErrorPage']="אם התמודדת עם שגיאה בדף, את/ה יכול/ה לדווח על זה:";i['reportMobileIssue']="כבעיה (issue) בעמוד של אפליקציית Lichess ב-GitHub";i['reportWebsiteIssue']="כבעיה (issue) בעמוד של Lichess באתר GitHub";i['sendAppealTo']=s("את/ה יכול/ה לשלוח ערעור ל%s.");i['sendEmailAt']=s("שלחו לנו אימייל ל־%s.");i['toReportAPlayerUseForm']="כדי לדווח על שחקן, השתמשו בטופס הדיווח";i['tryCastling']="נסו את המשחקים האינטראקטיביים הקצרים האלו כדי ללמוד על הצרחה";i['tryEnPassant']="נסו את המשחק האינטראקטיבי הקצר הזה כדי ללמוד עוד על ״הכאה דרך הילוכו״.";i['videosAndBooks']="אתם יכול להציג את Lichess בסרטונים שלכם, ואתם יכול להדפיס צילומי מסך מתוך לִיצֶ\\'ס בספרים שלכם.";i['visitThisPage']="צפו בעמוד זה כדי לפתור את הבעיה";i['visitTitleConfirmation']="כדי להציג את התואר שלך בפרופיל בלִיצֶ\\'ס, וכדי להשתתף בארנות לבעלי תארים (Titled Arenas), בקר/י בעמוד אימות התארים";i['wantChangeUsername']="אני רוצה לשנות את שם המשתמש שלי";i['wantClearHistory']="אני רוצה למחוק את ההיסטוריה או הדירוג שלי";i['wantCloseAccount']="אני מעוניין/ת לסגור את חשבוני";i['wantReopen']="אני רוצה לפתוח מחדש את חשבוני";i['wantReport']="אני רוצה לדווח על שחקן";i['wantReportBug']="אני רוצה לדווח על תקלה";i['wantTitle']="אני רוצה שהתואר שלי יוצג בליצ\\'ס";i['welcomeToUse']="אתם מוזמנים להשתמש בליצ\\'ס לצורך פעילותכם, גם מסחרית.";i['whatCanWeHelpYouWith']="במה נוכל לעזור?";i['youCanAlsoReachReportPage']=s("את/ה יכול/ה גם להגיע לעמוד הדיווח על ידי לחיצה על הכפתור %s בעמוד הפרופיל.");i['youCanLoginWithEmail']="את/ה יכול/ה להתחבר עם כתובת האימייל שאיתה נרשמת"})()