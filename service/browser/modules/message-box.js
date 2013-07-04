/**
 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
 */
AceCore.addModule("MessageBox", function(sandbox){
    /**
     * 事件集合
     */
    var events = sandbox.getConfig("Events");
    /**
     * 类库
     */
    var lib = sandbox.getLib();
    /**
     * 消息列表
     */
    var messageTree;
    /**
     * 登录信息
     */
    var passportInfo = {};
    /**
     * 获取房间当前状态成功
     * @param {Object} data
     */
    function pickSuccess(data) {
        lib.each(data, function(item) {
            switch(item.type) {
                case "passport":
                    passportInfo = item.info;
                    break;
                case "messageAll":
                    messageTree.loadChilds(item.messages);
                    scrollBottom();
                    break;
                case "messageAdd":
                    messageTree.appendChilds(item.messages);
                    scrollBottom();
                    break;
            }
        });
    }
    /**
     * 滚动到底部
     */
    function scrollBottom() {
        var parent = messageTree.parent.parentNode;
        parent.scrollTop = parent.scrollHeight;
    }
    
    /**
     * 格式化时间
     * @param {Date} time
     */
    function formatTime(time) {
        time = new Date(time);
        var timeStr = lib.date.format(time, "HH:mm:ss");
        var dateStr = lib.date.format(time, "yyyy-MM-dd");
        return lib.date.format(new Date, "yyyy-MM-dd") == dateStr ? timeStr :
            [dateStr, timeStr].join(" ");
    }

    /**
     * 处理多行文本
     * @param {String} text 文本
     */
    function mutiline(text) {
        return lib.encodeHTML(text).replace(/\n/g, "<br/>");
    }
    
    
    function html2Text(html) {
        return String(html)
            .replace(/<br\s*\/>/g, "\n")
            .replace(/&amp;/g, "&")
            .replace(/&quot;/g, "\"")
            .replace(/&lt;/g, "<")
            .replace(/&gt;/g, ">")
            .replace(/&nbsp;/g, " ");
    }

    return {
        init: function() {
        
        AceUbb.addPlugin('code', function(text){
            return String(text).replace(/(\[code\])(.*?)(\[\/code\])/ig, function($0, $1, $2) {
                return "<pre><code>" + AceHighlight.exportHtml(html2Text($2)).replace(/\[/g, "&#91;").replace(/\[/g, "&#93;") + "</code></pre>";
            });
        });

        AceUbb.addPlugin('color', function(text){
            return String(text).replace(/\[(red|orange|yellow|green|blue|indigo|violet|beige|black|brown|gray|navy|silver|tan)\]([\s\S]*?)\[\/\1\]/g, function(all, color, text){
                return '<span style="color:' + color + ';">' + text + '</span>';
            });
        });

        AceUbb.addPlugin('weibo', function(text){
            var dict = {
'[bm做操]': '09/bmzuocao_thumb.gif',
'[bm抓狂]': '60/bmzhuakuang_thumb.gif',
'[bm中枪]': 'ff/bmzhongqiang_thumb.gif',
'[bm震惊]': '63/bmzhenjing_thumb.gif',
'[bm赞]': 'c9/bmzan_thumb.gif',
'[bm喜悦]': '47/bmxiyue_thumb.gif',
'[bm醒悟]': '8f/bmxingwu_thumb.gif',
'[bm兴奋]': 'a7/bmxingfen_thumb.gif',
'[bm血泪]': '0d/bmxielei_thumb.gif',
'[bm挖鼻孔]': 'bd/bmwabikong_thumb.gif',
'[bm吐舌头]': '8f/bmtushetou_thumb.gif',
'[bm吐槽]': '73/bmtucao_thumb.gif',
'[bm投诉]': '04/bmtousu_thumb.gif',
'[bm跳绳]': '2a/bmtiaosheng_thumb.gif',
'[bm调皮]': 'da/bmtiaopi_thumb.gif',
'[bm讨论]': '20/bmtaolun_thumb.gif',
'[bm抬腿]': '86/bmtaitui_thumb.gif',
'[bm思考]': '0f/bmsikao_thumb.gif',
'[bm生气]': '8f/bmshengqi_thumb.gif',
'[bm亲吻]': 'a4/bmqinwen_thumb.gif',
'[bm庆幸]': '6c/bmqingxing_thumb.gif',
'[bm内涵]': '66/bmneihan_thumb.gif',
'[bm忙碌]': '18/bmmanglu_thumb.gif',
'[bm乱入]': 'a9/bmluanru_thumb.gif',
'[bm卖萌]': 'ba/bmluanmeng_thumb.gif',
'[bm流泪]': 'd6/bmliulei_thumb.gif',
'[bm流口水]': 'a4/bmliukoushui_thumb.gif',
'[bm流鼻涕]': '4f/bmliubiti_thumb.gif',
'[bm路过]': '75/bmliguo_thumb.gif',
'[bm咧嘴]': '3e/bmliezui_thumb.gif',
'[bm啦啦队]': '4e/bmlaladui_thumb.gif',
'[bm哭诉]': '9b/bmkusu_thumb.gif',
'[bm哭泣]': 'a8/bmkuqi_thumb.gif',
'[bm苦逼]': 'dc/bmkubi_thumb.gif',
'[bm口哨]': '0c/bmkoushao_thumb.gif',
'[bm可爱]': '95/bmkeai_thumb.gif',
'[bm紧张]': '4c/bmjinzhang_thumb.gif',
'[bm惊讶]': '03/bmjingya_thumb.gif',
'[bm惊吓]': 'be/bmjingxia_thumb.gif',
'[bm焦虑]': '4e/bmjiaolv_thumb.gif',
'[bm会心笑]': '7e/bmhuixinxiao_thumb.gif',
'[bm坏笑]': 'ec/bmhuaixiao_thumb.gif',
'[bm花痴]': '4b/bmhuachi_thumb.gif',
'[bm厚脸皮]': '61/bmhoulianpi_thumb.gif',
'[bm好吧]': '16/bmhaoba_thumb.gif',
'[bm害怕]': '6c/bmhaipa_thumb.gif',
'[bm鬼脸]': '15/bmguilian_thumb.gif',
'[bm孤独]': 'e4/bmgudu_thumb.gif',
'[bm高兴]': '85/bmgaoxing_thumb.gif',
'[bm搞怪]': '4b/bmgaoguai_thumb.gif',
'[bm干笑]': 'b4/bmganxiao_thumb.gif',
'[bm感动]': '34/bmgandong_thumb.gif',
'[bm愤懑]': 'fc/bmfenmen_thumb.gif',
'[bm反对]': 'b6/bmfandui_thumb.gif',
'[bm踱步]': '54/bmduobu_thumb.gif',
'[bm顶]': '34/bmding_thumb.gif',
'[bm得意]': '7a/bmdeyi_thumb.gif',
'[bm得瑟]': '7d/bmdese_thumb.gif',
'[bm大笑]': '37/bmdaxiao_thumb.gif',
'[bm蛋糕]': 'f8/bmdangao_thumb.gif',
'[bm大哭]': 'dc/bmdaku_thumb.gif',
'[bm大叫]': '83/bmdajiao_thumb.gif',
'[bm吃惊]': 'b0/bmchijing_thumb.gif',
'[bm馋]': 'd9/bmchan_thumb.gif',
'[bm彩色]': '7e/bmcaise_thumb.gif',
'[bm缤纷]': '15/bmbinfen_thumb.gif',
'[bm变身]': 'b7/bmbianshen_thumb.gif',
'[bm悲催]': '77/bmbeicui_thumb.gif',
'[bm暴怒]': '12/bmbaonu_thumb.gif',
'[bm熬夜]': 'a4/bmaoye_thumb.gif',
'[bm暗爽]': 'bc/bmanshuang_thumb.gif',
'[月儿圆]': '3d/lxhyueeryuan_thumb.gif',
'[招财]': 'a9/lxhzhaocai_thumb.gif',
'[微博三岁啦]': '1e/lxhweibo3yr_thumb.gif',
'[复活节]': 'd6/lxhfuhuojie_thumb.gif',
'[挤火车]': '09/lxhjihuoche_thumb.gif',
'[愚人节]': '21/lxhyurenjie_thumb.gif',
'[收藏]': '83/lxhshoucang_thumb.gif',
'[喜得金牌]': 'a2/lxhhappygold_thumb.gif',
'[夺冠感动]': '69/lxhduoguan_thumb.gif',
'[冠军诞生]': '2c/lxhguanjun_thumb.gif',
'[传火炬]': 'f2/lxhchuanhuoju_thumb.gif',
'[奥运金牌]': '06/lxhgold_thumb.gif',
'[奥运银牌]': '43/lxhbronze_thumb.gif',
'[奥运铜牌]': 'fd/lxhsilver_thumb.gif',
'[德国队加油]': '12/germany_thumb.gif',
'[西班牙队加油]': 'be/spain_thumb.gif',
'[葡萄牙队加油]': 'f8/portugal_thumb.gif',
'[意大利队加油]': '03/italy_thumb.gif',
'[耍花灯]': 'be/lxhshuahuadeng_thumb.gif',
'[元宵快乐]': '83/lxhyuanxiaohappy_thumb.gif',
'[吃汤圆]': '52/lxhchitangyuan_thumb.gif',
'[金元宝]': '9b/lxhjinyuanbao_thumb.gif',
'[红包拿来]': 'bd/lxhhongbaonalai_thumb.gif',
'[福到啦]': 'f4/lxhfudaola_thumb.gif',
'[放鞭炮]': 'bd/lxhbianpao_thumb.gif',
'[发红包]': '27/lxhhongbao_thumb.gif',
'[大红灯笼]': '90/lxhdahongdenglong_thumb.gif',
'[拜年了]': '0c/lxhbainianle_thumb.gif',
'[龙啸]': 'cd/lxhlongxiao_thumb.gif',
'[光棍节]': '5b/lxh1111_thumb.gif',
'[蛇年快乐]': '5f/lxhshenian_thumb.gif',
'[过年啦]': '94/lxhguonianla_thumb.gif',
'[圆蛋快乐]': 'eb/lxhyuandan_thumb.gif',
'[发礼物]': '24/lxh_santa_thumb.gif',
'[要礼物]': 'd2/lxh_gift_thumb.gif',
'[平安果]': '0f/lxh_apple_thumb.gif',
'[吓到了]': 'fa/lxhscare_thumb.gif',
'[走你]': 'ed/zouni_thumb.gif',
'[吐血]': '8c/lxhtuxue_thumb.gif',
'[好激动]': 'ae/lxhjidong_thumb.gif',
'[没人疼]': '23/lxhlonely_thumb.gif',
'[转发]': '02/lxhzhuanfa_thumb.gif',
'[笑哈哈]': '32/lxhwahaha_thumb.gif',
'[得意地笑]': 'd4/lxhdeyidixiao_thumb.gif',
'[噢耶]': '3b/lxhxixi_thumb.gif',
'[偷乐]': 'fa/lxhtouxiao_thumb.gif',
'[泪流满面]': '64/lxhtongku_thumb.gif',
'[巨汗]': 'f6/lxhjuhan_thumb.gif',
'[抠鼻屎]': '48/lxhkoubishi_thumb.gif',
'[求关注]': 'ac/lxhqiuguanzhu_thumb.gif',
'[真V5]': '3a/lxhv5_thumb.gif',
'[群体围观]': 'a8/lxhweiguan_thumb.gif',
'[hold住]': '05/lxhholdzhu_thumb.gif',
'[羞嗒嗒]': 'df/lxhxiudada_thumb.gif',
'[非常汗]': '42/lxhpubuhan_thumb.gif',
'[许愿]': '87/lxhxuyuan_thumb.gif',
'[崩溃]': 'c7/lxhzhuakuang_thumb.gif',
'[好囧]': '96/lxhhaojiong_thumb.gif',
'[震惊]': 'e7/lxhchijing_thumb.gif',
'[别烦我]': '22/lxhbiefanwo_thumb.gif',
'[不好意思]': 'b4/lxhbuhaoyisi_thumb.gif',
'[纠结]': '1f/lxhjiujie_thumb.gif',
'[拍手]': 'e3/lxhguzhang_thumb.gif',
'[给劲]': 'a5/lxhgeili_thumb.gif',
'[好喜欢]': 'd6/lxhlike_thumb.gif',
'[好爱哦]': '74/lxhainio_thumb.gif',
'[路过这儿]': 'ac/lxhluguo_thumb.gif',
'[悲催]': '43/lxhbeicui_thumb.gif',
'[不想上班]': '6b/lxhbuxiangshangban_thumb.gif',
'[躁狂症]': 'ca/lxhzaokuangzheng_thumb.gif',
'[甩甩手]': 'a6/lxhshuaishuaishou_thumb.gif',
'[瞧瞧]': '8b/lxhqiaoqiao_thumb.gif',
'[同意]': '14/lxhtongyi_thumb.gif',
'[喝多了]': 'a7/lxhheduole_thumb.gif',
'[啦啦啦啦]': '3d/lxhlalalala_thumb.gif',
'[杰克逊]': 'e5/lxhjiekexun_thumb.gif',
'[雷锋]': '7a/lxhleifeng_thumb.gif',
'[带感]': 'd2/lxhdaigan_thumb.gif',
'[亲一口]': '88/lxhqinyikou_thumb.gif',
'[飞个吻]': '8a/lxhblowakiss_thumb.gif',
'[加油啊]': '03/lxhjiayou_thumb.gif',
'[七夕]': '9a/lxhqixi_thumb.gif',
'[困死了]': '00/lxhkunsile_thumb.gif',
'[有鸭梨]': '7e/lxhyouyali_thumb.gif',
'[右边亮了]': 'ae/lxhliangle_thumb.gif',
'[撒花]': 'b3/lxhfangjiala_thumb.gif',
'[好棒]': '3e/lxhhaobang_thumb.gif',
'[想一想]': 'e9/lxhxiangyixiang_thumb.gif',
'[下班]': 'f2/lxhxiaban_thumb.gif',
'[最右]': 'c8/lxhzuiyou_thumb.gif',
'[丘比特]': '35/lxhqiubite_thumb.gif',
'[中箭]': '81/lxhzhongjian_thumb.gif',
'[互相膜拜]': '3c/lxhhuxiangmobai_thumb.gif',
'[膜拜了]': '52/lxhmobai_thumb.gif',
'[放电抛媚]': 'd0/lxhfangdianpaomei_thumb.gif',
'[霹雳]': '41/lxhshandian_thumb.gif',
'[被电]': 'ed/lxhbeidian_thumb.gif',
'[拍砖]': '3b/lxhpaizhuan_thumb.gif',
'[互相拍砖]': '5b/lxhhuxiangpaizhuan_thumb.gif',
'[采访]': '8b/lxhcaifang_thumb.gif',
'[发表言论]': 'f1/lxhfabiaoyanlun_thumb.gif',
'[江南style]': '67/gangnamstyle_thumb.gif',
'[牛]': '24/lxhniu_thumb.gif',
'[玫瑰]': 'f6/lxhrose_thumb.gif',
'[赞啊]': '00/lxhzan_thumb.gif',
'[推荐]': 'e9/lxhtuijian_thumb.gif',
'[放假啦]': '37/lxhfangjiale_thumb.gif',
'[萌翻]': '99/lxhmengfan_thumb.gif',
'[吃货]': 'ba/lxhgreedy_thumb.gif',
'[大南瓜]': '4d/lxhpumpkin_thumb.gif',
'[赶火车]': 'a2/lxhganhuoche_thumb.gif',
'[立志青年]': 'f9/lxhlizhiqingnian_thumb.gif',
'[得瑟]': 'ca/lxhdese_thumb.gif',
'[草泥马]': '7a/shenshou_thumb.gif',
'[神马]': '60/horse2_thumb.gif',
'[浮云]': 'bc/fuyun_thumb.gif',
'[给力]': 'c9/geili_thumb.gif',
'[围观]': 'f2/wg_thumb.gif',
'[威武]': '70/vw_thumb.gif',
'[熊猫]': '6e/panda_thumb.gif',
'[兔子]': '81/rabbit_thumb.gif',
'[奥特曼]': 'bc/otm_thumb.gif',
'[囧]': '15/j_thumb.gif',
'[互粉]': '89/hufen_thumb.gif',
'[礼物]': 'c4/liwu_thumb.gif',
'[呵呵]': 'ac/smilea_thumb.gif',
'[嘻嘻]': '0b/tootha_thumb.gif',
'[哈哈]': '6a/laugh.gif',
'[可爱]': '14/tza_thumb.gif',
'[可怜]': 'af/kl_thumb.gif',
'[挖鼻屎]': 'a0/kbsa_thumb.gif',
'[吃惊]': 'f4/cj_thumb.gif',
'[害羞]': '6e/shamea_thumb.gif',
'[挤眼]': 'c3/zy_thumb.gif',
'[闭嘴]': '29/bz_thumb.gif',
'[鄙视]': '71/bs2_thumb.gif',
'[爱你]': '6d/lovea_thumb.gif',
'[泪]': '9d/sada_thumb.gif',
'[偷笑]': '19/heia_thumb.gif',
'[亲亲]': '8f/qq_thumb.gif',
'[生病]': 'b6/sb_thumb.gif',
'[太开心]': '58/mb_thumb.gif',
'[懒得理你]': '17/ldln_thumb.gif',
'[右哼哼]': '98/yhh_thumb.gif',
'[左哼哼]': '6d/zhh_thumb.gif',
'[嘘]': 'a6/x_thumb.gif',
'[衰]': 'af/cry.gif',
'[委屈]': '73/wq_thumb.gif',
'[吐]': '9e/t_thumb.gif',
'[打哈欠]': 'f3/k_thumb.gif',
'[抱抱]': '27/bba_thumb.gif',
'[怒]': '7c/angrya_thumb.gif',
'[疑问]': '5c/yw_thumb.gif',
'[馋嘴]': 'a5/cza_thumb.gif',
'[拜拜]': '70/88_thumb.gif',
'[思考]': 'e9/sk_thumb.gif',
'[汗]': '24/sweata_thumb.gif',
'[困]': '7f/sleepya_thumb.gif',
'[睡觉]': '6b/sleepa_thumb.gif',
'[钱]': '90/money_thumb.gif',
'[失望]': '0c/sw_thumb.gif',
'[酷]': '40/cool_thumb.gif',
'[花心]': '8c/hsa_thumb.gif',
'[哼]': '49/hatea_thumb.gif',
'[鼓掌]': '36/gza_thumb.gif',
'[晕]': 'd9/dizzya_thumb.gif',
'[悲伤]': '1a/bs_thumb.gif',
'[抓狂]': '62/crazya_thumb.gif',
'[黑线]': '91/h_thumb.gif',
'[阴险]': '6d/yx_thumb.gif',
'[怒骂]': '89/nm_thumb.gif',
'[心]': '40/hearta_thumb.gif',
'[伤心]': 'ea/unheart.gif',
'[猪头]': '58/pig.gif',
'[ok]': 'd6/ok_thumb.gif',
'[耶]': 'd9/ye_thumb.gif',
'[good]': 'd8/good_thumb.gif',
'[不要]': 'c7/no_thumb.gif',
'[赞]': 'd0/z2_thumb.gif',
'[来]': '40/come_thumb.gif',
'[弱]': 'd8/sad_thumb.gif',
'[蜡烛]': '91/lazu_thumb.gif',
'[钟]': 'd3/clock_thumb.gif',
'[话筒]': '1b/m_thumb.gif',
'[蛋糕]': '6a/cake.gif',
'[挤眼]': 'c3/zy_thumb.gif',
'[亲亲]': '8f/qq_thumb.gif',
'[怒骂]': '89/nm_thumb.gif',
'[太开心]': '58/mb_thumb.gif',
'[懒得理你]': '17/ldln_thumb.gif',
'[打哈欠]': 'f3/k_thumb.gif',
'[生病]': 'b6/sb_thumb.gif',
'[书呆子]': '61/sdz_thumb.gif',
'[失望]': '0c/sw_thumb.gif',
'[可怜]': 'af/kl_thumb.gif',
'[黑线]': '91/h_thumb.gif',
'[吐]': '9e/t_thumb.gif',
'[委屈]': '73/wq_thumb.gif',
'[思考]': 'e9/sk_thumb.gif',
'[哈哈]': '6a/laugh.gif',
'[嘘]': 'a6/x_thumb.gif',
'[右哼哼]': '98/yhh_thumb.gif',
'[左哼哼]': '6d/zhh_thumb.gif',
'[疑问]': '5c/yw_thumb.gif',
'[阴险]': '6d/yx_thumb.gif',
'[顶]': '91/d_thumb.gif',
'[钱]': '90/money_thumb.gif',
'[悲伤]': '1a/bs_thumb.gif',
'[鄙视]': '71/bs2_thumb.gif',
'[拜拜]': '70/88_thumb.gif',
'[吃惊]': 'f4/cj_thumb.gif',
'[闭嘴]': '29/bz_thumb.gif',
'[衰]': 'af/cry.gif',
'[愤怒]': 'bd/fn_thumb.gif',
'[感冒]': 'a0/gm_thumb.gif',
'[酷]': '40/cool_thumb.gif',
'[来]': '40/come_thumb.gif',
'[good]': 'd8/good_thumb.gif',
'[haha]': '13/ha_thumb.gif',
'[不要]': 'c7/no_thumb.gif',
'[ok]': 'd6/ok_thumb.gif',
'[拳头]': 'cc/o_thumb.gif',
'[弱]': 'd8/sad_thumb.gif',
'[握手]': '0c/ws_thumb.gif',
'[赞]': 'd0/z2_thumb.gif',
'[耶]': 'd9/ye_thumb.gif',
'[最差]': '3e/bad_thumb.gif',
'[打哈气]': 'f3/k_thumb.gif',
'[可爱]': '14/tza_thumb.gif',
'[嘻嘻]': '0b/tootha_thumb.gif',
'[汗]': '24/sweata_thumb.gif',
'[呵呵]': 'ac/smilea_thumb.gif',
'[困]': '7f/sleepya_thumb.gif',
'[睡觉]': '6b/sleepa_thumb.gif',
'[害羞]': '6e/shamea_thumb.gif',
'[泪]': '9d/sada_thumb.gif',
'[爱你]': '6d/lovea_thumb.gif',
'[挖鼻屎]': 'a0/kbsa_thumb.gif',
'[花心]': '8c/hsa_thumb.gif',
'[偷笑]': '19/heia_thumb.gif',
'[心]': '40/hearta_thumb.gif',
'[哼]': '49/hatea_thumb.gif',
'[鼓掌]': '36/gza_thumb.gif',
'[晕]': 'd9/dizzya_thumb.gif',
'[馋嘴]': 'a5/cza_thumb.gif',
'[抓狂]': '62/crazya_thumb.gif',
'[抱抱]': '27/bba_thumb.gif',
'[怒]': '7c/angrya_thumb.gif',
'[右抱抱]': '0d/right_thumb.gif',
'[左抱抱]': '54/left_thumb.gif'};
                return String(text).replace(/\[([^\]]*)\]/g, function(all){
                    return dict[all] ? '<img width="22" height="22" src="http://img.t.sinajs.cn/t4/appstyle/expression/ext/normal/' + dict[all] + '" alt="' + all + '">' : all;
                });
            });
            messageTree = AceTree.create({
                parent: lib.g("messageListTemplate").parentNode,
                oninit: function(tree){
                    tree.eventHandler = AceEvent.on(tree.parent, function(command, element, e){
                        var node = tree.node4target(element);
                        node && tree.oncommand(command, node, e);
                    });
                },
                onreader: function(node){
                    return AceTemplate.format('messageListTemplate', node.data, {
                        node: node,
                        formatTime: formatTime,
                        mutiline: mutiline,
                        markdown: function(text){
                            return markdown.toHTML(text);
                        },
                        ubb: function(text){
                            return AceUbb.exportHtml(text);
                        }
                    });
                },
                oncommand: function(command, node, e){
                    switch (command) {
                        case "letter":
                            sandbox.fire(events.letterDialog, {
                                nick: node.data.nick,
                                to: node.data.from
                            });
                            break;
                    }
                },
                statusClasses: /^(focus|hover|select|expand|self)$/,
                oncreated: function(node) {
                    node.setStatus("self", node.data.from == passportInfo.id, true);
                }
            });
            
            sandbox.on(events.pickSuccess, pickSuccess);
        }
    };
});