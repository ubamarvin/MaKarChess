export function createUiBindings() {
  const elements = {
    connectionStatus: document.getElementById("connection-status"),
    authStatus: document.getElementById("auth-status"),
    authEmailInput: document.getElementById("auth-email-input"),
    authPasswordInput: document.getElementById("auth-password-input"),
    signInButton: document.getElementById("sign-in-button"),
    signUpButton: document.getElementById("sign-up-button"),
    googleSignInButton: document.getElementById("google-sign-in-button"),
    signOutButton: document.getElementById("sign-out-button"),
    botType: document.getElementById("bot-type"),
    botPlays: document.getElementById("bot-plays"),
    modeledSide: document.getElementById("modeled-side"),
    newGameButton: document.getElementById("new-game-button"),
    newGameModal: document.getElementById("new-game-modal"),
    closeNewGameModalButton: document.getElementById("close-new-game-modal-button"),
    cancelNewGameButton: document.getElementById("cancel-new-game-button"),
    confirmNewGameButton: document.getElementById("confirm-new-game-button"),
    resetGameButton: document.getElementById("reset-game-button"),
    uciInput: document.getElementById("uci-input"),
    submitMoveButton: document.getElementById("submit-move-button"),
    importInput: document.getElementById("import-input"),
    loadFenButton: document.getElementById("load-fen-button"),
    loadPgnButton: document.getElementById("load-pgn-button"),
    replayBackButton: document.getElementById("replay-back-button"),
    replayForwardButton: document.getElementById("replay-forward-button"),
    sideToMove: document.getElementById("side-to-move"),
    phaseTag: document.getElementById("phase-tag"),
    phaseWinner: document.getElementById("phase-winner"),
    isCheck: document.getElementById("is-check"),
    statusLine: document.getElementById("status-line"),
    currentPlayerLine: document.getElementById("current-player-line"),
    replayStatus: document.getElementById("replay-status"),
    selectedSquare: document.getElementById("selected-square"),
    infoMessage: document.getElementById("info-message"),
    errorMessage: document.getElementById("error-message")
  };

  function readNewGameConfig() {
    const config = {};
    if (elements.botType.value) {
      config.botType = elements.botType.value;
    }
    if (elements.botPlays.value) {
      config.botPlays = elements.botPlays.value;
    }
    if (elements.modeledSide.value) {
      config.modeledSide = elements.modeledSide.value;
    }
    return Object.keys(config).length === 0 ? null : config;
  }

  function setConnectionStatus(text) {
    elements.connectionStatus.textContent = text;
  }

  function setAuthStatus(text) {
    elements.authStatus.textContent = text;
  }

  function readAuthCredentials() {
    return {
      email: elements.authEmailInput.value.trim(),
      password: elements.authPasswordInput.value
    };
  }

  function clearAuthPassword() {
    elements.authPasswordInput.value = "";
  }

  function clearAuthInputs() {
    elements.authEmailInput.value = "";
    elements.authPasswordInput.value = "";
  }

  function setAuthControlsEnabled(enabled, signedIn = false) {
    const disabled = !enabled;
    elements.authEmailInput.disabled = disabled || signedIn;
    elements.authPasswordInput.disabled = disabled || signedIn;
    elements.signInButton.disabled = disabled || signedIn;
    elements.signUpButton.disabled = disabled || signedIn;
    elements.googleSignInButton.disabled = disabled || signedIn;
    elements.signOutButton.disabled = disabled || !signedIn;
  }

  function setChessControlsEnabled(enabled) {
    const disabled = !enabled;
    elements.newGameButton.disabled = disabled;
    elements.resetGameButton.disabled = disabled;
    elements.closeNewGameModalButton.disabled = disabled;
    elements.cancelNewGameButton.disabled = disabled;
    elements.confirmNewGameButton.disabled = disabled;
    if (elements.submitMoveButton) {
      elements.submitMoveButton.disabled = disabled;
    }
    elements.loadFenButton.disabled = disabled;
    elements.loadPgnButton.disabled = disabled;
    if (elements.uciInput) {
      elements.uciInput.disabled = disabled;
    }
    elements.importInput.disabled = disabled;
    elements.botType.disabled = disabled;
    elements.botPlays.disabled = disabled;
    elements.modeledSide.disabled = disabled;
    elements.replayBackButton.disabled = true;
    elements.replayForwardButton.disabled = true;
  }

  function setInfoMessage(text) {
    elements.infoMessage.textContent = text;
    elements.infoMessage.classList.remove("hidden");
  }

  function clearInfoMessage() {
    elements.infoMessage.classList.add("hidden");
    elements.infoMessage.textContent = "";
  }

  function setErrorMessage(text) {
    elements.errorMessage.textContent = text;
    elements.errorMessage.classList.remove("hidden");
  }

  function clearErrorMessage() {
    elements.errorMessage.textContent = "";
    elements.errorMessage.classList.add("hidden");
  }

  function setSelectedSquare(text) {
    elements.selectedSquare.textContent = text || "-";
  }

  function openNewGameModal() {
    elements.newGameModal.classList.remove("hidden");
    elements.newGameModal.setAttribute("aria-hidden", "false");
  }

  function closeNewGameModal() {
    elements.newGameModal.classList.add("hidden");
    elements.newGameModal.setAttribute("aria-hidden", "true");
  }

  function setLoading(isLoading) {
    const disabled = Boolean(isLoading);
    const signedIn = elements.signOutButton.disabled === false;
    setAuthControlsEnabled(!disabled, signedIn);
    setChessControlsEnabled(!disabled && signedIn);
    if (disabled) {
      elements.replayBackButton.disabled = true;
      elements.replayForwardButton.disabled = true;
    }
  }

  function renderStatus(status, boardState = null) {
    elements.sideToMove.textContent = status?.sideToMove || boardState?.sideToMove || "-";
    elements.phaseTag.textContent = status?.phase?.tag || boardState?.phase?.tag || "-";
    elements.phaseWinner.textContent = status?.phase?.winner || boardState?.phase?.winner || "-";
    const checkValue = status?.isCheck ?? boardState?.isCheck;
    elements.isCheck.textContent = typeof checkValue === "boolean" ? String(checkValue) : "-";
    elements.statusLine.textContent = status?.statusLine || "-";
    elements.currentPlayerLine.textContent = status?.currentPlayerLine || "-";
  }

  function renderReplayStatus(replay) {
    if (!replay?.active) {
      elements.replayStatus.textContent = "inactive";
      elements.replayBackButton.disabled = true;
      elements.replayForwardButton.disabled = true;
      return;
    }

    const index = replay.index ?? 0;
    const length = replay.length ?? 0;
    elements.replayStatus.textContent = `${index}/${length}`;
    elements.replayBackButton.disabled = index <= 0;
    elements.replayForwardButton.disabled = index >= length;
  }

  return {
    elements,
    readNewGameConfig,
    setConnectionStatus,
    setAuthStatus,
    readAuthCredentials,
    clearAuthPassword,
    clearAuthInputs,
    setAuthControlsEnabled,
    setChessControlsEnabled,
    setInfoMessage,
    clearInfoMessage,
    setErrorMessage,
    clearErrorMessage,
    setSelectedSquare,
    openNewGameModal,
    closeNewGameModal,
    setLoading,
    renderStatus,
    renderReplayStatus
  };
}
