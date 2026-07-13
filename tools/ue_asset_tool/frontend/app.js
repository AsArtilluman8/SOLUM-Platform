"use strict";

const SECTIONS = [
  "Overview", "Coverage", "Asset Browser", "Dependencies", "Textures", "Models",
  "Materials", "Material Functions", "MIC", "MPC", "Audio", "Curves",
  "Blueprint Graph", "Kismet Bytecode", "Niagara", "Map Actors", "Transforms",
  "UDS Runtime Reconstruction", "Errors", "Unsupported", "Provenance"
];

const state = {
  index: null, inventory: null, coverage: null, gate: null, errors: null, provenance: null,
  section: "Overview", query: "", classFilter: "All", page: 0, selected: null,
  assetCache: new Map(), contractCache: new Map()
};
const $ = selector => document.querySelector(selector);
const view = $("#view");

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = String(text);
  return node;
}
function clear(node) { while (node.firstChild) node.firstChild.remove(); }
function format(value) { return new Intl.NumberFormat().format(Number(value || 0)); }
function statusClass(value) {
  if (value === "VERIFIED" || value === "PASSED") return "verified";
  if (value === "PARTIAL_VERIFIED" || value === "RAW_VERIFIED") return "partial";
  return "error";
}
function metric(label, value) {
  const node = el("div", "metric");
  node.append(el("strong", "", format(value)), el("span", "", label));
  return node;
}
function card(title, body, className = "") {
  const node = el("article", `card ${className}`.trim());
  if (title) node.append(el("h2", "", title));
  if (body instanceof Node) node.append(body); else node.append(el("p", "", body));
  return node;
}
function provenance(asset, output) {
  const node = el("p", "provenance");
  const source = asset ? `${asset.source_file} · SHA-256 ${asset.source_sha256}` : "Canonical aggregate JSON";
  node.textContent = output ? `${source} · output SHA-256 ${output.sha256}` : source;
  return node;
}
function empty(message) {
  const node = $("#emptyTemplate").content.cloneNode(true);
  if (message) node.querySelector("p").textContent = message;
  return node;
}
function datasetUrl(relative) { return new URL(relative, new URL(state.index.dataset_url, location.href)).href; }
function outputUrl(path) {
  const marker = `/${new URL(state.index.dataset_url, location.href).pathname.split("/").filter(Boolean).pop()}/`;
  const at = path.indexOf(marker);
  return at >= 0 ? datasetUrl(path.slice(at + marker.length)) : null;
}
async function getJson(url) {
  const response = await fetch(url, {cache: "no-store"});
  if (!response.ok) throw new Error(`${response.status} ${response.statusText}: ${url}`);
  return response.json();
}
async function loadAsset(summary) {
  if (!state.assetCache.has(summary.asset_id)) {
    state.assetCache.set(summary.asset_id, getJson(datasetUrl(summary.json_path)));
  }
  return state.assetCache.get(summary.asset_id);
}
async function loadContract(summary) {
  if (!state.contractCache.has(summary.contract_path)) {
    state.contractCache.set(summary.contract_path, getJson(datasetUrl(summary.contract_path)));
  }
  return state.contractCache.get(summary.contract_path);
}

function renderHero() {
  const root = $("#summary"); clear(root);
  const grid = el("div", "metric-grid");
  grid.append(
    metric("Input files", state.gate.total_input_files), metric("Packages", state.gate.total_packages),
    metric("Assets", state.inventory.totals.assets), metric("Exact textures", state.gate.texture_outputs),
    metric("Exact GLB", state.gate.model_outputs), metric("Exact audio", state.gate.audio_outputs)
  );
  root.append(grid);
  const badge = $("#gateBadge");
  badge.textContent = state.gate.gate_status;
  badge.className = `status ${statusClass(state.gate.gate_status)}`;
}

