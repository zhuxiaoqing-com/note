<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <base href="<%=basePath%>">    
    <title>??</title>        
  <meta http-equiv="pragma" content="no-cache">
  <meta http-equiv="cache-control" content="no-cache">
  <meta http-equiv="expires" content="0">    
  <meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
  <meta http-equiv="description" content="This is my page">
<%-- <link rel="stylesheet" type="text/css" href="<%=basePath%>resource/css/bootstrap.css"/>

<script type="text/javascript" src="<%=basePath%>resource/js/jquery-3.3.1.min.js"></script>
<script type="text/javascript" src="<%=basePath%>resource/js/bootstrap.js"></script> --%>
<script type="text/javascript" src="js/jquery-easyui-1.4.1/jquery.min.js"></script>
<link href="/js/kindeditor-4.1.10/themes/default/default.css" type="text/css" rel="stylesheet">
<script type="text/javascript" charset="utf-8" src="/js/kindeditor-4.1.10/kindeditor-all-min.js"></script>
<script type="text/javascript" charset="utf-8" src="/js/kindeditor-4.1.10/lang/zh_CN.js"></script>

 </head>
 <script type="text/javascript">
 	$(function() {
 		KindEditor.create("[name=desc]", kingEditorParams);
 		//同步文本框中的商品描述
		itemAddEditor.sync();
 	});
 	var kingEditorParams = {
		//指定上传文件参数名称
		filePostName  : "uploadFile",
		//指定上传文件请求的url。
		uploadJson : '/pic/upload',
		//上传类型，分别为image、flash、media、file
		dir : "image"
	}
 </script>
 <body>
 	<div class="container-fluid">
 		<div class="row">
		  <div class="col-md-offset-3 col-md-9">
		  	<h1>hello 宜立方商城</h1>
		  </div>
		</div>
	</div>
	<textarea style="width:800px;height:300px;visibility:hidden;" name="desc"></textarea>
 </body>
</html>