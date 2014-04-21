var AceString = typeof exports == 'undefined' ? AceString || {} : exports;
void function(exports){
    /**
     * Ace Engine String
     * 字符串编码处理
     * @see http://code.google.com/p/ace-engine/wiki/AceString
     * @author 王集鹄(wangjihu，http://weibo.com/zswang)
     * @version 1.0
     * @copyright www.baidu.com
     */
    /**
     * 对字符串进行utf8编码
     * param{string} str 原始字符串
     */
    function encodeUTF8(str) {
        if (!str) return;
        return String(str).replace(
            /[\u0080-\u07ff]/g,
            function(c) {
                var cc = c.charCodeAt(0);
                return String.fromCharCode(0xc0 | cc >> 6, 0x80 | cc & 0x3f);
            }
        ).replace(
            /[\u0800-\uffff]/g,
            function(c) {
                var cc = c.charCodeAt(0);
                return String.fromCharCode(0xe0 | cc >> 12, 0x80 | cc >> 6 & 0x3f, 0x80 | cc & 0x3f);
            }
        );
    }
    
    /**
     * 格式化函数
     * @param {String} template 模板
     * @param {Object} json 数据项
     */
    function format(template, json){
        return template.replace(/#\{(.*?)\}/g, function(all, key){
            return json && (key in json) ? json[key] : "";
        });
    }

    var crc32dict;
    function crc32(str){
        var i, j, k;
        if (!crc32dict){
            crc32dict = [];
            for (i = 0; i < 256; i++) {
                k = i;
                for (j = 8; j--;)
                    if (k & 1) 
                        k = (k >>> 1) ^ 0xedb88320;
                    else k >>>= 1;
                crc32dict[i] = k;
            }
        }
        
        str = encodeUTF8(str);
        k = -1;
        for (i = 0; i < str.length; i++) {
            k = (k >>> 8) ^ crc32dict[(k & 0xff) ^ str.charCodeAt(i)];
        }
        return -1 ^ k;
    }
    
	function str2bytes(str) {
		if (!str) return;
		str = encodeUTF8(str);
		var result = new (typeof Uint8Array == 'undefined' ? Array : Uint8Array)(str.length);
		for (var i = str.length - 1; i >= 0; i--) {
			result[i] = str.charCodeAt(i) & 0xff;
		}
		return result;
	}
	
    exports.format = format;
    exports.crc32 = crc32;
	exports.str2bytes = str2bytes;
}(AceString);