function renderOverview() {
  const stack = el("div", "stack");
  const grid = el("div", "metric-grid");
  grid.append(
    metric("VERIFIED", state.gate.VERIFIED_count),
    metric("PARTIAL", state.gate.PARTIAL_VERIFIED_count),
    metric("Unsupported assets", state.gate.unsupported_count),
    metric("Integrity errors", state.gate.integrity_error_count),
    metric("Material nodes", state.coverage.metrics.material_nodes),
    metric("Blueprint nodes", state.coverage.metrics.blueprint_nodes),
    metric("Bytecode expressions", state.gate.bytecode_expression_count),
    metric("Curve keys", state.gate.Curve_key_count)
  );
  stack.append(card("Extraction gate", grid));
  const runtime = state.gate.runtime_ready
    ? "EXECUTABLE RUNTIME CONTRACT"
    : "NOT YET EXECUTABLE — VERIFIED GRAPH/DATA CONTRACT ONLY";
  stack.append(card("Runtime truth", runtime, "banner"));
  stack.append(card("Scope", `All ${state.gate.total_input_files} archive entries have terminal records. Unsupported data remains visible and is not counted as successful extraction.`));
  view.append(stack);
}

function renderCoverage() {
  const rows = Object.entries(state.coverage.counts_by_asset_class).map(([name, count]) => [name, count]);
  view.append(table(["Asset class", "Count"], rows));
}
function table(headers, rows) {
  const wrap = el("div", "table-wrap"); const t = el("table");
  const head = el("thead"); const trh = el("tr");
  headers.forEach(value => trh.append(el("th", "", value))); head.append(trh); t.append(head);
  const body = el("tbody");
  rows.forEach(row => { const tr = el("tr"); row.forEach(value => tr.append(el("td", "", value ?? "—"))); body.append(tr); });
  t.append(body); wrap.append(t); return wrap;
}

function filteredAssets(predicate = () => true) {
  const query = state.query.trim().toLowerCase();
  return state.inventory.assets.filter(asset => {
    const match = !query || `${asset.asset_id} ${asset.asset_class} ${asset.package_path}`.toLowerCase().includes(query);
    const cls = state.classFilter === "All" || asset.asset_class === state.classFilter;
    return match && cls && predicate(asset);
  });
}
function assetList(assets) {
  const root = el("div", "stack");
  const perPage = 40; const pages = Math.max(1, Math.ceil(assets.length / perPage));
  state.page = Math.min(state.page, pages - 1);
  assets.slice(state.page * perPage, (state.page + 1) * perPage).forEach(asset => {
    const button = el("button", "card asset-card");
    button.append(el("h3", "", asset.package_path), el("p", "meta", `${asset.asset_class} · ${asset.extraction_status} · ${asset.asset_id}`));
    button.addEventListener("click", () => selectAsset(asset)); root.append(button);
  });
  const pager = el("div", "pager");
  const prev = el("button", "", "Previous"); prev.disabled = state.page === 0;
  const next = el("button", "", "Next"); next.disabled = state.page + 1 >= pages;
  prev.onclick = () => { state.page--; render(); }; next.onclick = () => { state.page++; render(); };
  pager.append(prev, el("span", "meta", `${assets.length} assets · ${state.page + 1}/${pages}`), next); root.append(pager);
  return root;
}
function classChips(assets) {
  const chips = el("div", "chips");
  ["All", ...new Set(assets.map(item => item.asset_class))].forEach(name => {
    const button = el("button", `chip ${state.classFilter === name ? "active" : ""}`, name);
    button.onclick = () => { state.classFilter = name; state.page = 0; render(); }; chips.append(button);
  });
  return chips;
}
function renderAssetBrowser(predicate = () => true) {
  const base = state.inventory.assets.filter(predicate);
  view.append(classChips(base), assetList(filteredAssets(predicate)));
}

