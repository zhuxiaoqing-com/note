package com.javasm.system.user.VO;

import java.util.List;

public class PageVO<T> {
	/*
	 * 需要当页数据 path_detailList<Path_detail>
	 * 需要总记录数 Integer totalRecored
	 * 需要 总页码 Integer totalPage
	 * 需要 当前页面 没有servlet 就算默认1; pageCode
	 * 需要 每页的记录长度 pageSize
	 */
	private Integer totalRecored;
	//private Integer totalPage; 总页码通过计算获得  总记录数/每页记录数
	private Integer pageCode; 
	private Integer pageSize; // 默认为 6
	private List<T> path_tList;
	public Integer getTotalRecored() {
		return totalRecored;
	}
	public void setTotalRecored(Integer totalRecored) {
		this.totalRecored = totalRecored;
	}
	public Integer getTotalPage() {
		/*
		 * 因为 java 的int 除法 只会返回 Int 所以 还有如果有余数 的就 还需要一页来显示不足一整页的页数的数据
		 * 如果页数正好除尽 说明 每页的数据正好都是满的 没有溢出的数据
		 * 
		 */
		return totalRecored%pageSize == 0 ? totalRecored/pageSize : totalRecored/pageSize + 1;
	}
	
	public Integer getPageCode() {
		return pageCode;
	}
	public void setPageCode(Integer pageCode) {
		this.pageCode = pageCode;
	}
	public Integer getPageSize() {
		return pageSize;
	}
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	public List<T> getPath_tList() {
		return path_tList;
	}
	public void setPath_tList(List<T> path_tList) {
		this.path_tList = path_tList;
	}

	
	
}
