"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.coordinates)window.i18n.coordinates={};let i=window.i18n.coordinates;i['aCoordinateAppears']="ボード上にマスの名前が表示されるので、そのマスをクリックしてください。";i['aSquareIsHighlightedExplanation']="ボード上にハイライト表示されるマスの座標（たとえば「e4」）を入力してください。";i['averageScoreAsBlackX']=s("黒番での平均スコア: %s");i['averageScoreAsWhiteX']=s("白番での平均スコア: %s");i['coordinates']="マスの位置";i['coordinateTraining']="座標を読むトレーニング";i['findSquare']="マスを探す";i['goAsLongAsYouWant']="じっくりどうぞ、時間制限なしです！";i['knowingTheChessBoard']="チェス盤の座標がすぐわかるのは非常にだいじなスキルです。";i['mostChessCourses']="チェスの講座、問題はほとんど「代数式」という記譜法を使っています。";i['nameSquare']="マスの名前を入力";i['showCoordinates']="座標を表示";i['showCoordsOnAllSquares']="すべてのマスに座標を表示";i['showPieces']="駒を表示";i['startTraining']="トレーニングを開始";i['talkToYourChessFriends']="またチェスの友人と話をするのも簡単になります。双方がいわば「チェス語」を理解しているからです。";i['youCanAnalyseAGameMoreEffectively']="このマスはどこか、いちいち探すことがなくなれば、ゲームの検討も効率よくできます。";i['youHaveThirtySeconds']="30 秒の間にできるだけ多くのマスを正しくクリック！"})()