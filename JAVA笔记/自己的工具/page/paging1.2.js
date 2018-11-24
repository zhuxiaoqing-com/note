/*
 * 需要一个 id 为	id="div_bottom"
 * 的div 最好是 div
 * 
 * 异步前函数名称：fun_before     主要用来处理 禁用按钮等
	回调函数： fun_callback        主要用来处理回调以后的数据
	异步地址： pageUrl
	异步格外参数：pageParams
	页码显示数量：pageButSize
 */
//定义一个 function 来接收其 外部传递过来的参数，并将其在页面加载完成后立即加载
	function externalParams(external) {
		//异步前函数名称：fun_before     主要用来处理 禁用按钮等
		//** 回调函数： fun_callback        主要用来处理回调以后的数据  必须
		//** 异步地址： pageUrl 			必须
		//异步格外参数：pageParams
		//页码显示数量：pageButSize
		if(!external) {
			throw "external Can't be empty ";
		}
		//this.external = external;
		if(!external.pageUrl) {
			throw  "pageUrl Can't be empty ";
		}
		if(!external.fun_callback) {
			throw "fun_callback Can't be empty ";
		}
		mypageObj.url = external.pageUrl;
		mypageObj.fun_callback = external.fun_callback;
		
		// 判断 butSize 是否存在
		if(external.pageButSize) {
			mypageObj.butSize = external.pageButSize;
		}
	
		
		// 进行异步
		page();
		// 给页面分页初始化
	}
	// 传递过来的参数
	//var external;
	
	var mypageObj = {};//定义一个对象
	// 变化前的结束页码
	mypageObj.lastEndPageCode = 0;
	// 当前页面码
	mypageObj.pageCode = 1;
	// 当前显示的页面数据数
	mypageObj.pageSize = 10;
	// 总共有几页
	mypageObj.totalPage = 0;
	// 总记录
	mypageObj.totalRecored = 0;
	// 判断是否进行了记录总数变化
	//var parameter = 0;
	// 换页规则 页码显示数量
	mypageObj.butSize = 9;
	// 搜索条件
	
	////////////
	// 异步参数
	mypageObj.url;
	// 回调函数
	mypageObj.fun_callback = null;
	// 异步前的函数
	mypageObj.fun_before = null;
	
	/*
	用来 page ajax 的页面
	*/
	function page() {
		
		// 参数 使用json 格式会自动传输  
		var params = {pageCode:mypageObj.pageCode, pageSize:mypageObj.pageSize};
		// 判断用户是否传递过来参数
		try {
			if(external.pageParams) {
				for(var i in external.pageParams) {
					params[i] = external.pageParams[i];
				}
			}
		} catch (e) {
			throw "params isn't JSON..."+ e.message;
		}
		
		////////////////////回调前函数
		if(external.fun_before) {
			external.fun_before();
		}
		////////////////////
		
		
		// 用来禁用 全部按钮 防止 数据错乱
		removePageClick();
		if(!mypageObj.url) {
			throw "url cannot null!!!";
		}
		$.post(mypageObj.url,params,myPage_LimitCallback,"json");
	}
	/*
		异步成功后的回调函数  myPage_LimitCallback
	*/
	function myPage_LimitCallback(datas) {
		
		
////////////////////////////////////
		/* 回调函数   */
		if(!mypageObj.fun_callback) {
			throw "fun_callback Can't be empty ";
		}
		mypageObj.fun_callback(datas);
		/* 回调函数   */
////////////////////////////////////		
		

		// 修正其ajax 调用后修改 相应的数据 pageCode 当前页面  totalPage 总页面  总记录
		// 放在 initPage()方法里面判断。只要其 totalRecored 或 totalPage 变化了，就
		// 调用 ArrayPage 来重新组装页码
	/* 	totalRecored = datas.totalRecored;
		totalPage = datas.totalPage; */
		
		
		// 调用方法修改 updatePageText参数 就是 共几页什么的
		updatePageText(datas);
		// 将 datas 的总页数赋给 页面上的
		// 如果存在 将值赋予 不存在就是 0
		if(datas.totalRecored) {
			mypageObj.totalRecored = datas.totalRecored;
		} else {
			mypageObj.totalRecored = 0;
		}
		// 如果存在 将值赋予 不存在就是 0
		if(datas.totalPage) {
			mypageObj.totalPage = datas.totalPage;
		} else {
			mypageObj.totalRecored = 0;
		}
	
		// 进行页码变化
		initPage(datas);
		// 将其按钮恢复
		pageClick();
	
		// 自动判断其按钮是否可用
		disabledAll();
	}
	/*
		
	*/
	function updatePageText(datas) {
		// 先修改 span 里面的参数
		$("#id_totalRecored").html("共" + datas.totalRecored + "条");
		$("#id_pageCode").html("第" + datas.pageCode + "页");
		
		// 因为 option 里面的参数要修改比较废时间所以就加个判断
	
		if(mypageObj.totalPage != datas.totalPage){
			$("#id_totalPage").html("共" + datas.totalPage + "页");
			var select = $("#id_select_totalPage");
			// 将 select 里面的数据清空
			select.html("");
			// 向里面添加数据
			for(var i =1; i<= datas.totalPage; i++) {
				var select_option = "<option value='" + i + "'>" + i + "</option>";
				select.append(select_option);
			}
		}
	}
	
	/*
		用来判断 操作 page 的按钮是否可用
		可用返回 true 不可用返回 false
	*/
	function disabledAll() {
		var $top_before = $("#top_but,#before_but");
		var $bottom_after = $("#bottom_but,#after_but");
		// 如果 当前页面  小于2 说明是第一页 
		if(mypageObj.pageCode < 2) {
			$top_before.addClass("disabled");
		} else {
			$top_before.removeClass("disabled");
		}
		// 如果当前页面和 最后一个页面 相同 说明是最后一页
		if(mypageObj.pageCode >= mypageObj.totalPage) {
			$bottom_after.addClass("disabled");
		} else {
			$bottom_after.removeClass("disabled");
		}
	}
	// 这是第一个 onload 函数
	$(function() {
		// 加载 分页全家桶
		myload();
		
		
		// 绑定 点击页码按钮也可以换页
		pageClick();
	});
	
