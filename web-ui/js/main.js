import {
  health,
  newGame,
  resetGame,
  getBoard,
  getStatus,
  getReplayStatus,
  loadFen,
  loadPgnReplay,
  replayStart,
  replayForward,
  replayBackward,
  replayEnd,
  makeMove,
  ApiClientError
} from "./api.js";
import { createScene } from "./scene.js";
import { createBoard, clearHighlights } from "./board.js";
import { clearPieces, renderPieces } from "./pieces.js";
import { createUiBindings } from "./ui.js";
import { createInteractionController } from "./interaction.js";

const PGN_DRAFT_STORAGE_KEY = "makarchess-pgn-draft";
const FEN_DRAFT_STORAGE_KEY = "makarchess-fen-draft";

const state = {
  currentBoardResponse: null,
  currentStatusResponse: null,
  currentReplayResponse: null,
  loading: false,
  gameStarted: false,
  replayAutoplayActive: false,
  replayAutoplayToken: 0
};

const ui = createUiBindings();
const sceneRoot = createScene(document.getElementById("board-container"));
createBoard(sceneRoot.scene);
clearPieces(sceneRoot.scene);
sceneRoot.start();

restoreDrafts();

function renderBoardState(boardState) {
  state.currentBoardResponse = boardState;
  renderPieces(sceneRoot.scene, boardState);
}

function renderStatus(status = state.currentStatusResponse, boardState = state.currentBoardResponse) {
  state.currentStatusResponse = status;
  ui.renderStatus(status, boardState);
}

function renderReplayStatus(replay = state.currentReplayResponse) {
  state.currentReplayResponse = replay;
  ui.renderReplayStatus(replay, state.replayAutoplayActive);
}

function persistDraft(storageKey, value) {
  try {
    window.localStorage.setItem(storageKey, value);
  } catch {
    // Ignore storage failures so the app keeps working in strict browser modes.
  }
}

function restoreDrafts() {
  try {
    ui.setPgnDraft(window.localStorage.getItem(PGN_DRAFT_STORAGE_KEY) || "");
    ui.elements.fenInput.value = window.localStorage.getItem(FEN_DRAFT_STORAGE_KEY) || "";
  } catch {
    ui.setPgnDraft("");
    ui.elements.fenInput.value = "";
  }
}

