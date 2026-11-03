// Downloads the mods Little Birds is tested alongside (the ones a cozy-pack player is likely to
// run with it) and their libraries, for one loader on Minecraft 26.3, into <folder>/mods/, checking
// each Modrinth file's SHA-512. Cozy Core comes from its GitHub release.
//   node tools/fetch-test-mods.js fabric   "<folder>"
//   node tools/fetch-test-mods.js neoforge "<folder>"
//   node tools/fetch-test-mods.js fabric --ids      prints the mod ids the game test should find loaded
const fs = require('fs'), path = require('path'), crypto = require('crypto');

const MC = '26.3';
// Vibrant Parrots is the other 26.3 bird pet (shoulder and seed-taming must not fight); Cozy Core
// should count a budgie as a pet; Cosy Critters draws ambient birds on the client (Fabric only).
const MODRINTH = {
	fabric: ['vibrant-parrots', 'puzzles-lib', 'forge-config-api-port', 'cosy-critters'],
	neoforge: ['vibrant-parrots', 'puzzles-lib'],
};
const IDS = {
	fabric: ['vibrantparrots', 'puzzleslib', 'cosycritters', 'cozy_core'],
	neoforge: ['vibrantparrots', 'puzzleslib', 'cozy_core'],
};
const UA = { 'User-Agent': 'gabmaybelle/little-birds test setup' };

(async () => {
	const [loader, folder] = process.argv.slice(2);
	if (!MODRINTH[loader] || !folder) throw new Error('usage: node tools/fetch-test-mods.js fabric|neoforge "<folder>" | --ids');
	if (folder === '--ids') { console.log(IDS[loader].join(',')); return; }
	const mods = path.join(folder, 'mods');
	fs.mkdirSync(mods, { recursive: true });
	for (const slug of MODRINTH[loader]) {
		const q = `loaders=${encodeURIComponent(`["${loader}"]`)}&game_versions=${encodeURIComponent(`["${MC}"]`)}`;
		const versions = await (await fetch(`https://api.modrinth.com/v2/project/${slug}/version?${q}`, { headers: UA })).json();
		if (!versions.length) throw new Error(`${slug} has no ${MC} ${loader} version`);
		const file = versions[0].files.find(f => f.primary) || versions[0].files[0];
		const out = path.join(mods, file.filename);
		if (fs.existsSync(out)) { console.log(`have ${file.filename}`); continue; }
		const buf = Buffer.from(await (await fetch(file.url, { headers: UA })).arrayBuffer());
		const sha = crypto.createHash('sha512').update(buf).digest('hex');
		if (sha !== file.hashes.sha512) throw new Error(`${file.filename}: SHA-512 mismatch, not saved`);
		fs.writeFileSync(out, buf);
		console.log(`got ${file.filename} (${(buf.length / 1048576).toFixed(1)} MB, SHA-512 ok)`);
	}
	// Cozy Core: the newest GitHub release's jar for this loader.
	const release = await (await fetch('https://api.github.com/repos/gabmaybelle/cozy-core/releases/latest', { headers: UA })).json();
	const asset = (release.assets || []).find(a => a.name.endsWith(`-${loader}.jar`));
	if (!asset) throw new Error(`Cozy Core ${release.tag_name || ''} has no ${loader} jar`);
	const out = path.join(mods, asset.name);
	if (fs.existsSync(out)) { console.log(`have ${asset.name}`); return; }
	const buf = Buffer.from(await (await fetch(asset.browser_download_url, { headers: UA })).arrayBuffer());
	fs.writeFileSync(out, buf);
	console.log(`got ${asset.name} (${(buf.length / 1048576).toFixed(1)} MB, from GitHub release ${release.tag_name})`);
})().catch(e => { console.error(e.message); process.exit(1); });
