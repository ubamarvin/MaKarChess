export function createUiBindings() {
  const elements = {
    connectionStatus: document.getElementById("connection-status"),
    botType: document.getElementById("bot-type"),
    botPlays: document.getElementById("bot-plays"),
    modeledSide: document.getElementById("modeled-side"),
    newGameButton: document.getElementById("new-game-button"),
    resetGameButton: document.getElementById("reset-game-button"),
    refreshButton: document.getElementById("refresh-button"),
    uciInput: document.getElementById("uci-input"),
    submitMoveButton: document.getElementById("submit-move-button"),
    sideToMove: document.getElementById("side-to-move"),
    phaseTag: document.getElementById("phase-tag"),
    phaseWinner: document.getElementById("phase-winner"),
    isCheck: document.getElementById("is-check"),
    statusLine: document.getElementById("status-line"),
    currentPlayerLine: document.getElementById("current-player-line"),
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

  function setLoading(isLoading) {
    const disabled = Boolean(isLoading);
    elements.newGameButton.disabled = disabled;
    elements.resetGameButton.disabled = disabled;
    elements.refreshButton.disabled = disabled;
    elements.submitMoveButton.disabled = disabled;
    elements.uciInput.disabled = disabled;
    elements.botType.disabled = disabled;
    elements.botPlays.disabled = disabled;
    elements.modeledSide.disabled = disabled;
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

  return {
    elements,
    readNewGameConfig,
    setConnectionStatus,
    setInfoMessage,
    clearInfoMessage,
    setErrorMessage,
    clearErrorMessage,
    setSelectedSquare,
    setLoading,
    renderStatus
  };
}