async function selectAsset(summary) {
  state.selected = summary; clear(view); view.append(card("Loading asset", summary.package_path));
  try {
    const [asset, wrapper] = await Promise.all([loadAsset(summary), loadContract(summary)]);
    renderAssetDetail(summary, asset, wrapper.contract);
  } catch (error) { renderFailure(error); }
}
function renderAssetDetail(summary, asset, contract) {
  clear(view); const root = el("div", "stack");
  const header = card(summary.package_path, `${summary.asset_class} · ${asset.extraction_status}`);
  header.append(provenance(asset)); root.append(header);
  const grid = el("div", "detail-grid");
  const facts = el("div", "stack");
  facts.append(card("Package", table(["Field", "Value"], [
    ["Asset ID", asset.asset_id], ["UE4 version", asset.package_version.file_version_ue4],
    ["UE5 version", asset.package_version.file_version_ue5], ["Imports", asset.imports.length],
    ["Exports", asset.exports.length], ["Raw regions", asset.raw_verified_regions.length],
    ["Unsupported", asset.unsupported_regions.length]
  ])));
  facts.append(card("Generated outputs", asset.generated_outputs.length ? table(["Kind", "Status", "SHA-256"], asset.generated_outputs.map(x => [x.kind, x.status, x.sha256])) : empty()));
  const raw = el("pre", "", JSON.stringify(contract, null, 2));
  grid.append(facts, card("Canonical contract", raw)); root.append(grid);
  view.append(root);
}

async function renderMedia(kind) {
  const root = el("div", "stack"); view.append(root);
  if (!state.provenance) state.provenance = await getJson(state.index.provenance_url);
  const summaries = new Map(state.inventory.assets.map(item => [item.asset_id, item]));
  let count = 0;
  for (const owner of state.provenance.assets) {
    const summary = summaries.get(owner.asset_id); if (!summary) continue;
    const asset = {source_file: owner.source_file, source_sha256: owner.source_sha256};
    for (const output of owner.generated_outputs.filter(item => item.kind === kind && item.status === "VERIFIED")) {
      const url = outputUrl(output.path); if (!url) continue; count++;
      const node = card(summary.package_path, `${summary.asset_class} · VERIFIED ${kind}`, "media-card");
      if (kind === "audio") { const audio = el("audio"); audio.controls = true; audio.preload = "none"; audio.src = url; node.append(audio); }
      else if (kind === "texture") { const image = el("img"); image.loading = "lazy"; image.src = url; image.alt = summary.package_path; image.style.maxWidth = "100%"; node.append(image); }
      else { const link = el("a", "", "Open verified GLB"); link.href = url; node.append(link); }
      node.append(provenance(asset, output)); root.append(node);
    }
  }
  if (!count) root.append(empty(`No VERIFIED ${kind} output exists; no substitute is rendered.`));
}

async function renderCurve(summary, target = view) {
  const [asset, wrapper] = await Promise.all([loadAsset(summary), loadContract(summary)]);
  const contract = wrapper.contract; const root = el("div", "stack");
  for (const channel of contract.channels || []) {
    const node = card(`${summary.package_path} · ${channel.name}`, "");
    const keys = channel.keys || [];
    if (channel.status === "VERIFIED" && keys.length) {
      const times = keys.map(k => Number(k.time)), values = keys.map(k => Number(k.value));
      const minT = Math.min(...times), maxT = Math.max(...times), minV = Math.min(...values), maxV = Math.max(...values);
      const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg"); svg.setAttribute("viewBox", "0 0 600 240"); svg.classList.add("graph-svg");
      const points = keys.map(k => `${20 + (Number(k.time)-minT)/Math.max(maxT-minT,1e-12)*560},${220-(Number(k.value)-minV)/Math.max(maxV-minV,1e-12)*200}`).join(" ");
      const line = document.createElementNS(svg.namespaceURI, "polyline"); line.setAttribute("points", points); line.setAttribute("class", "curve"); svg.append(line); node.append(svg);
      node.append(table(["Time", "Value", "Interp", "Offset", "SHA-256"], keys.map(k => [k.time, k.value, k.interp_mode.name, k.provenance.physical_offset, k.provenance.sha256])));
    } else node.append(empty("Serialized keys are not fully verified."));
    node.append(provenance(asset)); root.append(node);
  }
  target.append(root.childNodes.length ? root : empty());
}
async function renderCurves() {
  const curves = filteredAssets(a => ["CurveFloat", "CurveVector", "CurveLinearColor"].includes(a.asset_class));
  if (!curves.length) return view.append(empty());
  const selector = el("select"); curves.forEach(a => { const o=el("option", "", a.package_path); o.value=a.asset_id; selector.append(o); });
  const holder = el("div", "stack"); view.append(selector, holder);
  async function show() { clear(holder); const chosen=curves.find(a=>a.asset_id===selector.value); await renderCurve(chosen, holder); }
  selector.onchange = show;
  await show();
}

