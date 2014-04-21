var NLog = typeof exports == 'undefined' ? NLog || {} : exports;
void function(exports) {
	exports.send = function(url, data) {
		var plain = AceString.str2bytes(data);
		var gzip = new Zlib.Gzip(plain);
		var bytes = gzip.compress();

		var xmlhttp = new XMLHttpRequest();
		
		xmlhttp.open("POST", url, true);

		xmlhttp.send(bytes);
	};
}(NLog)