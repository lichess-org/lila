const app = document.querySelector<HTMLDivElement>("#app")!;

app.innerHTML = `
  <div class="shell">
    <header><div class="brand">♞ CHESS PLATFORM</div><nav><a>Play</a><a>Players</a><a>Tournaments</a></nav></header>
    <main class="hero">
      <section><span class="eyebrow">REALTIME CHESS</span><h1>Play chess.<br><b>Improve every game.</b></h1>
      <p>Fast online games, ratings, history, analysis and competition in one focused chess platform.</p>
      <button id="play">Start a game</button>
      <div id="result"></div></section>
      <div class="board">${Array.from({length:64},(_,i)=>`<i class="${(Math.floor(i/8)+i)%2?'dark':'light'}"></i>`).join("")}</div>
    </main>
  </div>`;

const css = document.createElement("style");
css.textContent = `
:root{font-family:Inter,system-ui,sans-serif;background:#0b0e14;color:#f7f8fa}*{box-sizing:border-box}body{margin:0}.shell{max-width:1280px;margin:auto;padding:24px 32px}header{height:64px;display:flex;align-items:center;justify-content:space-between}.brand{font-weight:900;letter-spacing:.08em}nav{display:flex;gap:28px;color:#aeb5c2}nav a{cursor:pointer}.hero{min-height:calc(100vh - 110px);display:grid;grid-template-columns:1fr 560px;gap:70px;align-items:center}.eyebrow{font-size:12px;letter-spacing:.25em;color:#8e98a8}h1{font-size:clamp(52px,7vw,92px);line-height:.95;margin:20px 0}h1 b{font-weight:800}p{max-width:560px;color:#aeb5c2;font-size:18px;line-height:1.7}button{margin-top:20px;border:0;border-radius:12px;background:#fff;color:#101319;padding:15px 22px;font-weight:800;font-size:15px;cursor:pointer}.board{aspect-ratio:1;display:grid;grid-template-columns:repeat(8,1fr);border-radius:14px;overflow:hidden;box-shadow:0 35px 100px #0009}.board i.light{background:#e7ebef}.board i.dark{background:#667386}#result{margin-top:16px;color:#9fa8b7}@media(max-width:900px){.hero{grid-template-columns:1fr;gap:32px}.board{max-width:560px;width:100%;margin:auto}nav{display:none}}`;
document.head.append(css);

document.querySelector<HTMLButtonElement>("#play")!.onclick = async () => {
  const result = document.querySelector<HTMLDivElement>("#result")!;
  try {
    const res = await fetch("http://localhost:8080/api/games",{method:"POST"});
    const game = await res.json();
    result.textContent = `Game ${game.id.slice(0,8)} created — ready for realtime play.`;
  } catch {
    result.textContent = "Start the chess server on port 8080 first.";
  }
};