async function renderGraph(kind) {
  const predicate = kind === "material"
    ? a => ["Material", "MaterialFunction"].includes(a.asset_class)
    : kind === "blueprint" ? a => a.contract_path.startsWith("blueprints/")
    : a => a.contract_path.startsWith("niagara/");
  const candidates = filteredAssets(predicate);
  if (!candidates.length) return view.append(empty());
  const summary = candidates[0]; const [asset, wrapper] = await Promise.all([loadAsset(summary), loadContract(summary)]);
  const contract = wrapper.contract; const graph = contract.graph || {};
  const root = el("div", "stack"); root.append(card(summary.package_path, `${graph.status} · nodes ${graph.node_count || 0}`));
  if (kind === "niagara") root.append(card("Execution status", "GRAPH/DATA CONTRACT ONLY — NOT EXACTLY EXECUTABLE", "banner"));
  const rows = [];
  if (kind === "material") (graph.nodes || []).forEach(n => rows.push([n.export_index, n.class, n.name, n.inputs.length, n.object]));
  else (graph.graphs || []).flatMap(g => g.nodes || []).forEach(n => rows.push([n.export_index, n.class, n.name, (n.pins || []).filter(Boolean).length, n.object]));
  root.append(card("Real serialized nodes", table(["Export", "Class", "Name", "Pins/inputs", "Object"], rows.slice(0, 500))));
  const links = kind === "material" ? graph.links || [] : (graph.graphs || []).flatMap(g => g.edges || []);
  root.append(card("Validated links", table(["From", "To", "Status"], links.slice(0, 500).map(x => [x.from_object || `${x.from_node}:${x.from_pin}`, x.to_object || `${x.to_node}:${x.to_pin}`, x.validation || (x.target_resolved ? "VERIFIED" : "UNRESOLVED")]))));
  if (kind === "material") {
    root.append(card("Parameters", table(["Class", "Name", "GUID"], (contract.parameters || []).map(x => [x.class, x.name, x.guid]))));
    root.append(card("Function calls", el("pre", "", JSON.stringify(contract.function_calls || [], null, 2))));
  }
  root.append(provenance(asset)); view.append(root);
}

async function renderBytecode() {
  const candidates = filteredAssets(a => a.contract_path.startsWith("blueprints/"));
  if (!candidates.length) return view.append(empty());
  const summary = candidates[0]; const [asset, wrapper] = await Promise.all([loadAsset(summary), loadContract(summary)]);
  const bytecode = wrapper.contract.bytecode || {}; const root = el("div", "stack");
  root.append(card(summary.package_path, `${bytecode.status} · ${bytecode.function_count || 0} functions`));
  (bytecode.functions || []).forEach(fn => root.append(card(fn.object, table(["Field", "Value"], [
    ["Status", fn.status], ["Expressions", fn.script?.expression_count], ["Storage offset", fn.script?.storage_physical_offset], ["Storage SHA-256", fn.script?.storage_sha256]
  ]))));
  root.append(provenance(asset)); view.append(root);
}

