void function(){
	
	var fs = require('fs');
	var path = require('path');

	function readFileText(filename) {
		if (!fs.existsSync(filename)) return "";
		return String(fs.readFileSync(filename));
	}

	function writeFileText(filename, text) {
		console.log(["writeFileText:", filename].join(" "));
		fs.writeFileSync(filename, text)
	}

	function processFile(sourceFile, destFile, jsFile, cssFile, release) {
		if (!sourceFile || !destFile || !jsFile || !cssFile) return;
		console.log(["Processing:", sourceFile, destFile, jsFile, cssFile].join(" "));
		var content = readFileText(sourceFile);
		if (!content) return;
		var match = sourceFile.match(/^(.*[\/\\])[^\/\\]*$/) || [];
		var sourceDir = match[1] || "";
		var styleContent = [];
		var jsContent = [];
		var random = release ? '?v=' + parseInt(Math.random() * 99999999).toString(36) : "";
		content = content.replace(/[ \f\t\v]*<!--\s*debug\s+start\s*-->[\s\S]*?<!--\s*debug\s+end\s*-->[ \f\t\v]*/ig, "")
			.replace(/-test(?=\.js)/g, "")
			.replace(/([(\uff08][\s\S]*?[)\uff09])(?=\s*<\/title>)/, "")
			.replace(/[ \f\t\v]*<link\s+[^>]*?href="(?!https?:)(?!components)(?!.*_ie6\.css)([^"]+\.css)"[^>]*?>[ \f\t\v]*/ig, function(all, value) {
				var filename = sourceDir + value;
				styleContent.push(readFileText(filename));
				return styleContent.length == 1 ? ['\t\t<link rel="stylesheet" type="text/css" href="resources/themes/default/styles/', cssFile.replace(/^(.*?)\\(?=[^\\]+$)/, ""), random, '" />'].join("") : "";
			})
			.replace(/[ \f\t\v]*<script\s+[^>]*?src="(?!https?:)([^"]+\.js)"[^>]*?>[\s\S]*?<\/script>[ \f\t\v]*/ig, function(all, value) {
				var filename = sourceDir + value;
				jsContent.push(readFileText(filename));
				return jsContent.length == 1 ? ['\t\t<script src="', jsFile.replace(/^(.*?)\\(?=[^\\]+$)/, ""), random, '"></script>'].join("") : "";
			})
			.replace(/[ \f\t\v]*<!--\s*\w+\s+(start|end)\s*-->[ \f\t\v]*/ig, "", "")
			.replace(/\s*\n(?=\s*\n)/g, "");
		writeFileText(cssFile, styleContent.join("\n").replace(/[ \f\t\v]*\/\*\s*debug\s+start\s*\*\/[\s\S]*?\/\*\s*debug\s+end\s*\*\/[ \f\t\v]*/ig, ""));
		writeFileText(jsFile, jsContent.join("\n").replace(/[ \f\t\v]*\/\*\s*debug\s+start\s*\*\/[\s\S]*?\/\*\s*debug\s+end\s*\*\/[ \f\t\v]*/ig, ""));
		writeFileText(destFile, content);
	}

	if (process.argv.length < 6) return;
	processFile(process.argv[2], process.argv[3], process.argv[4], process.argv[5], process.argv[6] == "release");
}();
