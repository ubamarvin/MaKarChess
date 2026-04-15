import * as THREE from "three";
import { squareToWorld } from "./board.js";

let piecesGroup = null;

const whiteMaterial = new THREE.MeshStandardMaterial({
  color: 0xf4f1e8,
  roughness: 0.38,
  metalness: 0.08
});

const blackMaterial = new THREE.MeshStandardMaterial({
  color: 0x1e2430,
  roughness: 0.3,
  metalness: 0.12
});

function materialFor(color) {
  return color === "White" ? whiteMaterial : blackMaterial;
}

function addMesh(group, geometry, material, y, options = {}) {
  const mesh = new THREE.Mesh(geometry, material);
  mesh.position.y = y;
  if (options.rotationX) {
    mesh.rotation.x = options.rotationX;
  }
  if (options.rotationY) {
    mesh.rotation.y = options.rotationY;
  }
  if (options.rotationZ) {
    mesh.rotation.z = options.rotationZ;
  }
  if (options.x) {
    mesh.position.x = options.x;
  }
  if (options.z) {
    mesh.position.z = options.z;
  }
  group.add(mesh);
  return mesh;
}

function createBase(group, material, bodyHeight, baseRadius = 0.33, footRadius = 0.4) {
  addMesh(group, new THREE.CylinderGeometry(footRadius, footRadius * 0.92, 0.1, 32), material, 0.05);
  addMesh(group, new THREE.TorusGeometry(baseRadius, 0.045, 18, 42), material, 0.11, { rotationX: Math.PI / 2 });
  addMesh(group, new THREE.CylinderGeometry(baseRadius * 0.92, baseRadius * 0.78, bodyHeight, 28), material, 0.12 + bodyHeight / 2);
}

function createPawn(material) {
  const group = new THREE.Group();
  createBase(group, material, 0.42, 0.22, 0.31);
  addMesh(group, new THREE.SphereGeometry(0.16, 24, 20), material, 0.72);
  return group;
}

function createRook(material) {
  const group = new THREE.Group();
  createBase(group, material, 0.58, 0.28, 0.34);
  addMesh(group, new THREE.CylinderGeometry(0.21, 0.24, 0.18, 24), material, 0.86);
  addMesh(group, new THREE.CylinderGeometry(0.3, 0.28, 0.1, 24), material, 1.0);

  for (let index = 0; index < 4; index += 1) {
    const angle = (index / 4) * Math.PI * 2;
    addMesh(group, new THREE.BoxGeometry(0.1, 0.12, 0.12), material, 1.1, {
      x: Math.cos(angle) * 0.2,
      z: Math.sin(angle) * 0.2
    });
  }

  return group;
}

function createKnight(material) {
  const group = new THREE.Group();
  createBase(group, material, 0.54, 0.27, 0.34);
  addMesh(group, new THREE.CylinderGeometry(0.16, 0.24, 0.28, 20), material, 0.82);
  addMesh(group, new THREE.BoxGeometry(0.24, 0.52, 0.16), material, 1.0, {
    x: 0.03,
    rotationZ: -0.3
  });
  addMesh(group, new THREE.BoxGeometry(0.28, 0.24, 0.18), material, 1.22, {
    x: 0.12,
    rotationZ: 0.4
  });
  addMesh(group, new THREE.ConeGeometry(0.06, 0.18, 4), material, 1.4, {
    x: 0.05,
    z: 0.05,
    rotationZ: -0.2
  });
  addMesh(group, new THREE.ConeGeometry(0.06, 0.18, 4), material, 1.38, {
    x: -0.03,
    z: -0.01,
    rotationZ: -0.45
  });
  return group;
}

function createBishop(material) {
  const group = new THREE.Group();
  createBase(group, material, 0.7, 0.24, 0.33);
  addMesh(group, new THREE.SphereGeometry(0.19, 24, 20), material, 1.0);
  addMesh(group, new THREE.ConeGeometry(0.11, 0.28, 20), material, 1.25);
  addMesh(group, new THREE.BoxGeometry(0.05, 0.22, 0.18), material, 1.07, {
    rotationZ: 0.5
  });
  return group;
}

function createQueen(material) {
  const group = new THREE.Group();
  createBase(group, material, 0.78, 0.28, 0.36);
  addMesh(group, new THREE.SphereGeometry(0.18, 24, 20), material, 1.1);
  addMesh(group, new THREE.CylinderGeometry(0.14, 0.16, 0.12, 20), material, 1.25);

  for (let index = 0; index < 5; index += 1) {
    const angle = (index / 5) * Math.PI * 2;
    addMesh(group, new THREE.SphereGeometry(0.055, 16, 12), material, 1.34, {
      x: Math.cos(angle) * 0.15,
      z: Math.sin(angle) * 0.15
    });
  }

  addMesh(group, new THREE.SphereGeometry(0.07, 16, 12), material, 1.36);
  return group;
}

function createKing(material) {
  const group = new THREE.Group();
  createBase(group, material, 0.88, 0.29, 0.37);
  addMesh(group, new THREE.SphereGeometry(0.16, 24, 20), material, 1.2);
  addMesh(group, new THREE.CylinderGeometry(0.08, 0.08, 0.22, 16), material, 1.4);
  addMesh(group, new THREE.BoxGeometry(0.26, 0.06, 0.08), material, 1.48);
  addMesh(group, new THREE.BoxGeometry(0.08, 0.24, 0.08), material, 1.48);
  return group;
}

function createPieceMesh(piece) {
  const material = materialFor(piece.color);

  switch (piece.kind) {
    case "Pawn":
      return createPawn(material);
    case "Rook":
      return createRook(material);
    case "Knight":
      return createKnight(material);
    case "Bishop":
      return createBishop(material);
    case "Queen":
      return createQueen(material);
    case "King":
      return createKing(material);
    default:
      return createPawn(material);
  }
}

export function clearPieces(scene) {
  if (piecesGroup) {
    scene.remove(piecesGroup);
  }

  piecesGroup = new THREE.Group();
  piecesGroup.name = "pieces";
  scene.add(piecesGroup);
}

export function renderPieces(scene, boardState) {
  if (!piecesGroup) {
    clearPieces(scene);
  }

  piecesGroup.clear();

  for (const occupiedSquare of boardState.board || []) {
    const { position, piece } = occupiedSquare;
    const mesh = createPieceMesh(piece);
    const { x, z } = squareToWorld(position.file, position.rank);
    mesh.position.set(x, 0.11, z);

    mesh.traverse((child) => {
      if (child.isMesh) {
        child.castShadow = true;
        child.receiveShadow = true;
      }
      child.userData = {
        type: "piece",
        color: piece.color,
        kind: piece.kind,
        position: `${position.file}${position.rank}`,
        file: position.file,
        rank: position.rank
      };
    });

    piecesGroup.add(mesh);
  }
}

export function getPiecesGroup() {
  return piecesGroup;
}
