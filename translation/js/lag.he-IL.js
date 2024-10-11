"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.lag)window.i18n.lag={};let i=window.i18n.lag;i['andNowTheLongAnswerLagComposedOfTwoValues']="ועכשיו, התשובה הארוכה! לאג במשחק נגרם משני ערכים לא קשורים אחד לשני (ככל שהם נמוכים יותר כך יותר טוב):";i['isLichessLagging']="האם יש לאג ב־Lichess?";i['lagCompensation']="פיצוי על הלאג";i['lagCompensationExplanation']="ליצ\\'ס מפצה על לאגים, בין אם הם מתרחשים לאורך זמן או שמתרחשת פתאום תקלה. הפיצוי אינו אינסופי, והוא תלוי בקטגוריית הזמן ובחישובים שונים (כגון סך הפיצוי עד עתה). כך, שני השחקנים מרגישים שתוצאת המשחק הגיונית. השהיית רשת גבוהה יותר מהיריב/ה אינה מגבלה!";i['lichessServerLatency']="זמן התגובה של שרתי Lichess";i['lichessServerLatencyExplanation']="הזמן שלוקח למהלך להתעדכן בשרת. הוא זהה לכולם, ורק תלוי בשרתים שלנו. הוא נהיה יותר ארוך ככל שיש יותר שחקנים, אבל המפתחים שלנו עושים את המיטב כדי לגרום לו להיות נמוך כמה שיותר. הוא בדרך כלל נשאר מתחת לעשירית שנייה.";i['measurementInProgressThreeDot']="מדידה מתבצעת...";i['networkBetweenLichessAndYou']="הרשת שבין ליצ\\'ס למכשיר שלך";i['networkBetweenLichessAndYouExplanation']="הזמן שלוקח למכשיר שלך לשלוח את המהלך ללִיצֶ\\'ס ולקבל את התשובה בחזרה. הוא תלוי במרחק שלך לצרפת (שם נמצאים השרתים שלנו) ובחיבור שלך לאינטרנט. מפתחי לִיצֶ\\'ס לא יכולים לתקן לך את האינטרנט, ולכן לא יכולים לגרום לפרמטר זה להיות יותר מהיר.";i['noAndYourNetworkIsBad']="לא. והרשת שלך לא עובדת כראוי.";i['noAndYourNetworkIsGood']="לא. והרשת שלך עובדת סבבה.";i['yesItWillBeFixedSoon']="כן. זה יתוקן בקרוב!";i['youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername']="אתם יכולים למצוא את הערכים האלה בכל זמן, על ידי לחיצה על שם המשתמש שלכם למעלה."})()