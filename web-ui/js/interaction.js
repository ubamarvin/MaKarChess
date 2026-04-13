import * as THREE from "three";
import { clearHighlights, getSquareMeshAt, highlightSquare } from "./board.js";

export function createInteractionController({ renderer, camera, scene, ui, onMoveRequested, getBoardState }) {
  const raycaster = new THREE.Raycaster();
  const pointer = new THREE.Vector2();
  let selectedFromSquare = null;
  let enabled = false;

  function isOccupiedSquare(square) {
    const boardState = getBoardState?.();
    if (!boardState?.board) {
      return false;
    }

    return boardState.board.some(({ position }) => position.file === square.file && position.rank === square.rank);
  }

  function setSelectedSquare(coordinate) {
    selectedFromSquare = coordinate;
    clearHighlights();
    if (coordinate) {
      highlightSquare(coordinate.file, coordinate.rank);
      ui.setSelectedSquare(`${coordinate.file}${coordinate.rank}`);
    } else {
      ui.setSelectedSquare("-");
    }
  }

  function parseSquareData(object) {
    let current = object;
    while (current) {
      if (current.userData?.type === "square") {
        return {
          file: current.userData.file,
          rank: current.userData.rank,
          coordinate: current.userData.coordinate
        };
      }
      if (current.userData?.type === "piece") {
        return {
          file: current.userData.file,
          rank: current.userData.rank,
          coordinate: current.userData.position
        };
      }
      current = current.parent;
    }
    return null;
  }

  async function submitMove(targetSquare) {
    if (!selectedFromSquare) {
      if (!isOccupiedSquare(targetSquare)) {
        ui.setInfoMessage("Select a square with a piece first.");
        return;
      }
      setSelectedSquare(targetSquare);
      return;
    }

    const uci = `${selectedFromSquare.coordinate}${targetSquare.coordinate}`;
    const from = selectedFromSquare;
    setSelectedSquare(null);
    await onMoveRequested(uci, from, targetSquare);
  }

  async function handlePointerDown(event) {
    if (!enabled) {
      return;
    }

    const rect = renderer.domElement.getBoundingClientRect();
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    raycaster.setFromCamera(pointer, camera);
    const intersections = raycaster.intersectObjects(scene.children, true);
    const selected = intersections.map((intersection) => parseSquareData(intersection.object)).find(Boolean);

    if (!selected) {
      return;
    }

    const mesh = getSquareMeshAt(selected.file, selected.rank);
    if (mesh) {
      await submitMove(selected);
    }
  }

  renderer.domElement.addEventListener("pointerdown", handlePointerDown);

  return {
    enable() {
      enabled = true;
    },
    disable() {
      enabled = false;
      setSelectedSquare(null);
    },
    setSelectedSquare,
    async submitManualMove(uci) {
      setSelectedSquare(null);
      await onMoveRequested(uci, null, null);
    },
    dispose() {
      renderer.domElement.removeEventListener("pointerdown", handlePointerDown);
    }
  };
}