////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////
// 进行点击页码来换页  给页码附加点击事件
	function pageClick() {
		// 页码点击事件
		$("body").on("click",".num a",page_a);
		// 首页按钮
    	$("#top_but").on("click",fun_top_but);
		// 上一页
		$("#before_but").on("click",fun_before_but);
		// 下一页按钮
 		$("#after_but").on("click",fun_after_but);
 		// 尾页按钮
 		$("#bottom_but").on("click",fun_bottom_but);
		// 跳转页面 id_select_totalPage	
		$("#id_select_totalPage").on("change",fun_id_select_totalPage);
		// 更改页面显示 数据量   id_pageSize
		$("#id_pageSize").on("change",fun_id_pageSize);
		
	}
	// 卸载事件
	function removePageClick(){
		// 页码点击事件
		$("body").off("click",page_a);
		// 首页按钮
    	$("#top_but").off("click",fun_top_but);
		// 上一页
		$("#before_but").off("click",fun_before_but);
		// 下一页按钮
 		$("#after_but").off("click",fun_after_but);
 		// 尾页按钮
 		$("#bottom_but").off("click",fun_bottom_but);
		// 跳转页面 id_select_totalPage	
		$("#id_select_totalPage").off("change",fun_id_select_totalPage);
		// 更改页面显示 数据量   id_pageSize
		$("#id_pageSize").off("change",fun_id_pageSize);

	}
	// 提取出来以便卸载 事件
	function page_a() {
			// 将页码修改为 当前
			mypageObj.pageCode = parseInt($(this).text());
			// 进行异步
			page();
		}

 // 首页按钮 副本
    function fun_top_but() {
    	// 虽然 按钮不可用是不可以点击的  但是还是加个判断
		var isBut = $(this).hasClass("disabled");
		if(!isBut) {
			// 修改要跳转的当前页码
    		mypageObj.pageCode = 1;
			// 要进行页面变化
			//parameter = 1;
    		// 进行 ajax
    		page();
		}
    }
 // 上一页按钮 副本
 	function fun_before_but() {
 	// 获取当前按钮的状态 为可用就将其跳转
		// 虽然 按钮不可用是不可以点击的  但是还是加个判断
		var isBut = $(this).hasClass("disabled");
		if(!isBut) {
			// 要进行页面变化
			//parameter = 1;
    		// 进行 ajax
    		mypageObj.pageCode = mypageObj.pageCode - 1;
    		// 进行 ajax
    		page();
		}
 	}
 	
 // 下一页按钮 副本
 	function fun_after_but() {
 	// 获取当前按钮的状态 为可用就将其跳转
		// 虽然 按钮不可用是不可以点击的  但是还是加个判断
		var isBut = $(this).hasClass("disabled");
		if(!isBut) {
			// 修改要跳转的当前页码
    		mypageObj.pageCode = mypageObj.pageCode + 1;
    		// 进行 ajax
    		page();
		}
 	}
	// 尾页按钮 副本
	function fun_bottom_but() {
		// 获取当前按钮的状态 为可用就将其跳转
		// 虽然 按钮不可用是不可以点击的  但是还是加个判断
		var isBut = $(this).hasClass("disabled");
		if(!isBut) {
			// 修改要跳转的当前页码
    		mypageObj.pageCode = mypageObj.totalPage;
    		// 进行 ajax
    		page();
		}
 	}
	
	// 跳转页面 id_select_totalPage 副本
	function fun_id_select_totalPage() {
		// 修改要跳转的当前页码
		mypageObj.pageCode =  parseInt($(this).val());
		// 进行 ajax
		page();
	}

	
	// 更改页面显示 数据量   id_pageSize 副本
	function fun_id_pageSize() {
		// 修改当前页码为 1
		mypageObj.pageCode = 1;
		// 修改 pageSize 为 $(this).val();
		mypageObj.pageSize = parseInt($(this).val());
		// 进行 ajax
		page();
	}
	
