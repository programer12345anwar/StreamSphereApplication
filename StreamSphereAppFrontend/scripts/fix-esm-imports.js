const fs = require('fs');
const path = require('path');

function walk(dir, cb) {
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch (e) {
    return;
  }
  for (const entry of entries) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p, cb);
    else if (entry.isFile()) cb(p);
  }
}

function shouldProcess(file) {
  if (!file.endsWith('.js')) return false;
  // Only target built ESM files under dist/* (es2015, es2019, es5)
  return /[\\/]dist[\\/](es2015|es2019|es5)[\\/]/.test(file);
}

function fixFile(file) {
  try {
    let content = fs.readFileSync(file, 'utf8');
    const original = content;
    // Fix `from './x'` and `from "./x"` where x does not already end with .js, .json or /
    content = content.replace(/from\s+(['"])(\.\/.+?)\1/g, (m, q, p) => {
      if (p.endsWith('.js') || p.endsWith('.json') || p.endsWith('/')) return m;
      return `from ${q}${p}.js${q}`;
    });
    // Fix `export ... from './x'`
    content = content.replace(/export\s+(?:\{[\s\S]*?\}|default|\*)\s+from\s+(['"])(\.\/.+?)\1/g, (m, q, p) => {
      if (p.endsWith('.js') || p.endsWith('.json') || p.endsWith('/')) return m;
      return m.replace(p + q, p + '.js' + q);
    });
    if (content !== original) {
      fs.writeFileSync(file, content, 'utf8');
      console.log('Patched', file);
    }
  } catch (e) {
    // ignore
  }
}

function main() {
  const root = path.join(__dirname, '..');
  const nm = path.join(root, 'node_modules');
  if (!fs.existsSync(nm)) {
    console.log('node_modules not found, skipping import fixes.');
    return;
  }
  walk(nm, (file) => {
    if (shouldProcess(file)) fixFile(file);
  });
  console.log('ESM import fix pass complete.');
}

main();
