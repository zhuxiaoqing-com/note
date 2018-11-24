function createXMLHttpRequest() {
	try {
		return new XMLHttpRequest();// 大多数浏览器
	} catch (e) {
		try {
			return new ActiveXObject("Msxm12.XMLHTTP");// IE6.0
		} catch (e) {
			try {
				return new new ACtiveXObject("Microsoft.XMLHTTP");// IE5.0
			} catch (e) {
				alert("哥们，你用的什么浏览器啊？");
				throw e;
			}
		}
	}
}
	/*
	 * potion的参数
	 * 请求参数	method  有默认
	 * 请求的url	url
	 * 是否异步	asyn	有默认
	 * 请求体	params	有默认
	 * 回调方法	callback
	 * 服务器响应数据转换成什么类型	type	 有默认
	 */

function ajax(option) {
	/*
	 * 1.得到xmlHttp
	 */
	var xmlHttp = createXMLHttpRequest();
	/*
	 * 2.打开连接
	 */
	if(!option.method) { // 因为是undefined，所以为false
		option.method = "GET";
	}
	if(option.asyn == undefined) {
		option.asyn = true;
	}
	xmlHttp.open(option.method, option.url, true);
	/*
	 * 3.判断请求是否为post
	 */
	if(option.method.toLowerCase() == "post") {
		xmlHttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	}
	/*
	 * 4.发送请求
	 */
	
	xmlHttp.send(option.params);
	/*
	 * 5.注册监听
	 */
	xmlHttp.onreadystatechange = function() {
		if(xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			var data;
			if(option.type.toLowerCase() == undefined) { // 没有赋值默认为 text
				data = xmlHttp.responseText;
			} else if(option.type.toLowerCase() == "text") {
				data = xmlHttp.responseText;
			} else if(option.type.toLowerCase() == "xml") {
				data = xmlHttp.responseXML;
			} else if(option.type.toLowerCase() == "json") {
				var text = xmlHttp.responseText;
				data = eval("(" + text + ")");
			} 
			
			/*
			 * 6.调用回调方法
			 */
			option.callback(data);
		}
	}
}