////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////
	
	/*
	用来判断 操作 page 的按钮是否可用
	可用返回 true 不可用返回 false
*/
/*function disabledAll() {
	var $top_before = $("#top_but,#before_but");
	var $bottom_after = $("#bottom_but,#after_but");
	// 如果 当前页面  小于2 说明是第一页 
	if(pageCode < 2) {
		$top_before.addClass("disabled");
	} else {
		$top_before.removeClass("disabled");
	}
	// 如果当前页面和 最后一个页面 相同 说明是最后一页
	if(pageCode == totalPage) {
		$bottom_after.addClass("disabled");
	} else {
		$bottom_after.removeClass("disabled");
	}
}*/
	
	
	
	/* 
		分页码改变的规则
		如果 开始页面 
		传入需要几个按钮
		//当前页面码
		var mypageObj.pageCode = "1";
		// 当前显示的页面数据数
		var mypageObj.pageSize = "${page.pageSize}";
		// 总共有几页
		var mypageObj.totalPage = "${page.totalPage}";
		// 总记录
		var mypageObj.totalRecored = "${page.totalRecored}";
	*/
	

	// 重新排列分页
	// myPage 对象 传入一个 myPage{startPage:0,endPage:10}
	function pageArray(myPage) {
		/*
			你也可以自己来选择新的页码规则
		*/
		// 将其原来按钮的清空
		var $ul = $("#page_ul");
		$ul.html("");
		// 循环为其添加
		for(var i = myPage.startPage; i <= myPage.endPage; i++) {
			var li = "<li class='num'><a href='javascript:void(0);'>" + i + "</a></li>";
			$ul.append(li);
		}
		// 给新的页码 附上点击事件
		// pageClick(); 不需要了因为采用了将点击事件附加在 body 然后过滤 
	}
	
	// 副本 计算出 新的 开始页 和结束页  如果其 lastPageCode 在其中就不需要变化页码
	// 判断结束页码不是更好吗。。老的结束页码和新的结束页码一样就说明不需要更换
	// 需要变化就调用其 pageArray
	// 初始化页码
	// 给页面分页初始化
	function initPage(datas) {
		
		// 计算首页和尾页的公式
		// var num =  pageCode % butSize = 1 说明 是 第一个 不是的话就将其 pageCode - num + 1  
		// num 是 尾页与 当前页的差距 所以需要减去差距  因为尾页本身也是一页 所以还需要加 1 
		// 
		var num = mypageObj.pageCode % mypageObj.butSize;
		if(num == 0) {
			num =  mypageObj.butSize;
		}
		var startPage = mypageObj.pageCode - num + 1;
		// 如果 num == 0 就说明到了最后一格了 这是特殊的
		/*if(num == 0) {
			startPage = mypageObj.pageCode - mypageObj.butSize + 1;
		}*/
		var endPage = mypageObj.butSize + startPage - 1;
		if(endPage > mypageObj.totalPage) {
			endPage = mypageObj.totalPage;
		}
		
//		lastEndPageCode
		
		// 将其  mypageObj.pageCode 赋给 mypageObj.lastPageCode
		//mypageObj.lastPageCode = mypageObj.pageCode;
		
		
		
		// 判断如果尾页变了 那么就说明 页码需要变化
		if(mypageObj.lastEndPageCode != endPage) {
			pageArray({
				startPage: startPage,
				endPage: endPage
			});
			// 将新的尾页赋给旧的尾页
			mypageObj.lastEndPageCode = endPage;
	} else {
		// 将有 active 的元素的 active 清除
		$("#page_ul > .active").removeClass("active");
	}
		$("#page_ul > li").each(function(){
			if($(this).text() == mypageObj.pageCode){
				$(this).addClass("active");
				return false;
			}
		});
	}
	/* // 计算开始和结尾页面
	function countPaging() {
		// var num =  pageCode % butSize = 1 说明 是 第一个 不是的话就将其 pageCode - num + 1  
		// num 是 尾页与 当前页的差距 所以需要减去差距  因为尾页本身也是一页 所以还需要加 1 
		var num = mypageObj.lastPageCode % mypageObj.butSize;
		var startPage = mypageObj.lastPageCode - num + 1;
		var endPage = mypageObj.butSize + startPage - 1;
		if(endPage > mypageObj.totalPage) {
			endPage = mypageObj.totalPage;
		}
		var obj_page = {startPage:startPage,endPage:endPage};
		return obj_page;
	} */

