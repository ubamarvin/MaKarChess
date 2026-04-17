import * as THREE from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";

export function createScene(container) {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x11151d);

  const camera = new THREE.PerspectiveCamera(45, container.clientWidth / container.clientHeight, 0.1, 1000);
  camera.position.set(0, 11, 10);

  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(container.clientWidth, container.clientHeight);
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  container.appendChild(renderer.domElement);

  const controls = new OrbitControls(camera, renderer.domElement);
  controls.target.set(0, 0, 0);
  controls.enableDamping = true;
  controls.minDistance = 8;
  controls.maxDistance = 24;
  controls.maxPolarAngle = Math.PI / 2.1;

  const ambientLight = new THREE.AmbientLight(0xffffff, 1.6);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 1.8);
  directionalLight.position.set(8, 16, 10);
  directionalLight.castShadow = true;
  directionalLight.target.position.set(0, 0, 0);
  directionalLight.shadow.mapSize.width = 2048;
  directionalLight.shadow.mapSize.height = 2048;
  directionalLight.shadow.bias = -0.0008;
  directionalLight.shadow.normalBias = 0.02;
  directionalLight.shadow.camera.left = -8;
  directionalLight.shadow.camera.right = 8;
  directionalLight.shadow.camera.top = 8;
  directionalLight.shadow.camera.bottom = -8;
  directionalLight.shadow.camera.near = 1;
  directionalLight.shadow.camera.far = 40;
  scene.add(directionalLight.target);
  scene.add(directionalLight);

  const fillLight = new THREE.PointLight(0x8fb4ff, 0.6);
  fillLight.position.set(-8, 8, -8);
  scene.add(fillLight);

  function onResize() {
    const width = container.clientWidth;
    const height = container.clientHeight;
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
  }

  window.addEventListener("resize", onResize);

  function render() {
    controls.update();
    renderer.render(scene, camera);
  }

  function start() {
    renderer.setAnimationLoop(render);
  }

  function stop() {
    renderer.setAnimationLoop(null);
    window.removeEventListener("resize", onResize);
  }

  return {
    THREE,
    scene,
    camera,
    renderer,
    controls,
    start,
    stop,
    render
  };
}
