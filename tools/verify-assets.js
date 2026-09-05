#!/usr/bin/env node
/**
 * Static asset integrity check. Catches the class of bug a compile + server boot
 * cannot: a lang key that renders as raw text in a GUI, a model whose parent or
 * texture does not exist, or a GUI sprite referenced in code with no PNG.
 *
 * These are all CLIENT-side failures, so CI's compile + dedicated-server-boot
 * rungs are blind to them — that is exactly why this exists.
 *
 * Usage: node tools/verify-assets.js         (exit 1 on any problem)
 */
const fs = require("fs"), path = require("path");

const SRC = "src/main/java";
const A   = "src/main/resources/assets/scguardgolem";
const problems = [];
const note = (m) => problems.push(m);

// ---- collect java sources ----
const javaFiles = [];
(function walk(d){ for (const e of fs.readdirSync(d, {withFileTypes:true})) {
  const p = path.join(d, e.name);
  if (e.isDirectory()) walk(p); else if (e.name.endsWith(".java")) javaFiles.push(p);
} })(SRC);
const javaText = javaFiles.map(f => fs.readFileSync(f, "utf8")).join("\n");

// ---- 1. lang keys ----
const lang = JSON.parse(fs.readFileSync(`${A}/lang/en_us.json`, "utf8"));
const KEY_RE = /"((?:scguardgolem|item\.scguardgolem|entity\.scguardgolem|itemGroup\.scguardgolem)[a-zA-Z0-9_.]*)"/g;
const referenced = new Set();
for (const m of javaText.matchAll(KEY_RE)) referenced.add(m[1]);
referenced.delete("scguardgolem"); // the mod id itself, not a lang key

const prefixes = [...referenced].filter(k => k.endsWith("."));   // built at runtime, e.g. threat. + MODE
const exact    = [...referenced].filter(k => !k.endsWith("."));
for (const k of exact) if (!(k in lang)) note(`lang: key referenced in code but missing from en_us.json -> ${k}`);
for (const p of prefixes) if (!Object.keys(lang).some(k => k.startsWith(p)))
  note(`lang: prefix built in code has no matching entries -> ${p}*`);

// ---- 2. models: parents + textures ----
const texExists = (ref) => {
  const [ns, rest] = ref.includes(":") ? ref.split(":") : ["minecraft", ref];
  if (ns !== "scguardgolem") return true;                       // vanilla / SecurityCraft: external
  return fs.existsSync(`${A}/textures/${rest}.png`);
};
for (const dir of ["models/item", "items"]) {
  if (!fs.existsSync(`${A}/${dir}`)) continue;
  for (const f of fs.readdirSync(`${A}/${dir}`)) {
    const j = JSON.parse(fs.readFileSync(`${A}/${dir}/${f}`, "utf8"));
    const parent = j.parent || (j.model && j.model.model) || null;
    if (parent) {
      const [ns, rest] = parent.includes(":") ? parent.split(":") : ["minecraft", parent];
      if (ns === "scguardgolem" && !fs.existsSync(`${A}/models/${rest}.json`))
        note(`model: ${dir}/${f} parent does not exist -> ${parent}`);
    }
    for (const [slot, ref] of Object.entries(j.textures || {}))
      if (!texExists(ref)) note(`model: ${dir}/${f} texture "${slot}" missing -> ${ref}`);
  }
}

// ---- 3. GUI sprites referenced in code ----
for (const m of javaText.matchAll(/scguardgolem:([a-z0-9_]+)"\)/g)) {
  const name = m[1];
  const isSprite = fs.existsSync(`${A}/textures/gui/sprites/${name}.png`);
  const isItem   = fs.existsSync(`${A}/textures/item/${name}.png`);
  const isModel  = fs.existsSync(`${A}/models/item/${name}.json`);
  if (!isSprite && !isItem && !isModel)
    note(`sprite: code references scguardgolem:${name} but no PNG/model found`);
}

// ---- report ----
if (problems.length) {
  console.error(`ASSET VERIFY FAILED (${problems.length}):`);
  for (const p of problems) console.error("  - " + p);
  process.exit(1);
}
console.log("asset verify OK: lang keys, model parents/textures and GUI sprites all resolve");
