/*js1*/
/*js1*/
/*js1*/

$(document).ready(function(){
			var $text = $(".header_li");
			var maxHeight = 0;
			/*遍历获取 li max height*/
			$text.each(function() {
			   var height = parseInt($(this).css("height")); /*得到 22px 使用 parseInt 会自动将其 px去掉变成 22*/
			   if(height > maxHeight) {
				   maxHeight = height;
			   }
			}); 
			/*再次遍历比较 li 的 height 与 maxHeight 的差距 将差距变为 padding top 和 padding bottom 对半*/
			$text.each(function() {
				var height = parseInt($(this).css("height"));
				$(this).css("padding-top", (maxHeight - height)/2);
				$(this).css("padding-bottom", (maxHeight - height)/2);
			});
			
			
			
			/*初始化.header_li 将其边框设置为 3px
				因为 前面需要计算 li 的height 有了 border 的边框以后 会变得不准确。因为 border-bottom 也会被算上height。
			*/
			$(".header_li").addClass("header_liChuShiHua");
			/*
				超链接事件
			*/
			$(".myfix").find("a").hover(
			function(){
				 $(this).parent().addClass("hover1");
			}, 
			function(){
				 $(this).parent().removeClass("hover1");
			});
				/*
					 淡入淡出
				*/
			 $(window).on("scroll", function() {
				 var sTop = $(window).scrollTop();
	              var sTop = parseInt(sTop);
	               if(sTop > 0) {
				$(".myfix").css("background-color","black");
				} else {
					 console.log("dd");
					 $(".myfix").css("background-color","transparent");
				} 
			 });
						
		}); 
	

/*js2*/
/*js2*/
/*js2*/










