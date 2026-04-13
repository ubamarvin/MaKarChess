import { health, newGame, resetGame, getBoard, getStatus, makeMove, ApiClientError } from "./api.js";
import { createScene } from "./scene.js";
import { createBoard, clearHighlights } from "./board.js";
import { clearPieces, renderPieces } from "./pieces.js";
import { createUiBindings } from "./ui.js";
import { createInteractionController } from "./interaction.js";

const state = {
  currentBoardResponse: null,
  currentStatusResponse: null,
  loading: false,
  gameStarted: false
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

async function refreshStatusAndBoard() {
  const [boardState, status] = await Promise.all([getBoard(), getStatus()]);
  renderBoardState(boardState);
  renderStatus(status, boardState);
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
    const status = await getStatus();
    renderStatus(status, boardState);
    ui.elements.uciInput.value = "";
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

ui.elements.newGameButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    const config = ui.readNewGameConfig();
    const boardState = await newGame(config);
    renderBoardState(boardState);
    const status = await getStatus();
    renderStatus(status, boardState);
    clearHighlights();
    interaction.enable();
    state.gameStarted = true;
    ui.setInfoMessage("New game started.");
  }, "Starting new game...");
});

ui.elements.resetGameButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    const boardState = await resetGame();
    renderBoardState(boardState);
    const status = await getStatus();
    renderStatus(status, boardState);
    clearHighlights();
    interaction.enable();
    state.gameStarted = true;
    ui.setInfoMessage("Game reset.");
  }, "Resetting game...");
});

ui.elements.refreshButton.addEventListener("click", async () => {
  await runWithUiFeedback(async () => {
    await refreshStatusAndBoard();
    if (state.currentBoardResponse) {
      state.gameStarted = true;
      interaction.enable();
    }
    ui.setInfoMessage("Board refreshed.");
  }, "Refreshing board...");
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

(async function bootstrap() {
  ui.setLoading(false);
  interaction.disable();
  renderStatus(null, null);
  try {
    const result = await health();
    ui.setConnectionStatus(`Backend status: ${result.status}`);
    ui.setInfoMessage("Click New Game to start");
  } catch (error) {
    const message = error instanceof ApiClientError ? error.message : "Backend not reachable.";
    ui.setConnectionStatus("Backend offline");
    ui.setErrorMessage(message);
  }
})();
