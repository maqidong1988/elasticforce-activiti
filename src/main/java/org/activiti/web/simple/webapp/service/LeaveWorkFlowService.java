package org.activiti.web.simple.webapp.service;

import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.web.simple.webapp.model.Leave;

public interface LeaveWorkFlowService {

	/**
	 * 启动工作流
	 * 
	 * @param key
	 * @param businessKey
	 * @param variables
	 * @return
	 */
	public ProcessInstance startWorkflow(String key, String businessKey,
			Map<String, Object> variables);

	/**
	 * 根据用户Id查询待办任务列表
	 * 
	 * @param userid
	 *            用户id
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	public List<Map<String, Object>> findRestTask(String userid, String processDefinitionKey, int firstResult, int maxResults);
	
	/**
	 * 根据用户Id数组查询待办任务列表
	 * Added by MingLiang on 2017.7.25
	 * 
	 * @param userids
	 *            用户id数组
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	public List<Map<String, Object>> findRestTask(String[] userids, String processDefinitionKey, int firstResult, int maxResults);
	
	
	public int findRestTaskCount(String userid, String processDefinitionKey);
	/**
	 * 
	 * 根据用户id数据获取所有待办条数
	 * Added by MingLiang on 2017.7.25
	 * 
	 * */
	public int findRestTaskCount(String[] userids, String processDefinitionKey);
	
	/**
	 * 根据用户Id查询待办任务列表
	 * 
	 * @param userid
	 *            用户id
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	public List<Leave> findTask(String userid, String processDefinitionKey);

	/**
	 * 查询运行中的流程实例
	 * 
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	public List<Leave> findRunningProcessInstaces(String processDefinitionKey);
	
	public List<Map<String, Object>> findRunningProcessInstances(String processDefinitionKey);

	/**
	 * 查询已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	public List<Leave> findFinishedProcessInstaces(String processDefinitionKey);
	
	public List<Map<String, Object>> findFinishedProcessInstances(String processDefinitionKey);
	/**
	 * 查询指定用户已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	public List<Map<String, Object>> findFinishedProcessInstancesByUser(
			String userId, 
			int firstResult, 
			int maxResults,
			String beginTime,
			String endTime,
			String businessType);
	
	public int findFinishedProcessInstancesByUser(
			String userId,
			String beginTime,
			String endTime,
			String businessType);
	/**
	 * 查询指定用户数组已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	public List<Map<String, Object>> findFinishedProcessInstancesByUsers(
			String[] userIds, 
			int firstResult, 
			int maxResults,
			String beginTime,
			String endTime,
			String businessType);
	
	public int findFinishedProcessInstancesByUsers(
			String[] userIds,
			String beginTime,
			String endTime,
			String businessType);
	/**
	 * 根据流程定义Id查询流程定义
	 * 
	 * @param processDefinitionId
	 * @return
	 */
	public ProcessDefinition getProcessDefinition(String processDefinitionId);

	/**
	 * 根据流程定义Id查询任务
	 * 
	 * @param taskId
	 * @return
	 */
	public TaskEntity findTaskById(String taskId) throws Exception;

}