////////////////////////////////////////////////////////////////////
// 用来初始化页面 按钮等
function myload() {
					/*
						在 id 为 div_bottom 的div 里面输入其 分页全家桶
					*/
					$("#div_bottom_limit_page").html("<span style='font-family: STCaiyun;'><div class='form-inline' style='vertical-align: bottom; padding-left: 10%;'>"+
							"<span class='span_botton'>"+
							"<a href='javascript:void(0);' id='top_but'  class='btn btn-success myPageBtn' role='button'>首页</a>"+
							"<a href='javascript:void(0);' id='before_but' class='btn btn-success myPageBtn' role='button'>上一页</a>"+
								"<nav style='display: inline-block; position: relative; top: 34px' aria-label='Page navigation'>"+						
								 "<ul class='pagination' id='page_ul'>"+
								  "</ul>"+
							"</nav>"+
							
							"<a href='javascript:void(0);' id='after_but'  class='btn btn-success myPageBtn' role='button'>下一页</a>"+
							"<a href='javascript:void(0);' id='bottom_but' class='btn btn-success myPageBtn' role='button'>尾页</a>"+
						"</span>"+
							"<span class='span_font'>"+
							"&emsp;<span id='id_pageCode'>第0页</span>"+
							"<span id='id_totalPage'>共0页</span>&emsp;"+
				"跳转到&nbsp;<select id='id_select_totalPage' name='pageSize' class='form-control myPageSel'>"+
								
							"</select>&nbsp;页 "+
								"&emsp;<span id='id_totalRecored'>共0条</span>"+
							"</span>"+
								"<span class='span_font' style='margin-left: 20px;'>"+
							" 表格显示&nbsp;<select id='id_pageSize' name='pageSize' class='form-control myPageSel'>"+
							 		
							"</select>&nbsp;条记录  "+
							"</span>"+
							"</div>"+
						"</div></span>");
					// 初始化 表格显示记录
					var $id_pageSize = $("#id_pageSize");
					for(var i=5; i<11; i++) {
						if(i == mypageObj.pageSize) {
							$id_pageSize.append("<option selected = 'true' value='" + i + "'>" + i + "</option>");
							continue; 
						}
						$id_pageSize.append("<option value='" + i + "'>" + i + "</option>");
					}
				};
