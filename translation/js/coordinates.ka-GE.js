"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="კოორდინატები ჩანს დაფაზე და თქვენ უნდა დააჭიროთ შესაბამის უჯრას.";i['aSquareIsHighlightedExplanation']="უჯრები წამოწეულია დაფაზე და ტქვენ უნდა დააჭიროთ მოცემულ უჯრას.";i['averageScoreAsBlackX']=s("საშუალო ქულა შავების მხრიდან: %s");i['averageScoreAsWhiteX']=s("საშუალო ქულა თეთრების მხრიდან: %s");i['coordinates']="კოორდინატები";i['coordinateTraining']="კოორდინატების ვარჯიში";i['findSquare']="იპოვნე უჯრა";i['goAsLongAsYouWant']="გააგრძელე როგორი ტემპითაც გსურს, დრო შეუზღუდავად გაქვთ!";i['knowingTheChessBoard']="დაფის კოორდინატების ცოდნა ძალიან მნიშვნელოვანი საჭადრაკო უნარია:";i['mostChessCourses']="ჭადრაკის კურსები და სავარჯიშოები უმეტესად იყენებენ ალგებრულ ნოტაციას.";i['nameSquare']="უჯრის სახელი";i['showCoordinates']="მიჩვენე კოორდინატები";i['showPieces']="მიჩვენე ფიგურები";i['startTraining']="დაიწყე ვარჯიში";i['talkToYourChessFriends']="ეს აადვილებს ესაუბროთ თქვენს მოჭადრაკე მეგობრებს, რადგან ორივეს გესმით \\\"ჭადრაკის ენა\\\".";i['youCanAnalyseAGameMoreEffectively']="თქვენ შეგეძლებათ გააანალიზოთ პარტიები უფრო ეფექტურად, რადგან არ მოგიწევთ ყოველ ჯერზე უჯრის სახელის ძიება.";i['youHaveThirtySeconds']="თქვენ გაქვთ 30 წამი უშეცდომოთ მოძებნოტ რაც შეიძლება მეტი უჯრა!"})()