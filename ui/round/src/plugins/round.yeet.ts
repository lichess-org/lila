/* This is lazily imported from JS:
 * https://gist.github.com/thomas-daniels/4a53ba9e08864e65b2e501c4a8c2ec7e
 * The following code does not follow our quality standards and is very poorly typed.
 * It's just a joke after all. */
export async function initModule(): Promise<void> {
  await site.sound.load('yeet', site.asset.url('sound/other/yeet.mp3'));
  site.sound.play('yeet');
  const gravity = 1.5; // higher -> pieces fall down faster
  const frictionMultiplier = 0.98; // lower -> more friction
  var minAngle = -0.436; // min angle for starting velocity
  var maxAngle = 3.576; // max angle for starting velocity
  var minMagnitude = 250; // min magnitude for starting velocity
  var maxMagnitude = 500; // max magnitude for starting velocity
  var boardRotateX = 445; // degrees
  var boardRotateZ = 15;
  var boardAnimationDurationMs = 750;

  document.head.insertAdjacentHTML(
    'beforeend',
    `<style>
    .main-board cg-board { box-shadow: none !important; }
    .main-board cg-board:not(.clone)::before {background-image: none !important}
</style>`,
  );
  $('.main-board square.last-move').remove();
  $('.main-board .cg-shapes').remove();
  $('.main-board coords').remove();
  var clone = document.createElement('cg-board');
  clone.className = 'clone';
  document.getElementsByTagName('cg-container')[0].appendChild(clone);

  var pieces = document.querySelectorAll<HTMLElement>('piece:not(.ghost)');

  clone.animate([{ transform: 'rotateX(' + boardRotateX + 'deg) rotateZ(' + boardRotateZ + 'deg)' }], {
    duration: boardAnimationDurationMs,
    fill: 'forwards',
  });

  function movePieces() {
    var keepGoing = false;
    pieces.forEach(p => {
      const d = p.dataset as any;
      var xTranslate = parseFloat(d.xTranslate);
      var yTranslate = parseFloat(d.yTranslate);
      var rot = parseFloat(d.rot);
      var xSpeed = parseFloat(d.xSpeed);
      var ySpeed = parseFloat(d.ySpeed);
      var rotSpeed = parseFloat(d.rotSpeed);

      let newXTr = xTranslate + xSpeed;
      let newYTr = yTranslate + ySpeed;

      d.xTranslate = newXTr;
      d.yTranslate = newYTr;
      p.style.translate = newXTr + 'px ' + newYTr + 'px';

      var bounds = p.getBoundingClientRect();
      var leftBounce = bounds.x <= 0;
      var topBounce = bounds.y <= 0;
      var rightBounce = bounds.right >= window.innerWidth;
      var bottomBounce = bounds.bottom >= window.innerHeight;

      if (leftBounce || topBounce || rightBounce || bottomBounce) {
        // reset position
        d.xTranslate = xTranslate;
        d.yTranslate = yTranslate;
        p.style.translate = xTranslate + 'px ' + yTranslate + 'px';
      }

      if (leftBounce || rightBounce) {
        xSpeed *= -1;
      } else if (topBounce || bottomBounce) {
        ySpeed *= -1;
      }

      ySpeed += gravity;
      xSpeed *= frictionMultiplier;
      ySpeed *= frictionMultiplier;

      rot += rotSpeed;
      rotSpeed *= frictionMultiplier;
      d.rot = rot;
      d.rotSpeed = rotSpeed;
      p.style.transform = p.dataset.origTransform + ' rotate(' + rot + 'rad)';

      d.xSpeed = xSpeed;
      d.ySpeed = ySpeed;

      if (
        (Math.abs(xSpeed) > 0.5 && Math.abs(ySpeed) > 0.5) ||
        window.innerHeight - p.getBoundingClientRect().bottom > 10
      ) {
        keepGoing = true;
      }
    });
    if (keepGoing) {
      requestAnimationFrame(movePieces);
    }
  }

  pieces.forEach(p => {
    const d = p.dataset as any;
    d.xTranslate = 0;
    d.yTranslate = 0;
    d.rot = 0;
    var angle = Math.random() * (maxAngle - minAngle) + minAngle;
    var magnitude = Math.random() * (maxMagnitude - minMagnitude) + minMagnitude;
    d.xSpeed = Math.cos(angle) * magnitude;
    d.ySpeed = Math.sin(angle) * magnitude;
    d.rotSpeed = (Math.random() - 0.5) * 1.5;
    d.origTransform = p.style.transform;
  });

  requestAnimationFrame(movePieces);
}
