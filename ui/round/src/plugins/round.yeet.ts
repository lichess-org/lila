export function initModule(): void {
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
    '<style>cg-board:not(.clone)::before {background-image: none !important}',
  );
  document.querySelectorAll('square.last-move').forEach(x => x.remove());
  document.querySelector('.cg-shapes').remove();
  var clone = document.createElement('cg-board');
  clone.className = 'clone';
  document.getElementsByTagName('cg-container')[0].appendChild(clone);

  var pieces = document.querySelectorAll('piece:not(.ghost)');

  clone.animate([{ transform: 'rotateX(' + boardRotateX + 'deg) rotateZ(' + boardRotateZ + 'deg)' }], {
    duration: boardAnimationDurationMs,
    fill: 'forwards',
  });

  function movePieces() {
    var keepGoing = false;
    pieces.forEach(p => {
      var xTranslate = parseFloat(p.dataset.xTranslate);
      var yTranslate = parseFloat(p.dataset.yTranslate);
      var rot = parseFloat(p.dataset.rot);
      var xSpeed = parseFloat(p.dataset.xSpeed);
      var ySpeed = parseFloat(p.dataset.ySpeed);
      var rotSpeed = parseFloat(p.dataset.rotSpeed);

      newXTr = xTranslate + xSpeed;
      newYTr = yTranslate + ySpeed;

      p.dataset.xTranslate = newXTr;
      p.dataset.yTranslate = newYTr;
      p.style.translate = newXTr + 'px ' + newYTr + 'px';

      var bounds = p.getBoundingClientRect();
      var leftBounce = bounds.x <= 0;
      var topBounce = bounds.y <= 0;
      var rightBounce = bounds.right >= window.innerWidth;
      var bottomBounce = bounds.bottom >= window.innerHeight;

      if (leftBounce || topBounce || rightBounce || bottomBounce) {
        // reset position
        p.dataset.xTranslate = xTranslate;
        p.dataset.yTranslate = yTranslate;
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
      p.dataset.rot = rot;
      p.dataset.rotSpeed = rotSpeed;
      p.style.transform = p.dataset.origTransform + ' rotate(' + rot + 'rad)';

      p.dataset.xSpeed = xSpeed;
      p.dataset.ySpeed = ySpeed;

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
    p.dataset.xTranslate = 0;
    p.dataset.yTranslate = 0;
    p.dataset.rot = 0;
    var angle = Math.random() * (maxAngle - minAngle) + minAngle;
    var magnitude = Math.random() * (maxMagnitude - minMagnitude) + minMagnitude;
    p.dataset.xSpeed = Math.cos(angle) * magnitude;
    p.dataset.ySpeed = Math.sin(angle) * magnitude;
    p.dataset.rotSpeed = (Math.random() - 0.5) * 1.5;
    p.dataset.origTransform = p.style.transform;
  });

  requestAnimationFrame(movePieces);
}
