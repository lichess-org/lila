"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("כדי ליצור איתנו קשר, השתמשו ב־%s.");i['common_note']=s("זו הודעת שירות בקשר לשימושך ב־%s.");i['common_orPaste']="(אם לחיצה אינה עובדת, נסו הדבקה לתוך הדפדפן!)";i['emailChange_click']="כדי לאשר את גישתך לכתובת המייל הזו, לחץ/י על הקישור הבא:";i['emailChange_intro']="יש לשנות את כתובת הדוא״ל שלך.";i['emailChange_subject']=s("אשר/י את כתובתך החדשה, %s");i['emailConfirm_click']="לחץ/י על הקישור כדי להפעיל את החשבון שלך ב־Lichess:";i['emailConfirm_ignore']="אם לא נרשמת ל־Lichess ניתן להתעלם מהודעה זו.";i['emailConfirm_subject']=s("%s, אשר/י את החשבון שלך ב־lichess.org");i['logInToLichess']=s("%s, התחבר/י ל־lichess.org");i['passwordReset_clickOrIgnore']="אם אכן זאת היא בקשתך, אשר/י ע\\\"י פתיחת הקישור המצורף. אחרת, אין צורך בפעולה כלשהי.";i['passwordReset_intro']="קיבלנו בקשה לאפס את סיסמת חשבונך.";i['passwordReset_subject']=s("%s, אפס/י את סיסמת חשבונך ב־lichess.org");i['welcome_subject']=s("%s, ברוך/ה הבא/ה ל־lichess.org");i['welcome_text']=s("יצרת בהצלחה את חשבונך באתר https://lichess.org.\nעמוד הפרופיל שלך הוא %1$s. את/ה יכול/ה לערוך אותו כרצונך ב%2$s\nאנו מאחלים לכלים שלך תמיד למצוא את דרכם למלך של יריביך!")})()