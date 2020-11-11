import zxcvbn from "zxcvbn";

interface PasswordFeedback {
  suggestions: string[];
  warning: string;
}

export function addPasswordChangeListener(id: string): void {
  const passwordInput = document.getElementById(id) as HTMLInputElement;
  passwordInput.addEventListener("input", () => {
    updatePasswordComplexityMeter(passwordInput.value);
  });
  // Update the meter if script loaded after user has already typed something
  if (passwordInput?.value){
    updatePasswordComplexityMeter(passwordInput.value);
  }
}

function updatePasswordComplexityMeter(password: string): void {
  const analysis = zxcvbn(password);
  updateMeter(analysis.score);
  updateLabel(password.length, analysis.score, analysis.feedback);
}

function updateMeter(score: number): void {
  const color =
    score > 3 ? "green" : score > 2 ? "#ffc800" : score > 1 ? "#ff8000" : "red";
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

function updateLabel(
  passwordLength: number,
  score: number,
  feedback: PasswordFeedback
): void {
  const suggestionLabel = document.querySelector(
    ".password-complexity-label"
  ) as HTMLElement;

  const suggestion = feedback.warning || feedback.suggestions[0];

  if (passwordLength < 4) {
    suggestionLabel.textContent = "Password must be at least four characters.";
  } else if (suggestion) {
    suggestionLabel.textContent = suggestion.endsWith(".")
      ? suggestion
      : suggestion + ".";
  } else {
    // fallback strings in case the suggestion is not included in zxcvbn feedback
    switch (score) {
      case 0:
      case 1:
        suggestionLabel.textContent = "Password is short and easy to guess.";
        break;
      case 2:
        suggestionLabel.textContent =
          "Somewhat guessable, but still susceptible to being cracked.";
        break;
      case 3:
        suggestionLabel.textContent =
          "Decent password, would require many guesses to crack.";
        break;
      case 4:
        suggestionLabel.textContent = "Strong and secure password!";
        break;
      default:
        suggestionLabel.textContent = "";
    }
  }
}
