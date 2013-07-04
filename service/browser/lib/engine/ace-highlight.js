/*--
标题：代码高亮控件
设计：王集鹄
博客：http://blog.csdn.net/zswang
日期：2009年5月15日
--*/

//2010年4月20日 王集鹄 添加 VB语法、处理javascript字符串“\换行”的情况

var AceHighlight = AceHighlight || {};

void function(exports){

    var css = '\
.Code{font-family:Courier New;font-size:14px;background-color:#EEEEEE;}\n\
.Code .Whitespace{}\n\
.Code .Regex{color:Lime;}\n\
.Code .LineComment, .MultiComment{font-weight:bold;font-style:italic;color:Gray;}\n\
.Code .CompilerDirective{color:Green;}\n\
.Code .Symbol{color:Blue;}\n\
.Code .Number, .Float{color:Red;font-weight:bold;}\n\
.Code .String, .Character{color:Maroon;}\n\
.Code .Identifier{}\n\
.Code .Reserved{font-weight:bold;}\n\
.Code .Type{color:#2B91AF;}\n\
.Code .Variant{color:Red;}\n\
.Code .Function{color:Fuchsia;}\n\
.Code .Unknown{font-style:italic;background-color:Black;color:White;}\n\
';

    var languages = {
        attribute: {
            syntaxs: [  
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^(=|:|;|\/)/, name: "Symbol" }
                , { pattren: /^('[^']*'|"[^"]*")/, name: "String" }
                , { pattren: /^[\w\/\-_]+/, name: "Variant" }
            ]
        }
        , property: {
            syntaxs: [ 
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^\/\*[\s\S]*?\*\//, name: "MultiComment" }
                , { pattren: /^([a-zA-Z0-9_\-]+)(\s*)(:)(\s*)([^;]*)(;|$)/, methods: ["Variant", "Whitespace", "Symbol", "Whitespace", "String", "Symbol"] }
            ]
        }
        , css: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^\/\*[\s\S]*?\*\//, name: "MultiComment" }
                , { pattren: /^(((\.|#)[a-zA-Z0-9_\-]+[\s,]*)+)(\{)([\s\S]*?)(\})/,  methods: ["Function", null, null, "Symbol", "property", "Symbol"] }
                
            ]
        }
        , javascript: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^\/\/.*/, name: "LineComment" }
                , { pattren: /^\/\*[\s\S]*?\*\//, name: "MultiComment" }
                , { pattren: /^\/([^\\\/\n\r]|\\.)+\/([img]{0,3})/, name: "Regex", start: ["", "Symbol"] }
                , { pattren: /^(~|\+|\-|\*|\(|\)|\[|\]|\{|\}|\||\;|:|=|<|>|\.|\,|\%|\!|&|\?|\^)+/, name: "Symbol" }
                , { pattren: /^\//, name: "Symbol" }
                , { pattren: /^(\d+(?!\.|x|e|d|m)u?)|^0x([\da-fA-F]+(?!\.|x|m)u?)|^NaN/, name: "Number" }
                , { pattren: /^(\d+)?\.\d+((\+|\-)?e\d+)?(m|d|f)?|^\d+((\+|\-)?e(\+|\-)?\d+)?(m|d|f)?/, name: "Float" }
                , { pattren: /^"([^\\"]|(\\[\s\S]))*"/, name: "String" }
                , { pattren: /^'([^\\']|(\\[\s\S]))*'/, name: "String" }
                , { pattren: /^[\w$_]+/, name: "Identifier" }
            ]
            , identifiers: [
                { pattren: /^(break|delete|function|return|typeof|case|do|if|switch|var|catch|else|in|this|void|continue|false|instanceof|throw|while|debugger)$/, name: "Reserved" }
                , { pattren: /^(finally|new|true|with|default|for|null|try|abstract|double|goto|native|static|boolean|enum|implements|package|super|byte|prototype)$/, name: "Reserved" }
                , { pattren: /^(export|import|private|synchronized|char|extends|int|protected|throws|class|final|interface|public|transient|const|float|long|short|volatile)$/, name: "Reserved" }
                , { pattren: /^(Array|String|Boolean|undefined|Object|Enumerator|Error|Math)$/, name: "Type" }
            ]
        }
        ,  vb: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^(~|\+|\-|\*|\(|\)|\[|\]|\{|\}|\||\;|:|=|<|>|\.|\,|\%|\!|&|\?|\^)+/, name: "Symbol" }
                , { pattren: /^(\d+(?!\.|x|e|d|m)u?)|^0x([\da-fA-F]+(?!\.|x|m)u?)|^NaN/, name: "Number" }
                , { pattren: /^(\d+)?\.\d+((\+|\-)?e\d+)?(m|d|f)?|^\d+((\+|\-)?e(\+|\-)?\d+)?(m|d|f)?/, name: "Float" }
                , { pattren: /^"[^"]*"/, name: "String" }
                , { pattren: /^('.*|REM\b.*')/, name: "LineComment" }
                , { pattren: /^#.*/, name: "CompilerDirective" }
                , { pattren: /^[\w$_]+/, name: "Identifier" }
            ]
            , identifiers: [
                { pattren: /^(AddHandler|AddressOf|Alias|And|AndAlso|As|Boolean|ByRef|Byte|ByVal|Call|Case|Catch|CBool|CByte|CChar|CDate|CDec|CDbl|Char|CInt|Class|CLng|CObj|Const|Continue)$/i, name: "Reserved" }
                , { pattren: /^(CSByte|CShort|CSng|CStr|CType|CUInt|CULng|CUShort|Date|Decimal|Declare|Default|Delegate|Dim|DirectCast|Do|Double|Each|Else|ElseIf|End|EndIf|Enum|Erase|Error)$/i, name: "Reserved" }
                , { pattren: /^(Event|Exit|False|Finally|For|Friend|Function|Get|GetType|GetXMLNamespace|Global|GoSub|GoTo|Handles|If|Implements|Imports|Imports|In|Inherits|Integer|Interface)$/i, name: "Reserved" }
                , { pattren: /^(Is|IsNot|Let|Lib|Like|Long|Loop|Me|Mod|Module|MustInherit|MustOverride|MyBase|MyClass|Namespace|Narrowing|New|Next|Not|Nothing|NotInheritable|NotOverridable)$/i, name: "Reserved" }
                , { pattren: /^(Object|Of|On|Operator|Option|Optional|Or|OrElse|Overloads|Overridable|Overrides|ParamArray|Partial|Private|Property|Protected|Public|RaiseEvent|ReadOnly|ReDim)$/i, name: "Reserved" }
                , { pattren: /^(RemoveHandler|Resume|Return|SByte|Select|Set|Shadows|Shared|Short|Single|Static|Step|Stop|String|Structure|Sub|SyncLock|Then|Throw|To|True|Try|TryCast)$/i, name: "Reserved" }
                , { pattren: /^(TypeOf|Variant|Wend|UInteger|ULong|UShort|Using|When|While|Widening|With|WithEvents|WriteOnly|Xor)$/i, name: "Reserved" }
            ]
        }
        , xhtml: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^<!--[\s\S]*?-->/, name: "MultiComment" }
                , { pattren: /^(<!)([\s\S]*?)(>)/, methods: ["Symbol", "attribute", "Symbol"] }
                , { pattren: /^(<\?)(xml\b)([\s\S]*?)(\?>)/, methods: ["Symbol", "Reserved", "attribute", "Symbol"] }
                , { pattren: /^(<\?)(php\b)(.*?)(\?>)/, methods: ["Symbol", "Reserved", "php", "Symbol"] }
                , { pattren: /^(<\?)(php\b)([\s\S]*?)(\n\?>|$)/, methods: ["Symbol", "Reserved", "php", "Symbol"] }
                , { pattren: /^(<%@)([\s\S]*?)(%>)/, methods: ["Symbol", "attribute", "Symbol"] }
                , { pattren: /^(<%)([\s\S]*?)(%>)/, methods: ["Symbol", "Text", "Symbol"] }
                , { pattren: /^(<)(style\b)(([^>"']*(".*?"|'.*?')*)*?)(>)([\s\S]*?)(<\/)(style)(>)/i, methods: ["Symbol", "Type", "attribute", null, null, "Symbol", "css", "Symbol", "Type", "Symbol"] }
                , { pattren: /^(<)(script\b)([^>]*("|')(text\/)?vbscript("|'))(([^>"']*(".*?"|'.*?')*)*?)(>)([\s\S]*?)(<\/)(script)(>)/i, methods: ["Symbol", "Type", "attribute", null, null, null, null, null, null, "Symbol", "vb", "Symbol", "Type", "Symbol"] }
                , { pattren: /^(<)(script\b)([^>]*("|')(text\/)?csharp("|'))(([^>"']*(".*?"|'.*?')*)*?)(>)([\s\S]*?)(<\/)(script)(>)/i, methods: ["Symbol", "Type", "attribute", null, null, null, null, null, null, "Symbol", "csharp", "Symbol", "Type", "Symbol"] }
                , { pattren: /^(<)(script\b)(([^>"']*(".*?"|'.*?')*)*?)(>)([\s\S]*?)(<\/)(script)(>)/i, methods: ["Symbol", "Type", "attribute", null, null, "Symbol", "javascript", "Symbol", "Type", "Symbol"] }
                , { pattren: /^(<\/?)([\w\-]+)(([^>"']*(".*?"|'.*?')*)*?)(>)/i, methods: ["Symbol", "Type", "attribute", null, null, "Symbol"] }
                , { pattren: /^[^<]+/i, name: "Text" }
            ]
        }
        , php: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^(\/\/|#).*/, name: "LineComment" }
                , { pattren: /^\/\*[\s\S]*?\*\//, name: "MultiComment" }
                , { pattren: /^(~|\+|\-|\*|\(|\)|\[|\]|\{|\}|\||\;|:|=|<|>|\.|\,|\%|\!|&|\?|\^|@)+/, name: "Symbol" }
                , { pattren: /^\//, name: "Symbol" }
                , { pattren: /^(\d+(?!\.|x|e|d|m)u?)|^0x([\da-fA-F]+(?!\.|x|m)u?)|^NaN/, name: "Number" }
                , { pattren: /^(\d+)?\.\d+((\+|\-)?e\d+)?(m|d|f)?|^\d+((\+|\-)?e(\+|\-)?\d+)?(m|d|f)?/, name: "Float" }
                , { pattren: /^"([^\\"]|\\.)*"/, name: "String" }              
                , { pattren: /^'([^\\']|\\.)*'/, name: "String" }
                , { pattren: /^\$[\w_]*/, name: "Variant" }
                , { pattren: /^[\w_]+/, name: "Identifier" }
            ]
            , identifiers: [
                { pattren: /^(require_once|include|and|or|xor|__FILE__|exception|php_user_filter|__LINE__|array|as|break|case|cfunction|class|const|continue|declare|default|die|do|each|echo|else|elseif|empty)$/, name: "Reserved" }
                , { pattren: /^(enddeclare|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|for|foreach|function|global|if|isset|list|new|old_function|print|return|static|switch|unset|use|var|while|__FUNCTION__|__CLASS__|__METHOD__)$/, name: "Reserved" }
                , { pattren: /^(NULL|FALSE|TRUE)$/, name: "Variant" }
            ]
        }
        , csharp: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^\/\/.*/, name: "LineComment" }
                , { pattren: /^\/\*[\s\S]*?\*\//, name: "MultiComment" }
                , { pattren: /^#.*/, name: "CompilerDirective" }
                , { pattren: /^(~|\+|\-|\*|\(|\)|\[|\]|\{|\}|\||\;|:|=|<|>|\.|\,|\%|\!|&|\?|\/)+/, name: "Symbol" }
                , { pattren: /^(\d+(?!\.|x|e|d|m)u?)|^0x([\da-fA-F]+(?!\.|x|m)u?)/, name: "Number" }
                , { pattren: /^(\d+)?\.\d+((\+|\-)?e\d+)?(m|d|f)?|^\d+((\+|\-)?e(\+|\-)?\d+)?(m|d|f)?/, name: "Float" }
                , { pattren: /^@"(""|[^"])*"|^"([^\\"]|\\.)*"/, name: "String" }
                , { pattren: /^\'([^\']|\\.)+\'/, name: "Character" }
                , { pattren: /^[\w_]+|^@[\w_]+/, name: "Identifier" }
            ]
            , identifiers: [
                { pattren: /^(abstract|as|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|do|double|else|enum)$/, name: "Reserved" }
                , { pattren: /^(event|explicit|extern|false|finally|fixed|float|for|foreach|goto|if|implicit|in|int|interface|internal|is|lock|long|namespace)$/, name: "Reserved" }
                , { pattren: /^(new|null|object|operator|out|override|params|private|protected|public|readonly|ref|return|sbyte|sealed|short|sizeof|stackalloc)$/, name: "Reserved" }
                , { pattren: /^(static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|virtual|void|volatile|while)$/, name: "Reserved" }
                , { pattren: /^(select|from|where|order|by|group|orderby|into)$/, name: "Reserved" }
                , { pattren: /^[A-Z][\w_]*$/, name: "Type" }
            ]
        }
        , delphi: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^\/\/.*/, name: "LineComment" }
                , { pattren: /^\{\$[\s\S]*?\}|^\(\*$[\s\S]*?\*\)/, name: "CompilerDirective" }
                , { pattren: /\(\*[\s\S]*?\*\)|^\{[\s\S]*?\}/, name: "MultiComment" }
                , { pattren: /^\d+|^\$[\da-f]+/i, name: "Number" }
                , { pattren: /^(\d+)?\.\d+((\+|\-)?e\d+)?|^\d+((\+|\-)?e(\+|\-)?\d+)?/, name: "Float" }
                , { pattren: /^(\+|\-|\*|\/|\(|\)|\[|\]|\{|\}|\||\;|:|=|<|>|\.|\,|\%|\^|@)+/, name: "Symbol" }
                , { pattren: /^'(''|[^'])*'/, name: "String" }
                , { pattren: /^#\d+|^#$[\da-f]+/i, name: "Character" }
                , { pattren: /^[\w_]+/, name: "Identifier" }
            ]
            , identifiers: [
                { pattren: /^(and|else|inherited|packed|then|array|end|initialization|procedure|threadvar|as|except|inline|program)$/i, name: "Reserved" }
                , { pattren: /^(to|asm|exports|interface|property|try|begin|file|is|raise|type|case|final|label|record|unit)$/i, name: "Reserved" }
                , { pattren: /^(class|finalization|library|repeat|unsafe|const|finally|mod|resourcestring|until|constructor|for|nil)$/i, name: "Reserved" }
                , { pattren: /^(sealed|uses|destructor|function|not|set|var|dispinterface|goto|object|shl|while|div|if|of|shr|with|do)$/i, name: "Reserved" }
                , { pattren: /^(implementation|or|static|xor|downto|in|out|string)$/i, name: "Reserved" }
                , { pattren: /^(absolute|dynamic|local|platform|requires|abstract|export|message|private|resident)$/i, name: "Reserved" }
                , { pattren: /^(assembler|external|name|protected|safecall|automated|far|near|public|stdcall|cdecl)$/i, name: "Reserved" }
                , { pattren: /^(forward|nodefault|published|stored|contains|implements|overload|read|varargs|default)$/i, name: "Reserved" }
                , { pattren: /^(index|override|readonly|virtual|deprecated|inline|package|register|write|dispid|library|pascal|reintroduce|writeonly)$/i, name: "Reserved" }
                , { pattren: /^(True|False)$/i, name: "Variant" }
                , { pattren: /^(Exit|Break|Continue|Str|Val)$/i, name: "Function" }
                , { pattren: /^(((I|T|C|P)[A-Z]\w+)|Integer|Cardinal|Shortint|Smallint|Longint|Int64|Longword|Boolean|String|Char|Byte|Double|Word|Real48|Single|Extended|Comp|Currency)$/, name: "Type" }
            ]
        }
        , mssql: {
            syntaxs: [
                { pattren: /^\s+/, name: "Whitespace" }
                , { pattren: /^-{2,}.*/, name: "LineComment" }
                , { pattren: /^\/\*[\s\S]*?\*\//, name: "MultiComment" }
                , { pattren: /^(\+|\-|\*|\/|\(|\)|\{|\}|\||\;|:|=|<|>|\.|\,|\%|\!)+/, name: "Symbol" }
                , { pattren: /^(^\d+)|(^\$[\da-f]+)/, name: "Number" }
                , { pattren: /^(\d+)?\.\d+((\+|\-)?e\d+)?|^\d+((\+|\-)?e(\+|\-)?\d+)?/, name: "Float" }
                , { pattren: /^(N)?'([^']|'')*'/, name: "String" }
                , { pattren: /^@@?\w+/, name: "Variant" }
                , { pattren: /^([\w_]+|\[[\s\S]*?\])/, name: "Identifier" }
            ]
            , identifiers: [
                { pattren: /^(ENCRYPTION|ORDER|ADD|END|OUTER|ALL|ERRLVL|OVER|ALTER|ESCAPE|PERCENT|AND|EXCEPT|PLAN|ANY|EXEC|PRECISION|AS|EXECUTE|PRIMARY|ASC|EXISTS|PRINT|AUTHORIZATION|EXIT|PROC|AVG|EXPRESSION)$/i, name: "Reserved" }
                , { pattren: /^(PROCEDURE|BACKUP|FETCH|PUBLIC|BEGIN|FILE|RAISERROR|BETWEEN|FILLFACTOR|READ|BREAK|FOR|READTEXT|BROWSE|FOREIGN|RECONFIGURE|BULK|FREETEXT|REFERENCES|BY|FREETEXTTABLE|REPLICATION|CASCADE|FROM|RESTORE|CASE|FULL|RESTRICT)$/i, name: "Reserved" }
                , { pattren: /^(CHECK|FUNCTION|RETURN|CHECKPOINT|GOTO|REVOKE|CLOSE|GRANT|RIGHT|CLUSTERED|GROUP|ROLLBACK|COALESCE|HAVING|ROWCOUNT|COLLATE|HOLDLOCK|ROWGUIDCOL|COLUMN|IDENTITY|RULE|COMMIT|IDENTITY_INSERT|SAVE)$/i, name: "Reserved" }
                , { pattren: /^(COMPUTE|IDENTITYCOL|SCHEMA|CONSTRAINT|IF|SELECT|CONTAINS|IN|SESSION_USER|CONTAINSTABLE|INDEX|SET|CONTINUE|INNER|SETUSER|CONVERT|INSERT|SHUTDOWN|COUNT|INTERSECT|SOME|CREATE|INTO|STATISTICS)$/i, name: "Reserved" }
                , { pattren: /^(CROSS|IS|SUM|CURRENT|JOIN|SYSTEM_USER|CURRENT_DATE|KEY|TABLE|CURRENT_TIME|KILL|TEXTSIZE|CURRENT_TIMESTAMP|LEFT|THEN|CURRENT_USER|LIKE|TO|CURSOR|LINENO|TOP|DATABASE|LOAD|TRAN|DATABASEPASSWORD)$/i, name: "Reserved" }
                , { pattren: /^(MAX|TRANSACTION|DATEADD|MIN|TRIGGER|DATEDIFF|NATIONAL|TRUNCATE|DATENAME|NOCHECK|TSEQUAL|DATEPART|NONCLUSTERED|UNION|DBCC|NOT|UNIQUE|DEALLOCATE|NULL|UPDATE|DECLARE|NULLIF|UPDATETEXT|DEFAULT)$/i, name: "Reserved" }
                , { pattren: /^(OF|USE|DELETE|OFF|USER|DENY|OFFSETS|VALUES|DESC|ON|VARYING|DISK|OPEN|VIEW|DISTINCT|OPENDATASOURCE|WAITFOR|DISTRIBUTED|OPENQUERY|WHEN|DOUBLE|OPENROWSET|WHERE|DROP|OPENXML|WHILE|DUMP|OPTION|WITH|ELSE|OR|WRITETEXT|OUTPUT|INPUT|OUT|PUT)$/i, name: "Reserved" }
                , { pattren: /^(AVG|COUNT||MAX|MIN||DATEADD|DATEDIFF||DATENAME|DATEPART|GETDATE|ABS|ACOS|ASIN|ATAN|ATN2|CEILING|COS|COT|DEGREES||EXP|FLOOR|LOG|LOG10|PI)$/i, name: "Function" }
                , { pattren: /^(POWER|RADIANS|RAND||ROUND|SIGN|SIN|SQRT|TAN|NCHAR|CHARINDEX|LEN|LOWERLTRIM|PATINDEX|REPLACEREPLICATERTRIMSPACE|STR|STUFF|SUBSTRING|UNICODE|UPPER|CASE|COALESCE|CONVERT|DATALENGTH|NEWID)$/i, name: "Function" }
                , { pattren: /^(bigint|int|smallint|tinyint|bit|decimal|numeric|money|smallmoney|float|real|datetime|smalldatetime|char|varchar|text|nchar|nvarchar|ntext|binary|varbinary|image|cursor|sql_variant|table|timestamp|uniqueidentifier)$/i, name: "Type" }
            ]
        }
    };
    
    function whatLanguage(code) {
        if (/^\s*<[\s\S]*>\s*/.test(code)) return languages["xhtml"];
        if (/^\s*(unit|program|library)\s+\w+(\.\w+)*\s*;/.test(code)) return languages["delphi"];
        if (/^\s*(uses)\s+\w+(\.\w+)*\s*(,|;)/.test(code)) return languages["delphi"];
        if (/^\s*(select|insert|update|create|delete)\s*/.test(code)) return languages["mssql"];
        if (/^\s*(using|namespace)\s*\w+(\.\w+)*\s*;/.test(code)) return languages["csharp"];
        if (/^\s*\[\s*\w+([^\]]*)\s*\]/.test(code)) return languages["csharp"];
        if (/^dim\s*\w+as\s*\w+/i.test(code)) return languages["vb"];
        if (/^('|REM\b)/i.test(code)) return languages["vb"];
        if (/^(public|private|protected|friend)(\s*\w+)*_\s*(\r|\n)/i.test(code)) return languages["vb"];
        return languages["javascript"];
    }
    
    function analyze(code, lang) {
        if (!lang) return null;
        var result = [];
        var find = true;
        var last = "";
        while (find && code) {
            find = false;
            var j;
            for (var i = 0; i < lang.syntaxs.length; i++) {
                if (lang.syntaxs[i].start) {
                    var exists = false;
                    for (j = 0; j < lang.syntaxs[i].start.length; j++) {
                        if (last == lang.syntaxs[i].start[j]) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) continue;
                }
                var match = lang.syntaxs[i].pattren.exec(code);
                if (match) {
                    var methods = lang.syntaxs[i].methods;
                    if (methods) {
                        for (j = 0; j < methods.length; j++) {
                            var method = methods[j];
                            if (!method) continue;
                            if (!match[j + 1]) continue;
                            if (languages[method] && languages[method].syntaxs) {
                                var items = analyze(match[j + 1], languages[method]);
                                for (var k = 0; k < items.length; k++) {
                                    result.push(items[k]);
                                }
                            } else result.push({ name: method, text: match[j + 1] });
                        }
                    } else {
                        if (lang.syntaxs[i].name) {
                            var item = { name: lang.syntaxs[i].name, text: match[0] };
                            if (item.name == "Identifier" && lang.identifiers) {
                                for (j = 0; j < lang.identifiers.length; j++)
                                    if (lang.identifiers[j].pattren.test(item.text))
                                        item.name = lang.identifiers[j].name;
                            }
                            if (!/^(Whitespace|LineComment|MultiComment)$/i.test(item.name)
                                && !/\)$/.test(item.text)) last = item.name;
                            result.push(item);
                        }
                    }
                    code = code.substr(match[0].length);
                    find = true;
                    break;
                }
            }
        }
        if (!find && code) result.push({ name: "Unknown", text: code });
        return result;
    }
    var languageStyle;
    function exportHtml(code, lang) {
        if (!languageStyle){
            if (document.createStyleSheet){
                languageStyle = document.createStyleSheet();
                languageStyle.cssText = css;
            } else {
                languageStyle = document.createElement("style");
                languageStyle.setAttribute("type", "text/css");
                languageStyle.appendChild(document.createTextNode(css));
                document.getElementsByTagName('script')[0].parentNode.appendChild(languageStyle);
            }
        }
        lang = lang || whatLanguage(code);
        var items = analyze(code, lang);
        var html = [];
        html.push("<pre class=\"Code\"><code>");
        for (var i = 0; i < items.length; i++)
            html.push("<span class=\"" + items[i].name + "\">"
                + items[i].text.replace(/</g, "&lt;").replace(/>/g, "&gt;") 
                + "</span>");
        html.push("</pre></code>");
        return html.join("");
    }
    
    exports.analyze = analyze;
    exports.exportHtml = exportHtml;
    
}(AceHighlight);