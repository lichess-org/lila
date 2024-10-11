"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="進んだポーン";i['advancedPawnDescription']="ポーンの昇格かその狙いがテーマの問題。";i['advantage']="優位";i['advantageDescription']="決定的な優位を得てください。（評価値は 200cp 以上 600cp 以下）";i['anastasiaMate']="アナスタシアのメイト";i['anastasiaMateDescription']="盤端と味方の駒にはさまれたキングを、ナイトとルーク（またはクイーン）でメイトする形。";i['arabianMate']="アラビアのメイト";i['arabianMateDescription']="盤の隅にいるキングを、ナイトとルークでメイトする形。";i['attackingF2F7']="f2/f7への攻撃";i['attackingF2F7Description']="f2 か f7 のポーンを狙う攻撃（フライド・リバー・アタックなど）。";i['attraction']="アトラクション（引き寄せ）";i['attractionDescription']="交換か捨て駒によって相手の駒を不利なマスに誘い込む。";i['backRankMate']="バックランク・メイト";i['backRankMateDescription']="一番下のランクで、上を自分の駒に塞がれたキングをメイトする。";i['bishopEndgame']="ビショップ・エンドゲーム";i['bishopEndgameDescription']="ビショップとポーンだけの終盤。";i['bodenMate']="ボーデンのメイト";i['bodenMateDescription']="味方の駒にじゃまされたキングを、2 個のビショップが交差した効き筋でメイトする形。";i['capturingDefender']="守り駒の除去";i['capturingDefenderDescription']="別の駒を守っている駒を消して、無防備になった駒を取る。";i['castling']="キャスリング";i['castlingDescription']="キングを安全にし、ルークを攻撃に活用する。";i['clearance']="クリアランス（解放）";i['clearanceDescription']="次のタクティクスのためにマス、ファイル、斜線を開く手（先手であることが多い）。";i['crushing']="圧倒";i['crushingDescription']="相手の悪手をとがめて圧倒的な優位を築きます。（評価値は 600cp 以上）";i['defensiveMove']="守り";i['defensiveMoveDescription']="駒損などの不利を避けるために必要な正確な手または手順。";i['deflection']="ディフレクション（そらし）";i['deflectionDescription']="相手の駒を別の役割（重要なマスを守るなど）からそらす手。";i['discoveredAttack']="ディスカバード・アタック";i['discoveredAttackDescription']="別のラインピースの効きを止めていた駒を動かす手。たとえばルークの効き筋からナイトを動かす。";i['doubleBishopMate']="ダブル・ビショップのメイト";i['doubleBishopMateDescription']="味方の駒にじゃまされたキングを、2 個のビショップが平行な効き筋でメイトする形。";i['doubleCheck']="ダブル・チェック";i['doubleCheckDescription']="ディスカバード・アタックによって、動いた駒とラインピースが相手のキングに同時にチェックをかける手。";i['dovetailMate']="燕尾のメイト";i['dovetailMateDescription']="斜め後ろを両方とも味方の駒に塞がれたキングを、クイーン1個でメイトする形。";i['endgame']="エンドゲーム";i['endgameDescription']="ゲームの終盤でのタクティクス。";i['enPassantDescription']="アンパッサン、つまり相手の 2 マス前進したポーンを途中で取る手を含むタクティクス。";i['equality']="互角";i['equalityDescription']="劣勢の局面から、ドローを確保するか互角の局面に戻します。（評価値は 200cp 以下）";i['exposedKing']="危険なキング";i['exposedKingDescription']="守り駒の少ないキングを攻める問題、多くの場合メイトにつながる。";i['fork']="フォーク（両取り）";i['forkDescription']="動いた駒が相手の 2 つの駒を同時に攻撃する手。";i['hangingPiece']="浮き駒";i['hangingPieceDescription']="守りのない駒、または守り駒の足りない駒をただで取る問題。";i['healthyMix']="混合";i['healthyMixDescription']="いろいろな問題を少しずつ。どんな問題が来るかわからないので油断しないで！　実戦と同じです。";i['hookMate']="釣り針のメイト";i['hookMateDescription']="ポーンの隣にいるキングを、ルーク、ナイト、ポーンでメイトする形。";i['interference']="インターフェア（干渉）";i['interferenceDescription']="相手の 2 つの駒の間に駒を入れて浮き駒を作る問題。相手の 2 個のルークの間に守られたナイトを入れる、など。";i['intermezzo']="ツヴィッシェンツーク（利かし）";i['intermezzoDescription']="当然に見える手を指す代わりに、いったん相手が受けざるを得ない別の手をはさむ問題。";i['kingsideAttack']="キングサイド攻撃";i['kingsideAttackDescription']="キングサイドにキャスリングした相手のキングを攻撃する。";i['knightEndgame']="ナイト・エンドゲーム";i['knightEndgameDescription']="ナイトとポーンだけの終盤。";i['long']="長手数問題";i['longDescription']="3 手で勝ちになります。";i['master']="マスターのゲーム";i['masterDescription']="タイトル保持者の対局から採った問題。";i['masterVsMaster']="マスター同士のゲーム";i['masterVsMasterDescription']="双方がタイトル保持者の対局から採った問題。";i['mate']="メイト";i['mateDescription']="きれいに勝ちを決める。";i['mateIn1']="1 手メイト";i['mateIn1Description']="1 手でチェックメイトにします。";i['mateIn2']="2 手メイト";i['mateIn2Description']="2 手でチェックメイトにします。";i['mateIn3']="3 手メイト";i['mateIn3Description']="3 手でチェックメイトにします。";i['mateIn4']="4 手メイト";i['mateIn4Description']="4 手でチェックメイトにします。";i['mateIn5']="5 手以上メイト";i['mateIn5Description']="長いメイトの手順を考える。";i['middlegame']="ミドルゲーム";i['middlegameDescription']="ゲームの中盤でのタクティクス。";i['oneMove']="1 手問題";i['oneMoveDescription']="1 手だけ動かす問題。";i['opening']="オープニング";i['openingDescription']="ゲームの序盤でのタクティクス。";i['pawnEndgame']="ポーン・エンドゲーム";i['pawnEndgameDescription']="ポーンだけの終盤。";i['pin']="ピン";i['pinDescription']="ピンを含む問題。ラインピースに狙われた駒が動くと、より価値の高い駒が取られてしまう状況。";i['playerGames']="プレイヤーの対局";i['playerGamesDescription']="自分の対局、他のプレイヤーの対局から取られた問題を検索します。";i['promotion']="プロモーション";i['promotionDescription']="ポーンの昇格かその狙いがテーマの問題。";i['puzzleDownloadInformation']=s("これらの問題はパブリックドメインにあり、%s でダウンロードできます。");i['queenEndgame']="クイーン・エンドゲーム";i['queenEndgameDescription']="クイーンとポーンだけの終盤。";i['queenRookEndgame']="クイーンとルーク";i['queenRookEndgameDescription']="クイーン、ルーク、ポーンだけの終盤。";i['queensideAttack']="クイーンサイド攻撃";i['queensideAttackDescription']="クイーンサイドにキャスリングした相手のキングを攻撃する。";i['quietMove']="静かな手";i['quietMoveDescription']="チェックでも駒取りでもないが、次に防げない狙いを用意する手。";i['rookEndgame']="ルーク・エンドゲーム";i['rookEndgameDescription']="ルークとポーンだけの終盤。";i['sacrifice']="サクリファイス（捨て駒）";i['sacrificeDescription']="短期的な駒損を含み、強制的な手順の後に優位を築く問題。";i['short']="短手数問題";i['shortDescription']="2 手で勝ちになります。";i['skewer']="スキュアー（串刺し）";i['skewerDescription']="ラインピースで価値の高い駒を攻撃し、それが逃げた後で背後にある価値の低い駒を取るタクティクス。ピンの裏返し。";i['smotheredMate']="スマザード・メイト";i['smotheredMateDescription']="キングが味方の駒に囲まれて動けない時（スマザー＝窒息している時）に、ナイト 1 個でかけるメイト。";i['superGM']="スーパー GM の対局";i['superGMDescription']="世界の一流選手の対局から採った問題。";i['trappedPiece']="敵駒を殺す";i['trappedPieceDescription']="相手の駒の動きを制限して、逃げられない状態にする問題。";i['underPromotion']="アンダープロモーション";i['underPromotionDescription']="ナイト、ビショップ、ルークへの昇格。";i['veryLong']="超長手数問題";i['veryLongDescription']="4 手以上で勝ちになります。";i['xRayAttack']="Ｘ線攻撃";i['xRayAttackDescription']="相手の駒の向こうにあるマスを間接的に攻撃（または防御）する。";i['zugzwang']="ツークツワンク";i['zugzwangDescription']="相手の指せる手が、どれを選んでも局面を悪くしてしまう形。"})()