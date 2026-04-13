import {
  health,
  newGame,
  resetGame,
  getBoard,
  getStatus,
  getReplayStatus,
  loadFen,
  loadPgnReplay,
  replayForward,
  replayBackward,
  makeMove,
  ApiClientError,
  setAuthTokenProvider
} from "./api.js";
import {
  getCurrentIdToken,
  observeAuthState,
  signInWithEmail,
  signInWithGoogle,
  signOutCurrentUser,
  signUpWithEmail
} from "./auth.js";
import { createScene } from "./scene.js";
import { createBoard, clearHighlights } from "./board.js";
import { clearPieces, renderPieces } from "./pieces.js";
import { createUiBindings } from "./ui.js";
import { createInteractionController } from "./interaction.js";

const state = {
  currentBoardResponse: null,
  currentStatusResponse: null,
  currentReplayResponse: null,
  loading: false,
  gameStarted: false,
  authenticatedUser: null
};

const ui = createUiBindings();
const sceneRoot = createScene(document.getElementById("board-container"));
createBoard(sceneRoot.scene);
clearPieces(sceneRoot.scene);
sceneRoot.start();

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
  ui.renderReplayStatus(replay);
}

function requireAuthCredentials() {
  const { email, password } = ui.readAuthCredentials();
  if (!email) {
    ui.setErrorMessage("Email is required.");
    return null;
  }
  if (!password) {
    ui.setErrorMessage("Password is required.");
    return null;
  }
  return { email, password };
}

async function syncAuthenticatedSession() {
  const [boardState, status, replay] = await Promise.all([getBoard(), getStatus(), getReplayStatus()]);
  renderBoardState(boardState);
  renderStatus(status, boardState);
  renderReplayStatus(replay);
  interaction.enable();
  state.gameStarted = true;
}

function applySignedOutState() {
  state.authenticatedUser = null;
  state.currentBoardResponse = null;
  state.currentStatusResponse = null;
  state.currentReplayResponse = { active: false, index: null, length: null };
  ui.setAuthStatus("Not signed in");
  ui.setChessControlsEnabled(false);
  ui.setAuthControlsEnabled(true, false);
  ui.closeNewGameModal();
  interaction.disable();
  clearHighlights();
  renderStatus(null, null);
  renderReplayStatus({ active: false, index: null, length: null });
  ui.setInfoMessage("Sign in to access your chess session.");
}

async function applySignedInState(user) {
  state.authenticatedUser = user;
  const label = user.email ? `Signed in as ${user.email}` : "Signed in";
  ui.setAuthStatus(label);
  ui.setAuthControlsEnabled(true, true);
  ui.clearErrorMessage();
  ui.setInfoMessage("Loading your game session...");
  try {
    await syncAuthenticatedSession();
    ui.setChessControlsEnabled(true);
    ui.setInfoMessage("Signed in. Your chess session is ready.");
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : "Unable to load your game session.";
    ui.setChessControlsEnabled(false);
    interaction.disable();
    ui.setErrorMessage(message);
  }
}

async function refreshReplayStatus() {
  const replay = await getReplayStatus();
  renderReplayStatus(replay);
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
  }
}

async function handleMoveRequest(uci) {
  await runWithUiFeedback(async () => {
    const boardState = await makeMove(uci);
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    if (ui.elements.uciInput) {
      ui.elements.uciInput.value = "";
    }
    ui.setInfoMessage(`Move applied: ${uci}`);
  }, `Submitting move ${uci}...`);
}

const interaction = createInteractionController({
  renderer: sceneRoot.renderer,
  camera: sceneRoot.camera,
  scene: sceneRoot.scene,
  ui,
  onMoveRequested: handleMoveRequest,
  getBoardState: () => state.currentBoardResponse
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
  await runWithUiFeedback(async () => {
    const config = ui.readNewGameConfig();
    const boardState = await newGame(config);
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    clearHighlights();
    interaction.enable();
    state.gameStarted = true;
    ui.closeNewGameModal();
    ui.setInfoMessage("New game started.");
  }, "Starting new game...");
});

