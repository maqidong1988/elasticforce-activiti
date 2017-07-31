package org.activiti.web.simple.webapp.service;

import java.util.List;

/**
 * 
 * fms系统流程指定受理人目录业务接口
 * 
 * @author xuzw
 *
 * 
 * 
 */
public interface FmsLdapService {
	/**
	 * 根据启动用户查询出其部门领导集合
	 * 
	 * @param employee
	 * @return
	 */
	public List<String> findDeptLeaders(String employee);

	/**
	 * 根据启动用户查询出其hr领导集合
	 * 
	 * @param employee
	 * @return
	 */
	public List<String> findHrOperators(String employee);

	/**
	 * 根据启动用户查询总监领导集合
	 * 
	 * @param employee
	 * @return
	 */
	public List<String> findLeaders(String employee);

	/**
	 * 根据启动用户查找财务集合
	 * 
	 * @param employee
	 * @return
	 */
	public List<String> findFinances(String employee);

	/**
	 * 根据启动用户查找出纳集合
	 * 
	 * @param employee
	 * @return
	 */
	public List<String> findCashiers(String employee);
}
