


export function addPasswordChangeListener(id: string) {
  const passwordInput = document.getElementById(id) as HTMLInputElement;
  passwordInput.addEventListener('input', () => {updatePasswordComplexityMeter(passwordInput.value)});
}

function updatePasswordComplexityMeter(password: string): void {
  const analysis = window.zxcvbn(password);
  updateMeter(analysis.score);
  updateLabel(password.length, analysis.score);
}


function updateMeter(score: number): void {
  const color =
    score > 3 ? "green" : score > 2 ? "yellow" : score > 1 ? "orange" : "red";
  const meter = document.querySelector(".password-complexity-meter");
  const children = meter?.children || [];

  for (var i = 0; i < children.length; i++) {
    if (i < score) {
      (children[i] as HTMLElement).style.backgroundColor = color;
    } else {
      (children[i] as HTMLElement).style.backgroundColor = "";
    }
  }
}

function updateLabel(passwordLength: number, score: number):void {
  const suggestionLabel = document.querySelector(
    ".password-complexity-label"
  ) as HTMLElement;

  if (passwordLength < 4) {
    suggestionLabel.textContent = "Password must be at least four characters.";
  } else {
    switch (score) {
      case 0:
        suggestionLabel.textContent = "Password is short and easy to guess."
        break;
      case 1:
        suggestionLabel.textContent = "Very weak password.";
        break;
      case 2:
        suggestionLabel.textContent = "Weak password.";
        break;
      case 3:
        suggestionLabel.textContent = "Decent password.";
        break;
      case 4:
        suggestionLabel.textContent = "Strong password!";
        break;
    }
  }
}
