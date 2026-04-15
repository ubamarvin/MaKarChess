export function createUiBindings() {
  const elements = {
    connectionStatus: document.getElementById("connection-status"),
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
    fenInput: document.getElementById("fen-input"),
    pgnInput: document.getElementById("pgn-input"),
    loadFenButton: document.getElementById("load-fen-button"),
    loadPgnButton: document.getElementById("load-pgn-button"),
    clearPgnInputButton: document.getElementById("clear-pgn-input-button"),
    currentPgnOutput: document.getElementById("current-pgn-output"),
    copyPgnButton: document.getElementById("copy-pgn-button"),
    savePgnButton: document.getElementById("save-pgn-button"),
    moveHistoryOutput: document.getElementById("move-history-output"),
    replayStartButton: document.getElementById("replay-start-button"),
    replayBackButton: document.getElementById("replay-back-button"),
    replayPlayButton: document.getElementById("replay-play-button"),
    replayForwardButton: document.getElementById("replay-forward-button"),
    replayEndButton: document.getElementById("replay-end-button"),
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

  function setReplayPlaying(isPlaying) {
    elements.replayPlayButton.textContent = isPlaying ? "Pause" : "Replay";
  }

  function setPgnDraft(text) {
    elements.pgnInput.value = text || "";
  }

  function setCurrentPgn(text) {
    elements.currentPgnOutput.value = text || "";
  }

  function setMoveHistory(moves) {
    elements.moveHistoryOutput.value = Array.isArray(moves)
      ? moves
          .reduce((lines, move, index) => {
            if (index % 2 === 0) {
              lines.push(`${Math.floor(index / 2) + 1}. ${move}`);
            } else {
              lines[lines.length - 1] = `${lines[lines.length - 1]} ${move}`;
            }
            return lines;
          }, [])
          .join("\n")
      : "";
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
    elements.newGameButton.disabled = disabled;
    elements.closeNewGameModalButton.disabled = disabled;
    elements.cancelNewGameButton.disabled = disabled;
    elements.confirmNewGameButton.disabled = disabled;
    elements.resetGameButton.disabled = disabled;
    elements.submitMoveButton.disabled = disabled;
    elements.loadFenButton.disabled = disabled;
    elements.loadPgnButton.disabled = disabled;
    elements.clearPgnInputButton.disabled = disabled;
    elements.copyPgnButton.disabled = disabled || !elements.currentPgnOutput.value.trim();
    elements.savePgnButton.disabled = disabled || !elements.currentPgnOutput.value.trim();
    elements.uciInput.disabled = disabled;
    elements.fenInput.disabled = disabled;
    elements.pgnInput.disabled = disabled;
    elements.botType.disabled = disabled;
    elements.botPlays.disabled = disabled;
    elements.modeledSide.disabled = disabled;

    if (disabled) {
      elements.replayStartButton.disabled = true;
      elements.replayBackButton.disabled = true;
      elements.replayPlayButton.disabled = true;
      elements.replayForwardButton.disabled = true;
      elements.replayEndButton.disabled = true;
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
    setCurrentPgn(status?.currentPgn || "");
    setMoveHistory(status?.moveHistory || []);
    const hasCurrentPgn = Boolean((status?.currentPgn || "").trim());
    elements.copyPgnButton.disabled = !hasCurrentPgn;
    elements.savePgnButton.disabled = !hasCurrentPgn;
  }

  function renderReplayStatus(replay, isPlaying = false) {
    setReplayPlaying(isPlaying);

    if (!replay?.active) {
      elements.replayStatus.textContent = "inactive";
      elements.replayStartButton.disabled = true;
      elements.replayBackButton.disabled = true;
      elements.replayPlayButton.disabled = true;
      elements.replayForwardButton.disabled = true;
      elements.replayEndButton.disabled = true;
      return;
    }

    const index = replay.index ?? 0;
    const length = replay.length ?? 0;
    const atStart = index <= 0;
    const atEnd = index >= length;

    elements.replayStatus.textContent = `${index}/${length}`;
    elements.replayStartButton.disabled = atStart;
    elements.replayBackButton.disabled = atStart;
    elements.replayPlayButton.disabled = atEnd;
    elements.replayForwardButton.disabled = atEnd;
    elements.replayEndButton.disabled = atEnd;
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
    setReplayPlaying,
    setPgnDraft,
    setCurrentPgn,
    setMoveHistory,
    openNewGameModal,
    closeNewGameModal,
    setLoading,
    renderStatus,
    renderReplayStatus
  };
}
