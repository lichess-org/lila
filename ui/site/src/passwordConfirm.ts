
export function addPasswordChangeListener(id2: string, id1: string): void {
  const passwordInput = document.getElementById(id1) as HTMLInputElement;
  const passwordConfirm = document.getElementById(id2) as HTMLInputElement;
  passwordConfirm.addEventListener("input", () => {
    comparePasswords(passwordConfirm.value, passwordInput.value);
  });
}

function comparePasswords(confirmPassword: string, password: string): void {
  const passwordConfirmLabel = document.querySelector(
    ".password-confirm-label"
  ) as HTMLElement;
  
  if(confirmPassword.localeCompare(password) != 0) {
    passwordConfirmLabel.textContent = "Passwords do not match";
  }
  else {
    passwordConfirmLabel.textContent = "";
  }
}