function downloadPgnFile(text) {
  const blob = new Blob([text], { type: "application/x-chess-pgn;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  anchor.href = url;
  anchor.download = `makarchess-replay-${timestamp}.pgn`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function wait(ms) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

function stopReplayAutoplay({ keepMessage = true } = {}) {
  const wasPlaying = state.replayAutoplayActive;
  state.replayAutoplayActive = false;
  state.replayAutoplayToken += 1;
  renderReplayStatus();

  if (wasPlaying && !keepMessage) {
    ui.setInfoMessage("Replay paused.");
  }
}

async function refreshBoardContext(boardState) {
  renderBoardState(boardState);
  const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
  renderStatus(status, boardState);
  renderReplayStatus(replay);
  clearHighlights();
  interaction.enable();
  state.gameStarted = true;
}

async function runWithUiFeedback(action, infoMessage = "") {
  ui.clearErrorMessage();
  if (infoMessage) {
    ui.setInfoMessage(infoMessage);
  }
  ui.setLoading(true);
  state.loading = true;

  try {
    await action();
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : "Unexpected frontend error.";
    ui.setErrorMessage(message);
  } finally {
    ui.setLoading(false);
    state.loading = false;
    renderReplayStatus();
  }
}

async function handleMoveRequest(uci) {
  stopReplayAutoplay();
  await runWithUiFeedback(async () => {
    const boardState = await makeMove(uci);
    await refreshBoardContext(boardState);
    ui.elements.uciInput.value = "";
    ui.setInfoMessage(`Move applied: ${uci}`);
  }, `Submitting move ${uci}...`);
}

async function applyReplayAction(request, loadingMessage, successMessage) {
  stopReplayAutoplay();
  await runWithUiFeedback(async () => {
    const boardState = await request();
    await refreshBoardContext(boardState);
    ui.setInfoMessage(successMessage);
  }, loadingMessage);
}

async function startReplayAutoplay() {
  if (!state.currentReplayResponse?.active) {
    ui.setErrorMessage("Load a PGN replay first.");
    return;
  }

  if ((state.currentReplayResponse.index ?? 0) >= (state.currentReplayResponse.length ?? 0)) {
    ui.setInfoMessage("Replay is already at the end.");
    return;
  }

  state.replayAutoplayActive = true;
  const token = ++state.replayAutoplayToken;
  renderReplayStatus();
  ui.clearErrorMessage();
  ui.setInfoMessage("Replay is playing...");

  while (state.replayAutoplayActive && token === state.replayAutoplayToken) {
    const replay = state.currentReplayResponse;
    if (!replay?.active || (replay.index ?? 0) >= (replay.length ?? 0)) {
      break;
    }

    try {
      const boardState = await replayForward();
      await refreshBoardContext(boardState);
    } catch (error) {
      const message = error instanceof ApiClientError ? error.message : "Replay could not continue.";
      ui.setErrorMessage(message);
      break;
    }

    await wait(700);
  }

  const finishedAtEnd =
    (state.currentReplayResponse?.index ?? 0) >= (state.currentReplayResponse?.length ?? 0);
  const shouldAnnounceFinish = state.replayAutoplayActive && finishedAtEnd;

  state.replayAutoplayActive = false;
  renderReplayStatus();

  if (shouldAnnounceFinish) {
    ui.setInfoMessage("Replay finished.");
  }
}

const interaction = createInteractionController({
  renderer: sceneRoot.renderer,
  camera: sceneRoot.camera,
  scene: sceneRoot.scene,
  ui,
  onMoveRequested: handleMoveRequest,
  getBoardState: () => state.currentBoardResponse
});

ui.elements.pgnInput.addEventListener("input", (event) => {
  persistDraft(PGN_DRAFT_STORAGE_KEY, event.target.value);
});

ui.elements.fenInput.addEventListener("input", (event) => {
  persistDraft(FEN_DRAFT_STORAGE_KEY, event.target.value);
});

ui.elements.clearPgnInputButton.addEventListener("click", () => {
  ui.setPgnDraft("");
  persistDraft(PGN_DRAFT_STORAGE_KEY, "");
  ui.setInfoMessage("PGN input cleared.");
});

ui.elements.copyPgnButton.addEventListener("click", async () => {
  const pgn = ui.elements.currentPgnOutput.value.trim();
  if (!pgn) {
    ui.setErrorMessage("There is no current PGN to copy.");
    return;
  }

  try {
    await navigator.clipboard.writeText(pgn);
    ui.setInfoMessage("Current PGN copied.");
  } catch {
    ui.setErrorMessage("Clipboard access failed.");
  }
});

ui.elements.savePgnButton.addEventListener("click", () => {
  const pgn = ui.elements.currentPgnOutput.value.trim();
  if (!pgn) {
    ui.setErrorMessage("There is no current PGN to save.");
    return;
  }

  downloadPgnFile(`${pgn}\n`);
  ui.setInfoMessage("PGN file saved.");
});

ui.elements.newGameButton.addEventListener("click", () => {
  ui.clearErrorMessage();
  ui.openNewGameModal();
});

ui.elements.closeNewGameModalButton.addEventListener("click", () => {
  ui.closeNewGameModal();
});

ui.elements.cancelNewGameButton.addEventListener("click", () => {
  ui.closeNewGameModal();
});

ui.elements.newGameModal.addEventListener("click", (event) => {
  if (state.loading) {
    return;
  }

  if (event.target === ui.elements.newGameModal) {
    ui.closeNewGameModal();
  }
});

window.addEventListener("keydown", (event) => {
  if (state.loading) {
    return;
  }

  if (event.key === "Escape") {
    ui.closeNewGameModal();
  }
});

ui.elements.confirmNewGameButton.addEventListener("click", async () => {
  stopReplayAutoplay();
  await runWithUiFeedback(async () => {
    const config = ui.readNewGameConfig();
    const boardState = await newGame(config);
    await refreshBoardContext(boardState);
    ui.closeNewGameModal();
    ui.setInfoMessage("New game started.");
  }, "Starting new game...");
});

ui.elements.resetGameButton.addEventListener("click", async () => {
  stopReplayAutoplay();
  await runWithUiFeedback(async () => {
    const boardState = await resetGame();
    await refreshBoardContext(boardState);
    ui.setInfoMessage("Game reset.");
  }, "Resetting game...");
});

ui.elements.submitMoveButton.addEventListener("click", async () => {
  const uci = ui.elements.uciInput.value.trim();
  if (!uci) {
    ui.setErrorMessage("Move input is required.");
    return;
  }

  await interaction.submitManualMove(uci);
});

ui.elements.uciInput.addEventListener("keydown", async (event) => {
  if (event.key !== "Enter") {
    return;
  }

  event.preventDefault();
  ui.elements.submitMoveButton.click();
});

ui.elements.loadFenButton.addEventListener("click", async () => {
  const fen = ui.elements.fenInput.value.trim();
  if (!fen) {
    ui.setErrorMessage("FEN input is required.");
    return;
  }

  stopReplayAutoplay();
  await runWithUiFeedback(async () => {
    const boardState = await loadFen(fen);
    await refreshBoardContext(boardState);
    ui.setInfoMessage("FEN loaded.");
  }, "Loading FEN...");
});

ui.elements.loadPgnButton.addEventListener("click", async () => {
  const pgn = ui.elements.pgnInput.value.trim();
  if (!pgn) {
    ui.setErrorMessage("PGN input is required.");
    return;
  }

  stopReplayAutoplay();
  await runWithUiFeedback(async () => {
    const boardState = await loadPgnReplay(pgn);
    await refreshBoardContext(boardState);
    ui.setInfoMessage("PGN replay loaded at start position.");
  }, "Loading PGN replay...");
});

ui.elements.replayStartButton.addEventListener("click", async () => {
  await applyReplayAction(replayStart, "Jumping to replay start...", "Replay moved to the start.");
});

ui.elements.replayBackButton.addEventListener("click", async () => {
  await applyReplayAction(replayBackward, "Stepping replay backward...", "Replay stepped backward.");
});

ui.elements.replayPlayButton.addEventListener("click", async () => {
  if (state.replayAutoplayActive) {
    stopReplayAutoplay({ keepMessage: false });
    return;
  }

  await startReplayAutoplay();
});

ui.elements.replayForwardButton.addEventListener("click", async () => {
  await applyReplayAction(replayForward, "Stepping replay forward...", "Replay stepped forward.");
});

ui.elements.replayEndButton.addEventListener("click", async () => {
  await applyReplayAction(replayEnd, "Jumping to replay end...", "Replay moved to the end.");
});

(async function bootstrap() {
  ui.setLoading(false);
  interaction.disable();
  renderStatus(null, null);
  renderReplayStatus({ active: false, index: null, length: null });

  try {
    const [result, boardState, status, replay] = await Promise.all([
      health(),
      getBoard(),
      getStatus(),
      getReplayStatus()
    ]);

    ui.setConnectionStatus(`Backend status: ${result.status}`);
    renderBoardState(boardState);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    interaction.enable();
    state.gameStarted = true;
    ui.setInfoMessage("Board loaded. Start a new game or continue from the current position.");
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : "Backend not reachable.";
    ui.setConnectionStatus("Backend offline");
    ui.setErrorMessage(message);
  }
})();
