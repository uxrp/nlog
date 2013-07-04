/**
 * NLogApi 配置
 * @author 王集鹄(wangjihu,http://weibo.com/zswang)
 */
AceCore.addConfig("NLogApi", {
	/**
	 * pick最大等待时间，单位：毫秒
	 */
	pickMaxWait: 45 * 1000,
	/**
	 * 请求的Host地址
	 */
	apiHost: "http://hunter.duapp.com"
});

if (/debug/.test(location)) {
	AceCore.addConfig("NLogApi", {
		apiHost: "http://localhost:2013"
	});
}