function renderDependencies() {
  if (!state.selected) return view.append(empty("Select an asset in Asset Browser to inspect exact package references."));
  selectAsset(state.selected);
}
function renderErrors(unsupportedOnly = false) {
  const items = state.errors.errors.filter(x => !unsupportedOnly || x.status === "UNSUPPORTED_FORMAT");
  if (!items.length) return view.append(empty());
  const root = el("div", "stack");
  items.slice(0, 1000).forEach(item => root.append(card(item.asset_id || item.source_file, item.reason || item.status, "error-card")));
  view.append(root);
}
function renderNoMaps(section) {
  const count = section === "Transforms" ? state.gate.verified_transform_count : state.gate.map_actor_count;
  view.append(card(section, `${count} verified records. P59 contains no .umap package, so no actor or transform is inferred.`));
}
function renderRuntime() {
  view.append(card("UDS Runtime Reconstruction", "NOT YET EXECUTABLE — VERIFIED GRAPH/DATA CONTRACT ONLY", "banner"));
  view.append(card("Evidence available", `Blueprint graphs ${state.gate.Blueprint_graph_count}; bytecode expressions ${state.gate.bytecode_expression_count}; exact curve keys ${state.gate.Curve_key_count}; MPC ${state.gate.MPC_count}. Native execution mappings remain incomplete.`));
}
function renderFailure(error) { clear(view); view.append(card("Visible error", `${error.name}: ${error.message}`, "error-card")); }

async function render() {
  clear(view);
  try {
    switch (state.section) {
      case "Overview": renderOverview(); break;
      case "Coverage": renderCoverage(); break;
      case "Asset Browser": renderAssetBrowser(); break;
      case "Dependencies": renderDependencies(); break;
      case "Textures": await renderMedia("texture"); break;
      case "Models": await renderMedia("model"); break;
      case "Audio": await renderMedia("audio"); break;
      case "Materials": await renderGraph("material"); break;
      case "Material Functions": renderAssetBrowser(a => a.asset_class === "MaterialFunction"); break;
      case "MIC": renderAssetBrowser(a => a.asset_class === "MaterialInstanceConstant"); break;
      case "MPC": renderAssetBrowser(a => a.asset_class === "MaterialParameterCollection"); break;
      case "Curves": await renderCurves(); break;
      case "Blueprint Graph": await renderGraph("blueprint"); break;
      case "Kismet Bytecode": await renderBytecode(); break;
      case "Niagara": await renderGraph("niagara"); break;
      case "Map Actors": case "Transforms": renderNoMaps(state.section); break;
      case "UDS Runtime Reconstruction": renderRuntime(); break;
      case "Errors": renderErrors(false); break;
      case "Unsupported": renderErrors(true); break;
      case "Provenance": renderAssetBrowser(); break;
      default: renderAssetBrowser();
    }
  } catch (error) { renderFailure(error); }
}

async function start() {
  try {
    state.index = await getJson("data/index.json");
    [state.inventory, state.coverage, state.gate, state.errors] = await Promise.all([
      getJson(state.index.inventory_url), getJson(state.index.coverage_url),
      getJson(state.index.gate_url), getJson(state.index.errors_url)
    ]);
    if (state.gate.gate_status !== "PASSED" || !state.gate.ready_for_html) throw new Error("Extraction gate is not PASSED");
    SECTIONS.forEach(name => { const option=el("option", "", name); option.value=name; $("#sectionSelect").append(option); });
    $("#sectionSelect").onchange = event => chooseSection(event.target.value);
    $("#search").oninput = event => { state.query=event.target.value; state.page=0; render(); };
    document.querySelectorAll(".bottom-nav button").forEach(button => button.onclick=()=>chooseSection(button.dataset.section));
    renderHero(); await render();
  } catch (error) { renderFailure(error); const badge=$("#gateBadge"); badge.textContent="ERROR"; badge.className="status error"; }
}
function chooseSection(name) {
  state.section=name; state.page=0; $("#sectionSelect").value=name;
  document.querySelectorAll(".bottom-nav button").forEach(button => button.classList.toggle("active", button.dataset.section===name));
  render();
}
start();