ui.elements.resetGameButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    const boardState = await resetGame();
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    clearHighlights();
    interaction.enable();
    state.gameStarted = true;
    ui.setInfoMessage("Game reset.");
  }, "Resetting game...");
});

if (ui.elements.submitMoveButton && ui.elements.uciInput) {
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
}

ui.elements.loadFenButton.addEventListener("click", async () => {
  const fen = ui.elements.importInput.value.trim();
  if (!fen) {
    ui.setErrorMessage("FEN input is required.");
    return;
  }

  await runWithUiFeedback(async () => {
    const boardState = await loadFen(fen);
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    clearHighlights();
    interaction.enable();
    state.gameStarted = true;
    ui.elements.importInput.value = "";
    ui.setInfoMessage("FEN loaded.");
  }, "Loading FEN...");
});

ui.elements.loadPgnButton.addEventListener("click", async () => {
  const pgn = ui.elements.importInput.value.trim();
  if (!pgn) {
    ui.setErrorMessage("PGN input is required.");
    return;
  }

  await runWithUiFeedback(async () => {
    const boardState = await loadPgnReplay(pgn);
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    clearHighlights();
    interaction.enable();
    state.gameStarted = true;
    ui.elements.importInput.value = "";
    ui.setInfoMessage("PGN replay loaded at start position.");
  }, "Loading PGN replay...");
});

ui.elements.replayBackButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    const boardState = await replayBackward();
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    clearHighlights();
    interaction.enable();
    ui.setInfoMessage("Replay stepped backward.");
  }, "Stepping replay backward...");
});

ui.elements.replayForwardButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    const boardState = await replayForward();
    renderBoardState(boardState);
    const [status, replay] = await Promise.all([getStatus(), getReplayStatus()]);
    renderStatus(status, boardState);
    renderReplayStatus(replay);
    clearHighlights();
    interaction.enable();
    ui.setInfoMessage("Replay stepped forward.");
  }, "Stepping replay forward...");
});

ui.elements.signInButton.addEventListener("click", async () => {
  const credentials = requireAuthCredentials();
  if (!credentials) {
    return;
  }

  await runWithUiFeedback(async () => {
    await signInWithEmail(credentials.email, credentials.password);
    ui.clearAuthPassword();
    ui.setInfoMessage("Signed in.");
  }, "Signing in...");
});

ui.elements.signUpButton.addEventListener("click", async () => {
  const credentials = requireAuthCredentials();
  if (!credentials) {
    return;
  }

  await runWithUiFeedback(async () => {
    await signUpWithEmail(credentials.email, credentials.password);
    ui.clearAuthPassword();
    ui.setInfoMessage("Account created.");
  }, "Creating account...");
});

ui.elements.googleSignInButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    await signInWithGoogle();
    ui.setInfoMessage("Signed in with Google.");
  }, "Signing in with Google...");
});

ui.elements.signOutButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    await signOutCurrentUser();
    ui.clearAuthInputs();
    ui.setInfoMessage("Signed out.");
  }, "Signing out...");
});

observeAuthState(async (user) => {
  setAuthTokenProvider(getCurrentIdToken);
  if (!user) {
    applySignedOutState();
    return;
  }

  await applySignedInState(user);
});

(async function bootstrap() {
  setAuthTokenProvider(getCurrentIdToken);
  ui.setLoading(false);
  interaction.disable();
  ui.setAuthControlsEnabled(true, false);
  ui.setChessControlsEnabled(false);
  renderStatus(null, null);
  renderReplayStatus({ active: false, index: null, length: null });
  try {
    const result = await health();
    ui.setConnectionStatus(`Backend status: ${result.status}`);
    ui.setInfoMessage("Sign in to access your chess session.");
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : "Backend not reachable.";
    ui.setConnectionStatus("Backend offline");
    ui.setErrorMessage(message);
  }
})();
