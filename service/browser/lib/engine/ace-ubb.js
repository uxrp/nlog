var AceUbb = AceUbb || {};

void function(exports){
    var plugins = {};
    
    function addPlugin(name, process){
        plugins[name] = process;
    }

    function exportHtml(str) {
        str = String(str).replace(/[&"<> \t\n\r]/g, function(all) {
            return ({
                "&": "&amp;"
                , "\"": "&quot;"
                , "<": "&lt;"
                , ">": "&gt;"
                , " ": "&nbsp;"
                , "\n": "<br/>"
                , "\r": ""
                , "\t": new Array(5).join("&nbsp;")
            })[all];
        });
        
        for (var p in plugins){
            if (plugins[p]){
                str = plugins[p](str);
            }
        }
        
        while ((/\[(i|u|s|sub|sup|small|big|left|center|right|img|url)\]\[\/\1\]/i).test(str))
            str = str.replace(/\[(i|u|s|sub|sup|small|big|left|center|right|img|url)\]\[\/\1\]/ig, ''); // 去掉无意义的
            
        str = str.replace(/\[size=(\d{1,2}(\.\d+)?(px|pt|in|cm|mm|pc|em|ex|%)+?)\](.*?)\[\/size\]/ig, '<font style="font-size:$1">$4</font>')
            .replace(/\[b\](.*?)\[\/b\]/ig, '<strong>$1</strong>');
        while ((/\[(i|u|s|sub|sup|small|big)\](.*?)\[\/\1\]/i).test(str))
            str = str.replace(/\[(i|u|s|sub|sup|small|big)\](.*?)\[\/\1\]/ig, '<$1>$2</$1>');
        str = str.replace(/\[(left|center|right)\](.+)\[\/\1]/ig, '<p align="$1">$2</p>');
        str = str.replace(/\[color=\s*(#[a-f0-9]{6}|red|orange|yellow|green|blue|indigo|violet|beige|black|brown|gray|navy|silver|tan)\s*\](.*?)\[\/color\]/ig, '<font style="color:$1">$2</font>')
            .replace(/\[img=\s*((https?:\/)?\/.*?)\](.*?)\s*\[\/img\]/ig, '<img src="$1" title="$3" alt="$3"/>')
            .replace(/\[img\]\s*((https?:\/)?\/.*?)\[\/img\]/ig, '<img src="$1" alt="img"/>')
            .replace(/\[url=\s*((https?:\/)?\/.*?)\](.*?)\s*\[\/url\]/ig, '<a href="$1" target="_blank">$3</a>')
            .replace(/\[url\]\s*((https?:\/)?\/.*?)\[\/url\]/ig, '<a href="$1" alt="url" target="_blank">$1</a>')
            .replace(/\[(br|hr)\]/ig, '<$1/>')
            .replace(/\[h([1-6])\](.+)\[\/h\1\]/ig, '<h$1>$2</h$1>');
            
        do {
            var length = str.length;
            str = str.replace(/\[quote=(.*?)\](.*?)\[\/quote\]/ig, '<fieldset><legend>$1</legend>$2</fieldset>');
        }  while (str.length != length)
        
        return str;
    }
    
    exports.exportHtml = exportHtml;
    exports.addPlugin = addPlugin;
}(AceUbb);